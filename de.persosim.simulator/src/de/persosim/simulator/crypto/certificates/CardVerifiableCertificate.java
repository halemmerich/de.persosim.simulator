package de.persosim.simulator.crypto.certificates;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import de.persosim.simulator.exception.CarParameterInvalidException;
import de.persosim.simulator.exception.CertificateNotParseableException;
import de.persosim.simulator.exception.NotImplementedException;
import de.persosim.simulator.exception.NotParseableException;
import de.persosim.simulator.protocols.TR03110Utils;
import de.persosim.simulator.protocols.ta.CertificateHolderAuthorizationTemplate;
import de.persosim.simulator.protocols.ta.CertificateRole;
import de.persosim.simulator.protocols.ta.RelativeAuthorization;
import de.persosim.simulator.protocols.ta.TaOid;
import de.persosim.simulator.protocols.ta.TerminalType;
import de.persosim.simulator.tlv.ConstructedTlvDataObject;
import de.persosim.simulator.tlv.PrimitiveTlvDataObject;
import de.persosim.simulator.tlv.TlvDataObject;
import de.persosim.simulator.utils.BitField;
import de.persosim.simulator.utils.Utils;


/**
 * This class implements card verifiable certificates as described in TR-03110
 * v2.10 Part 3 Appendix C.
 * 
 * @see CardVerifiableCertificate
 * @author mboonk
 * 
 */
public class CardVerifiableCertificate {
	int certificateProfileIdentifier;
	PublicKeyReference certificateAuthorityReference;
	TaOid publicKeyOid;
	PublicKey publicKey;
	PublicKeyReference certificateHolderReference;
	CertificateHolderAuthorizationTemplate certificateHolderAuthorizationTemplate;
	Date certificateEffective;
	Date certificateExpiration;
	List<CertificateExtension> certificateExtensions;
	
	public CardVerifiableCertificate() {
	}
	
	/**
	 * Create a certificate object from the TLV-encoding using the domain
	 * parameters from the certificate.
	 * @param certificateBodyData as described in TR-03110 V2.10 part 3, C
	 * @throws CertificateNotParseableException
	 */
	public CardVerifiableCertificate(ConstructedTlvDataObject certificateBodyData) throws CertificateNotParseableException {
		this(certificateBodyData, null);
	}

	/**
	 * Create a certificate object from the TLV-encoding using the domain
	 * parameters from the given public key if the certificate does not contain
	 * them.
	 * 
	 * @param certificateBodyData as described in TR-03110 V2.10 part 3, C
	 * @param currentPublicKey the public key to be used
	 * @throws CertificateNotParseableException
	 */
	public CardVerifiableCertificate(
			ConstructedTlvDataObject certificateBodyData,
			PublicKey currentPublicKey) throws CertificateNotParseableException {
		//certificate profile identifier
		certificateProfileIdentifier = Utils.getIntFromUnsignedByteArray(certificateBodyData.getTlvDataObject(TR03110Utils.TAG_5F29).getValueField());
		//certification authority reference
		try {
			certificateAuthorityReference = new PublicKeyReference(certificateBodyData.getTlvDataObject(TR03110Utils.TAG_42));
		} catch (CarParameterInvalidException e) {
			throw new CertificateNotParseableException("The certificate authority reference could not be parsed");
		}
		//public key
		try {
			ConstructedTlvDataObject publicKeyData = (ConstructedTlvDataObject) certificateBodyData.getTlvDataObject(TR03110Utils.TAG_7F49);
			publicKeyOid = new TaOid(publicKeyData.getTlvDataObject(TR03110Utils.TAG_06).getValueField());
			publicKey = TR03110Utils.parseCertificatePublicKey(publicKeyData, currentPublicKey);
			if (publicKey == null){
				throw new CertificateNotParseableException("The public key data could not be parsed");
			}
		} catch (GeneralSecurityException e) {
			throw new CertificateNotParseableException("The public key data could not be parsed");
		}
		//certificate holder reference
		try {
			certificateHolderReference = new PublicKeyReference(certificateBodyData.getTlvDataObject(TR03110Utils.TAG_5F20));
		} catch (CarParameterInvalidException e) {
			throw new CertificateNotParseableException("The certificate holder reference could not be parsed");
		}
		//dates
		try {
			certificateExpiration = TR03110Utils.parseDate(((PrimitiveTlvDataObject) certificateBodyData.getTlvDataObject(TR03110Utils.TAG_5F24)).getValueField());
			certificateEffective = TR03110Utils.parseDate(((PrimitiveTlvDataObject) certificateBodyData.getTlvDataObject(TR03110Utils.TAG_5F25)).getValueField());
		} catch (NotParseableException e) {
			throw new CertificateNotParseableException("The date could not be parsed");
		}
		
		if (certificateExpiration.before(certificateEffective)){
			throw new CertificateNotParseableException("The certificates expiration date is before the effective date");
		}
		
		//chat
		certificateHolderAuthorizationTemplate = parseChat((ConstructedTlvDataObject) certificateBodyData.getTlvDataObject(TR03110Utils.TAG_7F4C));
		//certificate extensions
		certificateExtensions = parseExtensions((ConstructedTlvDataObject) certificateBodyData.getTlvDataObject(TR03110Utils.TAG_65));
	}

