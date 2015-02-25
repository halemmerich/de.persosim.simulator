package de.persosim.simulator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URL;
import java.security.Security;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.osgi.framework.Bundle;

import de.persosim.simulator.jaxb.PersoSimJaxbContextProvider;
import de.persosim.simulator.perso.DefaultPersoTestPki;
import de.persosim.simulator.perso.MinimumPersonalization;
import de.persosim.simulator.perso.Personalization;
import de.persosim.simulator.utils.PersoSimLogger;

/**
 * This class provides access to and control of the actual simulator. It can be
 * used to start, stop and configure it. The simulator may be configured by
 * providing either command line arguments during start-up or user initiated
 * commands at runtime. As all parameters vital for the operation of the
 * simulator are implicitly set to default values by fall-through, no explicit
 * configuration is required.
 * 
 * @author slutters
 * 
 */
public class PersoSim implements Simulator {
	
	private SocketSimulator simulator;
	
	/*
	 * This variable holds the currently used personalization.
	 * It may explicitly be null and should not be read directly from here.
	 * As there exist several ways of providing a personalization of which none at all may be used the variable may remain null/unset.
	 * Due to this possibility access to this variable should be performed by calling the getPersonalization() method. 
	 */
	private Personalization currentPersonalization = new DefaultPersoTestPki();
	
	public static final String LOG_SIM_EXIT     = "simulator exit";
	
	public static final String persoPlugin = "platform:/plugin/de.persosim.rcp/";
	public static final String persoPath = "personalization/profiles/";
	public static final String persoFilePrefix = "Profile";
	public static final String persoFilePostfix = ".xml";
	
	private int simPort = DEFAULT_SIM_PORT; // default
	
	static {
		//register BouncyCastle provider
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}
	
	/**
	 * This constructor is used by the OSGi-service instantiation
	 */
	public PersoSim(){
		currentPersonalization = new MinimumPersonalization();
	}
	
	public PersoSim(String... args) {
		this();
		try {
			CommandParser.handleArgs(this, args);
		} catch (IllegalArgumentException e) {
			System.out.println("simulation aborted, reason is: " + e.getMessage());
		}
		
	}
	
	public void startPersoSim(){
		System.out.println("Welcome to PersoSim");
		PersoSimLogger.init();

		startSimulator();
		final Simulator sim = this;
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				CommandParser.handleUserCommands(sim);
			}
		}).start();
	}
	
	@Override
	public boolean startSimulator() {
		if (simulator != null && simulator.isRunning()) {
			System.out.println("Simulator already running");
			return true;
		}
		
		if (getPersonalization() == null) {
			System.out.println("No personalization available, please load a valid personalization before starting the simulator");
			return false;
		}
		
		SocketSimulator newSimulator = new SocketSimulator(getPersonalization(), simPort);
		
		if(newSimulator.start()) {
			simulator = newSimulator;
			System.out.println("The simulator has been started");
			return true;
		} else{
			return false;
		}

	}
	
	@Override
	public boolean stopSimulator() {
		boolean simStopped = false;
		
		if (simulator != null) {
			simStopped = simulator.stop();
			simulator = null;
			
			if(simStopped) {
				System.out.println("The simulator has been stopped and will no longer respond to incoming APDUs until it is (re-) started");
			}
		}
		
		return simStopped;
	}
	
	@Override
	public boolean restartSimulator() {
		stopSimulator();
		return startSimulator();
	}
	
	@Override
	public boolean exitSimulator() {
		System.out.println(LOG_SIM_EXIT);
		
		boolean stopped = stopSimulator();
		
		if(stopped) {
			System.out.println("The simulator has been terminated and will no longer respond to incoming APDUs or commands");
		}
				
		return stopped;
	}

	@Override
	public Personalization getPersonalization() {
		return currentPersonalization;
	}
	
	/**
	 * This method parses a {@link Personalization} object from a file identified by its name.
	 * @param persoFileName the name of the file to contain the personalization
	 * @return the parsed personalization
	 * @throws FileNotFoundException 
	 * @throws JAXBException if parsing of personalization not successful
	 */
	public static Personalization parsePersonalization(String persoFileName) throws FileNotFoundException, JAXBException {
		File persoFile = new File(persoFileName);
		
		Unmarshaller um = PersoSimJaxbContextProvider.getContext().createUnmarshaller();
		System.out.println("Parsing personalization from file " + persoFileName);
		return (Personalization) um.unmarshal(new FileReader(persoFile));
	}
	
	@Override
	public boolean loadPersonalization(String identifier) {
		currentPersonalization = null;

		//try to parse the given identifier as profile number
		try {
			int personalizationNumber = Integer.parseInt(identifier);
			System.out.println("trying to load personalization profile no: " + personalizationNumber);
			Bundle plugin = Activator.getContext().getBundle();
			
			if(plugin == null) {
				// TODO how to handle this case? Add OSGI requirement?
				System.out.println("unable to resolve bundle \"de.persosim.simulator\" - personalization unchanged");
				return false;
			} else {
				URL url = plugin.getResource(persoPath + persoFilePrefix + String.format("%02d", personalizationNumber) + persoFilePostfix);
				System.out.println("resolved absolute URL for selected profile is: " + url);
				identifier = url.getPath();
			}
		} catch (Exception e) {
			//seems to be a call to load a personalization by path
		}
		
		//actually load perso from the identified file
		try{
			currentPersonalization = parsePersonalization(identifier);
			return restartSimulator();
		} catch(FileNotFoundException | JAXBException e) {
			System.out.println("unable to set personalization, reason is: " + e.getMessage());
			stopSimulator();
			System.out.println("simulation is stopped");
			return false;
		}
	}

	@Override
	public void setPort(int newPort) {
		simPort = newPort;
	}

	@Override
	public int getPort() {
		return simPort;
	}

	@Override
	public byte[] processCommand(byte[] apdu) {
		return simulator.processCommand(apdu);
	}

}
