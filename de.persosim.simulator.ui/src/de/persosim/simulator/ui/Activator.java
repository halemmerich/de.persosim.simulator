package de.persosim.simulator.ui;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceException;
import org.osgi.util.tracker.ServiceTracker;

import de.persosim.simulator.Simulator;

/**
 * The activator for this bundle.
 * @author mboonk
 *
 */
public class Activator implements BundleActivator {

	private static BundleContext context;
	private static ServiceTracker<Simulator, Simulator> serviceTracker;
	
	public static void executeUserCommands(String command){
		Simulator sim = serviceTracker.getService();
		if (sim != null){
			sim.executeUserCommands(command);
		} else {
			throw new ServiceException("The Simulator service could not be found");
		}
	}

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;

		
		serviceTracker = new ServiceTracker<Simulator, Simulator>(bundleContext, Simulator.class.getName(), null);
		serviceTracker.open();
		
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
	}

}
