package com.fotonauts.lackr;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.Log4jConfigurer;

import junit.framework.TestCase;

public abstract class BaseTestSubstitution extends TestCase {

	Server backend;
	Server lackrServer;
	HttpClient client;
	final StringBuffer page = new StringBuffer();
	static final String ESI_HTML = "<p> un bout de html avec des \" et des \\ dedans\nsur plusieurs lignes\npour faire joli</p>";
	static final String ESI_JSON = "{ some: \"json crap\" }";

	public BaseTestSubstitution() {
		super();
	}

	public BaseTestSubstitution(String name) {
		super(name);
	}

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
    			} else if (request.getPathInfo().equals("/empty.html")) {
    				response.setContentType(MimeType.TEXT_HTML);
    				response.getWriter().print("");
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
    	service.setBackends("http://localhost:" + backend.getConnectors()[0].getLocalPort());
    	service.buildRing();
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

	@Override
    protected void tearDown() throws Exception {
    	super.tearDown();
    	lackrServer.stop();
    	backend.stop();
    }

}