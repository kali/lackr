package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.junit.After;
import org.junit.Ignore;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.Log4jConfigurer;

import com.fotonauts.lackr.client.JettyBackend;

@Ignore
public class BaseTestLackrFullStack {

	protected Server backend;
	protected Server femtorStub;
	protected Server lackrServer;
	protected HttpClient client;
	protected ApplicationContext ctx;

	protected AtomicReference<Handler> currentHandler;

	public BaseTestLackrFullStack() throws Exception {
		Log4jConfigurer.initLogging("classpath:log4j.debug.properties");

		currentHandler = new AtomicReference<Handler>();

		backend = new Server();
		backend.addConnector(new SelectChannelConnector());
		backend.setHandler(new AbstractHandler() {

			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			        throws IOException, ServletException {
				currentHandler.get().handle(target, baseRequest, request, response);
			}
		});
		backend.start();

		femtorStub = new Server();
		femtorStub.addConnector(new SelectChannelConnector());
		femtorStub.setHandler(new AbstractHandler() {

			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			        throws IOException, ServletException {
				if(target.equals("/blah"))
					writeResponse(response, "Femtor says hi!".getBytes("UTF-8"), "text/plain");
				else {
					response.setStatus(404);
				}
			}
		});
		femtorStub.start();

		File propFile = File.createTempFile("lackr.test.", ".props");
		propFile.deleteOnExit();

		Properties props = PropertiesLoaderUtils.loadProperties(new ClassPathResource("lackr.test.properties"));
		props.store(new FileOutputStream(propFile), "properties for lackr test run");

		System.setProperty("lackr.properties", "file:" + propFile.getCanonicalPath());

		ctx = new ClassPathXmlApplicationContext("lackr.xml");
//		Service service = (Service) ctx.getBean("proxyService");
/*
		JettyBackend femtorBackend = (JettyBackend) ctx.getBean("femtorBackend");
		femtorBackend.setDirector(new ConstantHttpDirector("http://localhost:" + femtorStub.getConnectors()[0].getLocalPort()));
		*/
		JettyBackend picorBackend = (JettyBackend) ctx.getBean("picorBackend");
		picorBackend.setDirector(new ConstantHttpDirector("http://localhost:" + backend.getConnectors()[0].getLocalPort()));
		
//		service.setBackends(new Backend[] { picorBackend });

		lackrServer = (Server) ctx.getBean("Server");
		lackrServer.start();

		client = new HttpClient();
		client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
		client.setConnectTimeout(5);
		client.start();
	}

	public static void writeResponse(HttpServletResponse response, byte[] data, String type) throws IOException {
		response.setContentType(type);
		response.setCharacterEncoding("UTF-8");
		response.getOutputStream().write(data);
		response.flushBuffer();
	}

	protected ContentExchange createExchange(String url) {
		ContentExchange e = new ContentExchange(true);
		e.setURL(url);
		return e;
	}

	protected void runRequest(ContentExchange e, String expect) {
		try {
			client.send(e);
		} catch (IOException e1) {
			e1.printStackTrace();
			assertTrue("unreachable", false);
		}
		while (!e.isDone())
			Thread.yield();

		assertEquals(200, e.getResponseStatus());
		try {
			assertEquals(expect, e.getResponseContent());
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			assertTrue("unreachable", false);
		}
	}
	
	@After
    public void tearDown() throws Exception {
    	lackrServer.stop();
    	backend.stop();
    }
}
