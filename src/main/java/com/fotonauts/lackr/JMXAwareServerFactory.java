package com.fotonauts.lackr;

import java.lang.management.ManagementFactory;

import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.log.Log;

public class JMXAwareServerFactory {

	public Server getObject() throws Exception {
		Server server = new Server();
		MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
//		server.getContainer().addEventListener(mbContainer);
		try {
			server.addBean(mbContainer);
		} catch (RuntimeException e) {
			System.err.println("Error setting up JMX: " + e);
		}
//		mbContainer.addBean(Log.getRootLogger());
		return server;
	}
}
