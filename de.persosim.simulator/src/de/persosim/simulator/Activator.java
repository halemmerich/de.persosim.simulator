package de.persosim.simulator;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
	
	private static BundleContext context;
	private static List<SocketSimulator> simulators = new ArrayList<>();
	
	public static BundleContext getContext() {
		return context;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		Activator.context = context;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		context = null;
		for (SocketSimulator socketSimulator : simulators) {
			socketSimulator.stop();
		}
		simulators = new ArrayList<>();
	}

	public static void addForTermination(SocketSimulator sim){
		simulators.add(sim);
	}
	
}
