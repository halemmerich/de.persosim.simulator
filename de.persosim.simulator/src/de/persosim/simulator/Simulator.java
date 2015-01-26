package de.persosim.simulator;

import de.persosim.simulator.perso.Personalization;

public interface Simulator {

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
	public static final int DEFAULT_SIM_PORT = 9876;
	public static final String DEFAULT_SIM_HOST = "localhost";

	/**
	 * This method handles instantiation and (re)start of the SocketSimulator.
	 * @return whether instantiation and starting was successful
	 */
	public abstract boolean startSimulator();

	/**
	 * Stops the simulator thread and returns when the thread is stopped.
	 * @return whether stopping was successful
	 */
	public abstract boolean stopSimulator();

	/**
	 * This method restarts the simulator.
	 * @return whether restarting has been successful
	 */
	public abstract boolean restartSimulator();

	/**
	 * This method stops the simulator and no longer accepts user input.
	 * @return whether the simulator has been stopped
	 */
	public abstract boolean exitSimulator();

	/**
	 * This method returns the content of {@link #currentPersonalization}, the
	 * currently used personalization. If no personalization is set, i.e. the
	 * variable is null, it will be set to the default personalization which
	 * will be returned thereafter. This mode of accessing personalization
	 * opportunistic assumes that a personalization will always be set and
	 * generating a default personalization is an overhead only to be spent as a
	 * measure of last resort.
	 * 
	 * @return the currently used personalization
	 */
	public abstract Personalization getPersonalization();

	public abstract boolean loadPersonalization(String identifier);

	/**
	 * This method implements the execution of commands initiated by user interaction.
	 * It processes user commands based on a single String containing the whole command and all of its parameters.
	 * @param cmd the single String command
	 */
	public abstract void executeUserCommands(String cmd);

	/**
	 * This method implements the execution of commands initiated by user interaction.
	 * It processes user commands based on single String representing the command and all of its parameters.
	 * @param args the parsed commands and arguments
	 */
	public abstract void executeUserCommands(String... args);

}