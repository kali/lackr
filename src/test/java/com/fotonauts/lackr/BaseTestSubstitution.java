package com.fotonauts.lackr;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public abstract class BaseTestSubstitution extends BaseTestLackrFullStack {

	final StringBuffer page = new StringBuffer();
	static final String ESI_HTML = "<p> un bout de html avec des \" et des \\ dedans\nsur plusieurs lignes\npour faire joli</p>";
	static final String ESI_JSON = "{ \"some\": \"json crap\" }";
	static final String ESI_URL = "http://hou.salut.com/blah&merci.pour.tout";
    static final String ESI_TEXT = "something like a \"base title\" or \nlike a http://url?with=options&blah=blih";
	static final String ESI_MUST = "some text from the template name:{{name}} value:{{value}} some:{{esi.some}}";

	{
    	System.setProperty("lackr.properties", "classpath:/lackr.test.properties");
	}
	
	public BaseTestSubstitution() throws Exception {
		super();
    	remoteControlledStub.set(new AbstractHandler() {
    
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
                } else if (request.getPathInfo().endsWith("method")) {
                    writeResponse(response, request.getMethod().getBytes(), MimeType.TEXT_PLAIN);
    			} else if (request.getPathInfo().endsWith("url")) {
    				writeResponse(response, ESI_URL.getBytes(), MimeType.TEXT_PLAIN);
                } else if (request.getPathInfo().endsWith("text")) {
                    writeResponse(response, ESI_TEXT.getBytes(), MimeType.TEXT_PLAIN);
    			} else if (request.getPathInfo().endsWith("json")) {
    				writeResponse(response, ESI_JSON.getBytes("UTF-8"), MimeType.APPLICATION_JSON);
    			} else if (request.getPathInfo().endsWith("html")) {
    				writeResponse(response, ESI_HTML.getBytes("UTF-8"), MimeType.TEXT_HTML);
    			}
    		}
    	});    
    }

	public String expand(String testPage) throws IOException, InterruptedException, TimeoutException, ExecutionException {
		return expand(testPage, false, null);
	}
	
    public String expand(String testPage, boolean expectNon200) throws IOException, InterruptedException, TimeoutException, ExecutionException {
        return expand(testPage, expectNon200, null);
    }

    public String expand(String testPage, boolean expectNon200, String hostname) throws IOException, InterruptedException, TimeoutException, ExecutionException {
    	org.eclipse.jetty.client.api.Request e = client.newRequest("http://localhost:" + lackrPort + "/page.html");
    	page.setLength(0);
    	page.append(testPage);
    	if(hostname != null)
    	    e.getHeaders().add("Host", hostname);
    	ContentResponse r = e.send();
    	if((r.getStatus() != 200) != expectNon200) {
    		System.err.println(r.getContentAsString());
    		return null;
    	}
    	return r.getContentAsString();
    }


}