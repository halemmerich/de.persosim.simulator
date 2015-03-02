package de.persosim.simulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;

import de.persosim.simulator.perso.Personalization;

public class CommandParser {


	public static final String CMD_START = "start";
	public static final String CMD_RESTART = "restart";
	public static final String CMD_STOP = "stop";
	public static final String CMD_EXIT = "exit";
	public static final String CMD_SET_PORT = "setport";
	public static final String ARG_SET_PORT = "-port";
	public static final String CMD_LOAD_PERSONALIZATION = "loadperso";
	public static final String ARG_LOAD_PERSONALIZATION = "-perso";
	public static final String CMD_SEND_APDU = "sendapdu";
	public static final String CMD_HELP = "help";
	public static final String ARG_HELP = "-h";
	public static final String CMD_CONSOLE_ONLY = "--consoleOnly";

	public static final String LOG_UNKNOWN_ARG  = "unknown argument";
	public static final String LOG_NO_OPERATION = "nothing to process";
	
	private static boolean processingCommandLineArguments = false;
	private static boolean executeUserCommands = false;
	
	public static final String persoPlugin = "platform:/plugin/de.persosim.rcp/";
	public static final String persoPath = "personalization/profiles/";
	public static final String persoFilePrefix = "Profile";
	public static final String persoFilePostfix = ".xml";


	/**
	 * This method processes the command for exiting the simulator.
	 * @param args arguments that may contain an exit command
	 * @return whether exiting was successful
	 */
	public static boolean cmdExitSimulator(Simulator sim, List<String> args) {
		if((args != null) && (args.size() >= 1)) {
			String cmd = args.get(0);
			
			if(cmd.equals(CMD_EXIT)) {
				args.remove(0);
				return sim.exitSimulator();
			}
		}
		
		return false;
	}
	
	/**
	 * This method processes the command for starting the simulator.
	 * @param args arguments that may contain a start command
	 * @return whether instantiation and starting was successful
	 */
	public static boolean cmdStartSimulator(Simulator sim, List<String> args) {
		if((args != null) && (args.size() >= 1)) {
			String cmd = args.get(0);
			
			if(cmd.equals(CMD_START)) {
				args.remove(0);
				return sim.startSimulator();
			}
		}
		
		return false;
	}
	
	/**
	 * This method processes the command for stopping the simulator.
	 * @param args arguments that may contain a stop command
	 * @return whether stopping was successful
	 */
	public static boolean cmdStopSimulator(Simulator sim, List<String> args) {
		if((args != null) && (args.size() >= 1)) {
			String cmd = args.get(0);
			
			if(cmd.equals(CMD_STOP)) {
				args.remove(0);
				return sim.stopSimulator();
			}
		}
		
		return false;
	}

	
	/**
	 * This method processes the command for restarting the simulator.
	 * @param args arguments that may contain a restart command
	 * @return whether restarting was successful
	 */
	public static boolean cmdRestartSimulator(Simulator sim, List<String> args) {
		if((args != null) && (args.size() >= 1)) {
			String cmd = args.get(0);
			
			if(cmd.equals(CMD_RESTART)) {
				args.remove(0);
				return sim.restartSimulator();
			}
		}
		
		return false;
	}

	
	/**
	 * This method processes the send APDU command according to the provided arguments.
	 * @param args the arguments provided for processing
	 * @return whether processing has been successful
	 */
	public static String cmdSendApdu(Simulator sim, List<String> args) {
		if((args != null) && (args.size() >= 2)) {
			String cmd = args.get(0);
			
			if(cmd.equals(CMD_SEND_APDU)) {
				String result;
				
				try{
	    			result = sendCmdApdu(sim, "sendApdu " + args.get(1));
	    			args.remove(0);
	    			args.remove(0);
	    			return result;
	    		} catch(RuntimeException e) {
	    			result = "unable to send APDU, reason is: " + e.getMessage();
	    			args.remove(0);
	    			return result;
	    		}
			} else{
				return "no send APDU command";
			}
		} else{
			return "missing parameter for APDU content";
		}
	}
	
