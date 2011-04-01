package com.fotonauts.lackr;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.Log4jConfigurer;

public class TestESI extends TestCase {

	private static final String ESI_JSON = "{ some: \"json crap\" }";

	private static final String ESI_HTML = "<p> un bout de html avec des \" et des \\ dedans\nsur plusieurs lignes\npour faire joli</p>";

	Server backend;
	Server lackrServer;
	HttpClient client;

	final StringBuffer page = new StringBuffer();

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		Log4jConfigurer.initLogging("classpath:log4j.debug.properties");

		backend = new Server();

		backend.addConnector(new SelectChannelConnector());

		backend.setHandler(new AbstractHandler() {

			public void handle(String target, Request baseRequest, HttpServletRequest request,
			        HttpServletResponse response) throws IOException, ServletException {
				if (request.getPathInfo().equals("/page.html")) {
					response.setContentType(MimeType.TEXT_HTML);
					response.getWriter().print(page);
					response.getWriter().flush();
					response.flushBuffer();
				} else if (request.getPathInfo().equals("/esi.json")) {
					response.setContentType(MimeType.APPLICATION_JSON);
					response.getWriter().print(ESI_JSON);
					response.getWriter().flush();
					response.flushBuffer();
				} else if (request.getPathInfo().equals("/esi.html")) {
					response.setContentType(MimeType.TEXT_HTML);
					response.getWriter().print(ESI_HTML);
					response.getWriter().flush();
					response.flushBuffer();
				}
			}
		});

		System.setProperty("lackr.properties", "classpath:/lackr.test.properties");

		backend.start();
		backend.getConnectors()[0].getLocalPort();

		ApplicationContext ctx = new ClassPathXmlApplicationContext("lackr.xml");
		Service service = (Service) ctx.getBean("proxyService");
		service.setBackend("http://localhost:" + backend.getConnectors()[0].getLocalPort());
		lackrServer = (Server) ctx.getBean("Server");
		lackrServer.start();

		client = new HttpClient();
		client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
		client.setConnectTimeout(5);
		client.start();
	}
	
	public String expand(String testPage) throws IOException, InterruptedException {
		ContentExchange e = new ContentExchange(true);
		page.setLength(0);
		page.append(testPage);
		e.setURL("http://localhost:" + lackrServer.getConnectors()[0].getLocalPort() + "/page.html");
		client.send(e);
		while (!e.isDone())
			Thread.sleep(10);
		if(e.getResponseStatus() != 200)
			return null;
		return new String(e.getResponseContentBytes());
	}

	public void testHtmlInHtml() throws Exception {
		String result = expand("before\n<!--# include virtual=\"/esi.html\" -->\nafter\n");
		assertEquals("before\n" + ESI_HTML + "\nafter\n", result);
	}
	
	public void testJsInHtmlShouldCrash() throws Exception {	
		assertNull(expand("<!--# include virtual=\"/esi.json\" -->"));
	}

	public void testHtmlInJs() throws Exception {
		String result = expand("before\n\"ssi:include:virtual:/esi.html\"\nafter\n");
		assertEquals("before\n" + JSONObject.quote(ESI_HTML) + "\nafter\n", result);
	}

	public void testJsInJs() throws Exception {
		String result = expand("before\n\"ssi:include:virtual:/esi.json\"\nafter\n");
		assertEquals("before\n" + ESI_JSON + "\nafter\n", result);
	}

	public void testHtmlInMlJs() throws Exception {
		String result = expand("before\n\\u003C!--# include virtual=\\\"/esi.html\\\" --\\u003E\nafter\n");
		String json = JSONObject.quote(ESI_HTML);
		assertEquals("before\n" + json.substring(1, json.length() - 1) + "\nafter\n", result);
	}

	public void testJInMlJsShouldCrash() throws Exception {
		assertNull(expand("before\n\\u003C!--# include virtual=\\\"/esi.json\\\" --\\u003E\nafter\n"));
	}

	public void testHttp() throws Exception {
		String result = expand("before\nhttp://esi.include.virtual/esi.html#\nafter\n");
		assertEquals("before\n" + ESI_HTML + "\nafter\n", result);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		lackrServer.stop();
		backend.stop();
	}

}
