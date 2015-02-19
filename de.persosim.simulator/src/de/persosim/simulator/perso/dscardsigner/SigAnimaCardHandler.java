package de.persosim.simulator.perso.dscardsigner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.List;

import de.persosim.simulator.utils.HexString;
import static de.persosim.simulator.utils.PersoSimLogger.TRACE;
import static de.persosim.simulator.utils.PersoSimLogger.log;

/**
 * Class for communication with smartcards containing an SigAnima JavaCard applet (https://github.com/tsenger/SigAnima)
 *  
 * SigAnima provided ECDSA plain signature function.  
 * This class provides all necessary methods to use all functions of this applet.
 * All implemented commands with all parameters are described in the APDU reference document of SigAnima on the project repository. 
 * 
 * @author tsenger
 *
 */
public class SigAnimaCardHandler {

    public static final byte PERM_FREE = 0;
    public static final byte PERM_PIN = 1;

    private static final int SW_NO_ERROR = 0x9000;



    public SigAnimaCardHandler(int slotId, byte[] aid)  {
    }

    /**
     * @param password The data field should contain the ASCII bytes of the PIN (i.e. from the range 0x30 .. 0x39). 
     * @return Returns true if the PIN was correct. Otherwise it return false. The retry counter has a initial value of 3.
     * @throws CardException
     */
    public boolean verify(String password){
    	return false;
    }

    /**
     * Signs the given plain data and return the signature. The input field may contain the hash value of the data to be signed. 
     * The sign command will sign what ever it gets in the input field. 
     * The signing function will pad data with leading zero up to the size of the public key if the data is shorter then the public key size. 
     * If the data is bigger then the public key, the data will be truncated to the size of the public key (most significant bytes will be cut off).
     * 
     * @param keyId Contains the key identifier to the key to sign with. There three slots for key pair. The key identifier is simply the index of the key pair and must be a value between 0x00 and 0x02.
     * @param input Input data to sign. It may contain the hash value of the data to be signed. 
     * @return Signature bytes
     * @throws CardException
     */
    public byte[] sign(byte keyId, byte[] input){
    	return null;
    }

    /**
     * Generates a ECDSA key pair and return the public key
     * 
     * @param keyId Contains the key identifier. There three slots for key pair. The key identifier is simply the index of the key pair and must be a value between 0x00 and 0x02.
     * @param domainParameterId  Contains the ID of the standardized domain parameters. Valid values are: 0x0A for secp224r1, 0x0B for BrainpoolP224r1, 0x0C for secp256r1, 0x0D for BrainpoolP256r1, 0x0E for BrainpoolP320r1
     * @return The response is a simple TLV structures with tag 0x86 which contains the uncompressed EC public key as value.
     * @throws CardException
     */
    public byte[] genKeyPair(byte keyId, byte domainParameterId){
    	return null;
    }

    /**
     * Generates a new EF and fill it with the given data
     * 
     * @param fid FID of the EF to create
     * @param fileBytes The content of the EF
     * @return success
     * @throws CardException
     */
    public boolean createAndWriteFile(short fid, byte[] fileBytes){
    	return false;
    }

    /**
     * Select existing EF and fill it with the given data
     * @param fid EF to select
     * @param fileBytes The content of the EF
     * @return success
     * @throws CardException
     */
    public boolean writeFile(short fid, byte[] fileBytes){
    	return false;
    }

    /**
     * Sets the PUK of the applet.
     * This command is only available when the applet is in the initial state. 
     * 
     * @param puk The field should contain the ASCII bytes of the PUK (i.e. from the range 0x30 .. 0x39).  The PUK length should always be 10.
     * @return success
     * @throws CardException
     */
    public boolean setPUK(String puk) {
    	return false;
    }

    /**
     * Change the PIN of the applet
     * 
     * @param puk The field should contain the ASCII bytes of the PUK (i.e. from the range 0x30 .. 0x39). The PUK length should always be 10.
     * @param pin The field should contain the ASCII bytes of the PUK (i.e. from the range 0x30 .. 0x39). The PIN length should be between 4 and 10.
     * @return success
     * @throws CardException
     */
    public boolean setPIN(String puk, String pin) {
    	return false;
    }

    /**
     * Sets the internal state of the applet.
     * 
     * @param state 0x01 initial, 0x02 prepersonalized. The state is set to personalized (0x03) implicitly by the change reference data command when setting the user PIN. 
     * @return success
     * @throws CardException
     */
    public boolean setState(byte state) {
    	return false;
    }

    /**
     * Reads the content of an elementary transparent file (EF). If the file is
     * bigger then 255 byte this function uses multiply READ BINARY command to
     * get the whole file.
     *
     * @param fid contains the FID of the EF to read.
     * @return Returns the content of the EF with the given SFID
     * @throws CardException
     * @throws IOException
     */
    public byte[] getFile(short fid) throws IOException   {

        return null;
    }

    /**
     * Get the length value from a TLV coded byte array. This function is adapted
     * from bouncycastle
     *
     * @see org.bouncycastle.asn1.ASN1InputStream#readLength(InputStream s, int
     *      limit)
     *
     * @param b
     *            TLV coded byte array that contains at least the tag and the
     *            length value. The data value is not necessary.
     * @return
     * @throws IOException
     */
    private int getLength(byte[] b) throws IOException {
        ByteArrayInputStream s = new ByteArrayInputStream(b);
        int size = 0;
        s.read(); // Skip the the first byte which contains the Tag value
        int length = s.read();
        if (length < 0)
            throw new EOFException("EOF found when length expected");

        if (length == 0x80)
            return -1; // indefinite-length encoding

        if (length > 127) {
            size = length & 0x7f;

            // Note: The invalid long form "0xff" (see X.690 8.1.3.5c) will be
            // caught here
            if (size > 4)
                throw new IOException("DER length more than 4 bytes: " + size);

            length = 0;
            for (int i = 0; i < size; i++) {
                int next = s.read();
                if (next < 0)
                    throw new EOFException("EOF found reading length");
                length = (length << 8) + next;
            }

            if (length < 0)
                throw new IOException("corrupted stream - negative length found");

        }
        return length + size + 2; // +1 tag, +1 length
    }

}