	/**
	 * This method prints the help menu to the command line.
	 */
	private static void printHelpArgs() {
		System.out.println("Available commands:");
		System.out.println(ARG_LOAD_PERSONALIZATION + " <file name>");
		System.out.println(ARG_SET_PORT + " <port number>");
		System.out.println(ARG_HELP);
	}
	
	/**
	 * This method prints the help menu to the user command line.
	 */
	private static void printHelpCmd() {
		System.out.println("Available commands:");
		System.out.println(CMD_SEND_APDU + " <hexstring>");
		System.out.println(CMD_LOAD_PERSONALIZATION + " <file name>");
		System.out.println(CMD_SET_PORT + " <port number>");
		System.out.println(CMD_START);
		System.out.println(CMD_RESTART);
		System.out.println(CMD_STOP);
		System.out.println(CMD_EXIT);
		System.out.println(CMD_HELP);
	}
	
	/**
	 * This method processes the load personalization command according to the provided arguments.
	 * @param args the arguments provided for processing the load personalization command
	 * @return whether processing of the load personalization command has been successful
	 */
	public static boolean cmdLoadPersonalization(Simulator sim, List<String> args) {
		
		if((args != null) && (args.size() >= 2)) {
			String cmd = args.get(0);
			
			if(cmd.equals(CMD_LOAD_PERSONALIZATION) || cmd.equals(ARG_LOAD_PERSONALIZATION)) {
				String arg = args.get(1);
				
				args.remove(0);
    			args.remove(0);
				Personalization perso = getPerso(arg);
				if (perso != null) {
					boolean persoLoaded = sim.loadPersonalization(perso);
					if (persoLoaded) {
						sim.restartSimulator();
					}
					return persoLoaded;
    			}
				// the personalization could not be loaded
				sim.stopSimulator();
			}
		}
		
		return false;
	}
	
	private static Personalization getPerso(String identifier){

		//try to parse the given identifier as profile number
		try {
			int personalizationNumber = Integer.parseInt(identifier);
			System.out.println("trying to load personalization profile no: " + personalizationNumber);
			Bundle plugin = Activator.getContext().getBundle();
			
			if(plugin == null) {
				// TODO how to handle this case? Add OSGI requirement?
				System.out.println("unable to resolve bundle \"de.persosim.simulator\" - personalization unchanged");
				return null;
			} else {
				URL url = plugin.getResource(persoPath + persoFilePrefix + String.format("%02d", personalizationNumber) + persoFilePostfix);
				System.out.println("resolved absolute URL for selected profile is: " + url);
				identifier = url.getPath();
			}
		} catch (Exception e) {
			//seems to be a call to load a personalization by path
		}
		return null;
	} 
	
	public static void executeUserCommands(Simulator sim, String... args) {
		if((args == null) || (args.length == 0)) {System.out.println(LOG_NO_OPERATION); return;}
		
		ArrayList<String> currentArgs = new ArrayList<String>(Arrays.asList(args)); // plain return value of Arrays.asList() does not support required remove operation
		
		for(int i = currentArgs.size() - 1; i >= 0; i--) {
			if(currentArgs.get(i) == null) {
				currentArgs.remove(i);
			}
		}
		
		if(currentArgs.size() == 0) {System.out.println(LOG_NO_OPERATION); return;}
		
		int noOfArgsWhenCheckedLast;
		while(currentArgs.size() > 0) {
			noOfArgsWhenCheckedLast = currentArgs.size();
			
			cmdLoadPersonalization(sim, currentArgs);
			cmdSendApdu(sim, currentArgs);
			cmdStartSimulator(sim, currentArgs);
			cmdRestartSimulator(sim, currentArgs);
			cmdStopSimulator(sim, currentArgs);
			cmdExitSimulator(sim, currentArgs);
			cmdHelp(currentArgs);
			
			if(noOfArgsWhenCheckedLast == currentArgs.size()) {
				//first command in queue has not been processed
				String currentArgument = currentArgs.get(0);
				System.out.println(LOG_UNKNOWN_ARG + " \"" + currentArgument + "\" will be ignored, processing of arguments stopped");
				currentArgs.remove(0);
				printHelpCmd();
				break;
			}
		}
		
	}
	
