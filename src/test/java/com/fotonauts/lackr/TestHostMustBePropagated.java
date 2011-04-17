package com.fotonauts.lackr;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.Log4jConfigurer;

public class TestHostMustBePropagated extends TestCase {
	
	public void testProp() throws Exception {
	    
    	Log4jConfigurer.initLogging("classpath:log4j.debug.properties");

		Server backend = new Server();
		backend.addConnector(new SelectChannelConnector());
		backend.setHandler(new AbstractHandler() {

			public void handle(String target, Request baseRequest,
					HttpServletRequest request, HttpServletResponse response)
					throws IOException, ServletException {
				response.setContentType(MimeType.TEXT_HTML);
				response.getWriter().print(request.getHeader("Host") + request.getPathInfo() + " " + new String(FileCopyUtils.copyToByteArray(request.getInputStream())));
				response.flushBuffer();
			}
		});

		System.setProperty("lackr.properties",
				"classpath:/lackr.test.properties");

		backend.start();

		ApplicationContext ctx = new ClassPathXmlApplicationContext("lackr.xml");
		Service service = (Service) ctx.getBean("proxyService");
		service.setBackends("http://localhost:"
				+ backend.getConnectors()[0].getLocalPort());
		service.buildRing();
		Server lackrServer = (Server) ctx.getBean("Server");
		lackrServer.start();

    	HttpClient client = new HttpClient();
    	client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
    	client.setConnectTimeout(5);
    	client.start();
		
    	String uri = "http://localhost:" + lackrServer.getConnectors()[0].getLocalPort() + "/blah/%C3%89.html";

    	ContentExchange e = new ContentExchange(true);
    	e.setURL(uri);
    	e.setRequestHeader("Host", "something");
    	e.setRequestContent(new ByteArrayBuffer("coin"));
    	e.setRequestHeader("Content-Length", "4");
    	client.send(e);
    	
    	while(!e.isDone())
    		Thread.yield();
    	
    	assertEquals(200, e.getResponseStatus());
    	assertEquals("something/blah/\u00C9.html coin", e.getResponseContent());
	}
}
