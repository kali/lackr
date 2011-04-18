package com.fotonauts.lackr;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Test;
import org.springframework.util.FileCopyUtils;

public class TestPropagation extends BaseTestLackrFullStack {

	public TestPropagation(String clientImplementation) throws Exception {
	    super(clientImplementation);
    }

	@Test
	public void hostProp() throws Exception {

		currentHandler.set(new AbstractHandler() {

			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request,
			        HttpServletResponse response) throws IOException, ServletException {
				writeResponse(response, request.getHeader("Host").getBytes(), MimeType.TEXT_HTML);
			}
		});

		ContentExchange e = createExchange("http://localhost:" + lackrServer.getConnectors()[0].getLocalPort() + "/");
		e.setRequestHeader("Host", "something");
		runRequest(e, "something");
	}
	
	@Test
	public void userAgenProp() {

		currentHandler.set(new AbstractHandler() {

			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request,
			        HttpServletResponse response) throws IOException, ServletException {
				writeResponse(response, request.getHeader("User-Agent").getBytes(), MimeType.TEXT_HTML);
			}
		});

		ContentExchange e = createExchange("http://localhost:" + lackrServer.getConnectors()[0].getLocalPort() + "/");
		e.setRequestHeader("User-Agent", "something");
		runRequest(e, "something");		
	}
	
	@Test
	public void reqBodyProp() {
		currentHandler.set(new AbstractHandler() {

			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request,
			        HttpServletResponse response) throws IOException, ServletException {
				writeResponse(response, FileCopyUtils.copyToByteArray(request.getInputStream()), MimeType.TEXT_HTML);
			}
		});

		ContentExchange e = createExchange("http://localhost:" + lackrServer.getConnectors()[0].getLocalPort() + "/");
		e.setMethod("POST");
		e.setRequestContent(new ByteArrayBuffer("coin"));
		e.setRequestHeader("Content-Length", "4");
		runRequest(e, "coin");
    }
}