	/**
	 * Create a list of all certificate extensions
	 * @param extensionsData as described in TR03110 v2.10 part 3, C
	 * @return all parsed extensions
	 */
	private List<CertificateExtension> parseExtensions(
			ConstructedTlvDataObject extensionsData) {
		List<CertificateExtension> result = new ArrayList<>();
		if (extensionsData != null){
			for (TlvDataObject ddt : extensionsData.getTlvDataObjectContainer()){
				if (ddt instanceof ConstructedTlvDataObject){
					result.add(new CertificateExtension((ConstructedTlvDataObject) ddt));
				}
			}
		}
		return result;
	}
	
	/**
	 * Create a CHAT object from data as stored in a card verifiable certificate.
	 * @param chatData as described in TR-03110 V2.10 part 3, C
	 * @return the {@link CertificateHolderAuthorizationTemplate} object
	 * @throws CertificateNotParseableException
	 */
	private CertificateHolderAuthorizationTemplate parseChat(
			ConstructedTlvDataObject chatData) throws CertificateNotParseableException {

		TaOid objectIdentifier = new TaOid(chatData.getTlvDataObject(TR03110Utils.TAG_06).getValueField());
		PrimitiveTlvDataObject relativeAuthorizationData = (PrimitiveTlvDataObject) chatData.getTlvDataObject(TR03110Utils.TAG_53);
		CertificateRole role = CertificateRole.getFromMostSignificantBits(relativeAuthorizationData.getValueField()[0]);
		BitField authorization = BitField.buildFromBigEndian(relativeAuthorizationData.getLengthValue() * 8 - 2, relativeAuthorizationData.getValueField());
		RelativeAuthorization relativeAuthorization = new RelativeAuthorization(role, authorization);
		
		CertificateHolderAuthorizationTemplate result = new CertificateHolderAuthorizationTemplate(objectIdentifier, relativeAuthorization);
		
		//check if oid and relative authorization fit together
		TerminalType type = result.getTerminalType();
		int authBits = result.getRelativeAuthorization().getRepresentation().getNumberOfBits();
		
		if ((type.equals(TerminalType.AT) && authBits != 40) || ((type.equals(TerminalType.IS) || type.equals(TerminalType.ST)) && authBits != 8)){
			throw new CertificateNotParseableException("invalid combination of OID and terminal type");
		}
		
		return result;
	}

	public int getCertificateProfileIdentifier() {
		return certificateProfileIdentifier;
	}
	
	/**
	 * @return the reference to the public key of the certificate authority
	 */
	public PublicKeyReference getCertificateAuthorityReference() {
		return certificateAuthorityReference;
	}
	
	/**
	 * @return the reference to the public key of the certificate holder
	 */
	public PublicKeyReference getCertificateHolderReference() {
		return certificateHolderReference;
	}
	
	/**
	 * @return the public key associated with this certificate
	 */
	public PublicKey getPublicKey() {
		return publicKey;
	}
	
	/**
	 * @return the {@link CertificateHolderAuthorizationTemplate} for this
	 *         certificate
	 */
	public CertificateHolderAuthorizationTemplate getCertificateHolderAuthorizationTemplate() {
		return certificateHolderAuthorizationTemplate;
	}

	/**
	 * @return the date from which the certificate is valid
	 */
	public Date getEffectiveDate() {
		return certificateEffective;
	}
	
	/**
	 * @return the date form which the certificate is no longer valid
	 */
	public Date getExpirationDate() {
		return certificateExpiration;
	}
	
	/**
	 * @return the extensions this certificate has included
	 */
	public Collection<CertificateExtension> getCertificateExtensions() {
		return certificateExtensions;
	}

	/**
	 * Get the DER-encoded representation of this certificate.
	 * 
	 * @return
	 * 
	 */
	public byte[] getEncoded() {
		//IMPL MBK return tlv encoded certificate here
		throw new NotImplementedException();
	}

	/**
	 * @return the public keys oid
	 */
	public TaOid getPublicKeyOid() {
		return publicKeyOid;
	}
}
