package de.persosim.simulator.ui;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import de.persosim.simulator.Simulator;

/**
 * The activator for this bundle.
 * @author mboonk
 *
 */
public class Activator implements BundleActivator {

	private static BundleContext context;
	private static Simulator sim;

	public static Simulator getSim() {
		return sim;
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

		
		ServiceListener serviceListener = new ServiceListener() {
			
			@Override
			public void serviceChanged(ServiceEvent event) {
				ServiceReference<?> serviceReference = event.getServiceReference();
				switch (event.getType()) {
				case ServiceEvent.REGISTERED:
					sim = (Simulator) context.getService(serviceReference);
					break;
				default:
					break;
				}
				
			}
		};
		
		bundleContext.addServiceListener(serviceListener);
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