	/**
	 * This method implements the execution of commands initiated by command line arguments.
	 * @param args the parsed commands and arguments
	 */
	public  static void handleArgs(Simulator sim, String... args) {
		if((args == null) || (args.length == 0)) {System.out.println(LOG_NO_OPERATION); return;}
		
		processingCommandLineArguments = true;
		
		List<String> currentArgs = Arrays.asList(args);
		// the list returned by Arrays.asList() does not support optional but required remove operation
		currentArgs = new ArrayList<String>(currentArgs);
		
		for(int i = currentArgs.size() - 1; i >= 0; i--) {
			if(currentArgs.get(i) == null) {
				currentArgs.remove(i);
			}
		}
		
		if(currentArgs.size() == 0) {System.out.println(LOG_NO_OPERATION); return;}
		
		int noOfArgsWhenCheckedLast;
		while(currentArgs.size() > 0) {
			noOfArgsWhenCheckedLast = currentArgs.size();
			
			cmdLoadPersonalization(sim, currentArgs);
			cmdHelp(currentArgs);
			
			if(currentArgs.size() > 0) {
				if(currentArgs.get(0).equals(CMD_CONSOLE_ONLY)) {
					// do no actual processing, i.e. prevent simulator from logging unknown command error as command has already been processed
		        	// command is passed on as part of unprocessed original command line arguments
		        	currentArgs.remove(0);
				}
			}
			
			if(noOfArgsWhenCheckedLast == currentArgs.size()) {
				//first command in queue has not been processed
				String currentArgument = currentArgs.get(0);
				System.out.println(LOG_UNKNOWN_ARG + " \"" + currentArgument + "\" will be ignored, processing of arguments stopped");
				currentArgs.remove(0);
				printHelpCmd();
				break;
			}
		}
		
		processingCommandLineArguments = false;
		
	}
	
	public static boolean cmdHelp(List<String> args) {
		if((args != null) && (args.size() >= 1)) {
			String cmd = args.get(0);
			
			if(cmd.equals(CMD_HELP) || cmd.equals(ARG_HELP)) {
				args.remove(0);
				
				if(processingCommandLineArguments) {
					printHelpArgs();
				} else{
					printHelpCmd();
				}
				return true;
			}
		}
		
		return false;
	}

	/**
	 * Transmit an APDU to the card
	 * 
	 * @param cmd
	 *            string containing the command
	 * @return the response
	 */
	private static String sendCmdApdu(Simulator sim, String cmd) {
		cmd = cmd.trim();

		Pattern cmdSendApduPattern = Pattern
				.compile("^send[aA]pdu ([0-9a-fA-F\\s]+)$");
		Matcher matcher = cmdSendApduPattern.matcher(cmd);
		if (!matcher.matches()) {
			throw new RuntimeException("invalid arguments to sendApdu");
		}
		String apdu = matcher.group(1);
		return exchangeApdu(sim, apdu);

	}
	
	/**
	 * Transmit the given APDU to the simulator, which processes it and returns
	 * the response. The response APDU is received from the simulator via its
	 * socket interface and returned to the caller as HexString.
	 * 
	 * @param cmdApdu
	 *            HexString containing the CommandAPDU
	 * @return
	 */
	private static String exchangeApdu(Simulator sim, String cmdApdu) {
		//FIXME: remove this method or move the CommandParser
		return exchangeApdu(cmdApdu, Simulator.DEFAULT_SIM_HOST, Simulator.DEFAULT_SIM_PORT);
	}

