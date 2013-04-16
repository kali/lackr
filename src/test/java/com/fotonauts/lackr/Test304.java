package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Test;

public class Test304 extends BaseTestLackrFullStack {

	public Test304() throws Exception {
	    super();
    }

	protected ContentResponse run(String testPage) throws IOException, InterruptedException, TimeoutException, ExecutionException {
		return run(testPage, null);
	}

	protected ContentResponse run(final String testPage, String etag) throws IOException, InterruptedException, TimeoutException, ExecutionException {
		currentHandler.set(new AbstractHandler() {
			
			@Override
			public void handle(String target, org.eclipse.jetty.server.Request request, HttpServletRequest baseRequest, HttpServletResponse response)
			        throws IOException, ServletException {
				writeResponse(response, testPage.getBytes(), MimeType.TEXT_HTML);
			}
		});
		Request e = client.newRequest("http://localhost:" + lackrPort + "/page.html");
		if(etag != null)
			e.header(HttpHeader.IF_NONE_MATCH.asString(), etag);
		return e.send();
	}

	@Test
	public void testEtagGeneration() throws Exception {
		ContentResponse e1 = run("blah");
		String etag1 = e1.getHeaders().getStringField("etag");
		assertNotNull("e1 has etag", etag1);

		ContentResponse e2 = run("blih");
		String etag2 = e2.getHeaders().getStringField("etag");
		assertNotNull("e2 has etag", etag2);

		assertTrue("etags are different", !etag1.equals(etag2));
	}

	@Test
	public void testEtagAndIfNoneMatch() throws Exception {
	    ContentResponse e1 = run("blah");
		String etag1 = e1.getHeaders().getStringField("etag");
		assertNotNull("e1 has etag", etag1);

		ContentResponse e2 = run("blih", etag1);
		assertEquals(e2.getStatus(), HttpStatus.OK_200);

		ContentResponse e3 = run("blah", etag1);
		assertEquals(e3.getStatus(), HttpStatus.NOT_MODIFIED_304);
	}
}
