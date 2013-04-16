package com.fotonauts.lackr;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.Log4jConfigurer;

import com.ibm.icu.util.TimeZone;


public class Lackr {

	@Option(name = "--log4j", usage = "overrides -d")
	private String log4jConfigFile;

	@Option(name = "-d", usage = "run with debug logging parameters")
	boolean debugMode;

	@Argument
	private List<String> arguments = new ArrayList<String>();

	public static void main(String[] args) throws Exception {
		new Lackr().doMain(args);
	}
	
	public void doMain(String[] args) throws Exception {
		CmdLineParser parser = new CmdLineParser(this);

		try {
		    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		    
			System.setProperty("org.mortbay.util.URI.charset", "UTF-8");
			System.setProperty("org.eclipse.jetty.util.URI.charset", "UTF-8");

			parser.parseArgument(args);

			if (log4jConfigFile != null) {
				Log4jConfigurer.initLogging(log4jConfigFile);
			} else if (debugMode) {
				Log4jConfigurer.initLogging("classpath:log4j.debug.properties");
			} else {
				Log4jConfigurer.initLogging("classpath:log4j.prod.properties");
			}

			if (arguments.isEmpty())
				arguments.add("lackr.xml");
			if (arguments.size() > 1)
				throw new CmdLineException("Only one configuration file allowed");

		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println("java com.fotonauts.lackr.Lackr [options...] [config-file.xml]");
			parser.printUsage(System.err);
			System.err.println();

			return;
		}

		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("lackr.xml");
		Server server = new Server();
		server.setHandler((Handler) ctx.getBean("proxyService"));
		ServerConnector connector = new ServerConnector(server);
		connector.setPort((Integer) ctx.getBean("serverPort"));
		server.addConnector(connector);
		
		server.start();
		server.join();
	}
}
