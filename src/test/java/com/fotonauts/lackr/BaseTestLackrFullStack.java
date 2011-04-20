package com.fotonauts.lackr;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
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
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.Log4jConfigurer;

@Ignore
@RunWith(Parameterized.class)
public class BaseTestLackrFullStack {

	protected Server backend;
	protected Server lackrServer;
	protected HttpClient client;

	protected AtomicReference<Handler> currentHandler;

	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] { { "jettyClient" }, /* { "ahcClient" }, */ { "apacheClient" } });
	}

	public BaseTestLackrFullStack(String clientImplementation) throws Exception {
		Log4jConfigurer.initLogging("classpath:log4j.debug.properties");

		currentHandler = new AtomicReference<Handler>();

		backend = new Server();
		backend.addConnector(new SelectChannelConnector());
		backend.setHandler(new AbstractHandler() {

			public void handle(String target, Request baseRequest, HttpServletRequest request,
			        HttpServletResponse response) throws IOException, ServletException {
				currentHandler.get().handle(target, baseRequest, request, response);
			}
		});
		backend.start();

		System.setProperty("lackr.properties", "classpath:/lackr.test.properties");

		ApplicationContext ctx = new ClassPathXmlApplicationContext("lackr.xml");
		Service service = (Service) ctx.getBean("proxyService");
		service.setClient((BackendClient) ctx.getBean(clientImplementation));
		service.setBackends("http://localhost:" + backend.getConnectors()[0].getLocalPort());
		service.buildRing();
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

}