	/**
	 * Transmit the given APDU to the simulator identified by host name and port
	 * number, where it will be processed and answered by a response. The
	 * response APDU is received from the simulator via its socket interface and
	 * returned to the caller as HexString.
	 * 
	 * @param cmdApdu
	 *            HexString containing the CommandAPDU
	 * @param host
	 *            the host to contact
	 * @param port
	 *            the port to query
	 * @return the response
	 */
	private static String exchangeApdu(String cmdApdu, String host, int port) {
		cmdApdu = cmdApdu.replaceAll("\\s", ""); // remove any whitespace

		Socket socket;
		try {
			socket = new Socket(host, port);
		} catch (IOException e) {
			socket = null;
			showExceptionToUser(e);
			return null;
		}

		PrintStream out = null;
		BufferedReader in = null;
		try {
			out = new PrintStream(socket.getOutputStream());
			in = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
		} catch (IOException e) {
			showExceptionToUser(e);
		}

		out.println(cmdApdu);
		out.flush();

		String respApdu = null;
		try {
			respApdu = in.readLine();
		} catch (IOException e) {
			showExceptionToUser(e);
		} finally {
			System.out.println("> " + cmdApdu);
			System.out.println("< " + respApdu);
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					showExceptionToUser(e);
				}
			}
		}

		return respApdu;
		
	}
	
	/**
	 * This method parses the provided String object for commands and possible
	 * arguments. First the provided String is trimmed. If the String is empty,
	 * the returned array will be of length 0. If the String does not contain at
	 * least one space character ' ', the whole String will be returned as first
	 * and only element of an array of length 1. If the String does contain at
	 * least one space character ' ', the substring up to but not including the
	 * position of the first occurrence will be the first element of the
	 * returned array. The rest of the String will be trimmed and, if not of
	 * length 0, form the second array element.
	 * 
	 * IMPL extend to parse for multiple arguments add recognition of "
	 * characters as indication of file names allowing white spaces in between.
	 * 
	 * @param args
	 *            the argument String to be parsed
	 * @return the parsed arguments
	 */
	public static String[] parseCommand(String args) {
		String argsInput = args.trim();
		
		int index = argsInput.indexOf(" ");
		
		if(index >= 0) {
			String cmd = argsInput.substring(0, index);
			String params = argsInput.substring(index).trim();
			return new String[]{cmd, params};
		} else{
			if(argsInput.length() > 0) {
				return new String[]{argsInput};
			} else{
				return new String[0];
			}
		}
	}
	
	public static void executeUserCommands(Simulator sim, String cmd) {
		String trimmedCmd = cmd.trim();
		String[] args = parseCommand(trimmedCmd);
		
		executeUserCommands(sim, args);
	}
	
	public static void showExceptionToUser(Exception e) {
		System.out.println("Exception: " + e.getMessage());
		e.printStackTrace();
	}
	
	/**
	 * This method implements the behavior of the user command prompt. E.g.
	 * prints the prompt, reads the user commands and forwards this to the the
	 * execution method for processing. Only one command per invocation of the
	 * execution method is allowed. The first argument provided must be the
	 * command, followed by an arbitrary number of parameters. If the number of
	 * provided parameters is higher than the number expected by the command,
	 * the surplus parameters will be ignored.
	 */
	static void handleUserCommands(Simulator sim) {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		executeUserCommands = true;
		while (executeUserCommands) {
			System.out.println("PersoSim commandline: ");
			String cmd = null;
			try {
				cmd = br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				if (cmd != null) {
					cmd = cmd.trim();
					String[] args = parseCommand(cmd);
					executeUserCommands(sim, args);
				}
			} catch (RuntimeException e) {
				showExceptionToUser(e);
			}
		}
	}
}
