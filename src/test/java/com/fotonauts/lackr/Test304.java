package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Test;

public class Test304 extends BaseTestLackrFullStack {

	public Test304() throws Exception {
	    super();
    }

	protected ContentExchange run(String testPage) throws IOException, InterruptedException {
		return run(testPage, null);
	}

	protected ContentExchange run(final String testPage, String etag) throws IOException, InterruptedException {
		currentHandler.set(new AbstractHandler() {
			
			@Override
			public void handle(String target, Request request, HttpServletRequest baseRequest, HttpServletResponse response)
			        throws IOException, ServletException {
				writeResponse(response, testPage.getBytes(), MimeType.TEXT_HTML);
			}
		});
		ContentExchange e = new ContentExchange(true);
		e.setURL("http://localhost:" + lackrServer.getConnectors()[0].getLocalPort() + "/page.html");
		if(etag != null)
			e.setRequestHeader(HttpHeaders.IF_NONE_MATCH, etag);
		client.send(e);
		while (!e.isDone())
			Thread.sleep(10);
		return e;
	}

	@Test
	public void testEtagGeneration() throws Exception {
		ContentExchange e1 = run("blah");
		String etag1 = e1.getResponseFields().getStringField("etag");
		assertNotNull("e1 has etag", etag1);

		ContentExchange e2 = run("blih");
		String etag2 = e2.getResponseFields().getStringField("etag");
		assertNotNull("e2 has etag", etag2);

		assertTrue("etags are different", !etag1.equals(etag2));
	}

	@Test
	public void testEtagAndIfNoneMatch() throws Exception {
		ContentExchange e1 = run("blah");
		String etag1 = e1.getResponseFields().getStringField("etag");
		assertNotNull("e1 has etag", etag1);

		ContentExchange e2 = run("blih", etag1);
		assertEquals(e2.getResponseStatus(), HttpStatus.OK_200);

		ContentExchange e3 = run("blah", etag1);
		assertEquals(e3.getResponseStatus(), HttpStatus.NOT_MODIFIED_304);
	}
}
