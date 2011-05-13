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
import org.junit.After;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public abstract class BaseTestSubstitution extends BaseTestLackrFullStack {

	final StringBuffer page = new StringBuffer();
	static final String ESI_HTML = "<p> un bout de html avec des \" et des \\ dedans\nsur plusieurs lignes\npour faire joli</p>";
	static final String ESI_JSON = "{ \"some\": \"json crap\" }";
	static final String ESI_URL = "http://hou.salut.com/blah&merci.pour.tout";
	static final String ESI_MUST = "some text from the template name:{{name}} value:{{value}} some:{{esi.some}}";

	public BaseTestSubstitution(String clientImplementation) throws Exception {
		super(clientImplementation);
    	currentHandler.set(new AbstractHandler() {
    
    		public void handle(String target, Request baseRequest, HttpServletRequest request,
    		        HttpServletResponse response) throws IOException, ServletException {
    			if (request.getPathInfo().equals("/page.html")) {
    				//System.err.println(page.toString());
    				writeResponse(response, page.toString().getBytes("UTF-8"), MimeType.TEXT_HTML);
    			} else if (request.getPathInfo().equals("/empty.html")) {
    				writeResponse(response, "".getBytes(), MimeType.TEXT_HTML);
    			} else if (request.getPathInfo().equals("/500.html")) {
    				response.setStatus(500);
    				response.setHeader("X-SSI-AWARE", "yes");
    				writeResponse(response, "<!-- ignore me -->".getBytes(), MimeType.TEXT_HTML);
    			} else if (request.getPathInfo().endsWith("must")) {
    				writeResponse(response, ESI_MUST.getBytes(), MimeType.TEXT_PLAIN);
    			} else if (request.getPathInfo().endsWith("url")) {
    				writeResponse(response, ESI_URL.getBytes(), MimeType.TEXT_PLAIN);
    			} else if (request.getPathInfo().endsWith("json")) {
    				writeResponse(response, ESI_JSON.getBytes("UTF-8"), MimeType.APPLICATION_JSON);
    			} else if (request.getPathInfo().endsWith("html")) {
    				writeResponse(response, ESI_HTML.getBytes("UTF-8"), MimeType.TEXT_HTML);
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
		return expand(testPage, false);
	}
	
	public String expand(String testPage, boolean expectNon200) throws IOException, InterruptedException {
    	ContentExchange e = new ContentExchange(true);
    	page.setLength(0);
    	page.append(testPage);
    	e.setURL("http://localhost:" + lackrServer.getConnectors()[0].getLocalPort() + "/page.html");
    	client.send(e);
    	while (!e.isDone())
    		Thread.sleep(10);
    	if((e.getResponseStatus() != 200) != expectNon200) {
    		System.err.println(e.getResponseContent());
    		return null;
    	}
    	return new String(e.getResponseContentBytes());
    }

	@After
    public void tearDown() throws Exception {
    	lackrServer.stop();
    	backend.stop();
    }

}