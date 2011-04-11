package com.fotonauts.lackr;

import java.io.IOException;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;

public class Test304 extends BaseTestSubstitution {

	public ContentExchange run(String testPage) throws IOException, InterruptedException {
		return run(testPage, null);
	}

	public ContentExchange run(String testPage, String etag) throws IOException, InterruptedException {
		ContentExchange e = new ContentExchange(true);
		page.setLength(0);
		page.append(testPage);
		e.setURL("http://localhost:" + lackrServer.getConnectors()[0].getLocalPort() + "/page.html");
		if(etag != null)
			e.setRequestHeader(HttpHeaders.IF_NONE_MATCH, etag);
		client.send(e);
		while (!e.isDone())
			Thread.sleep(10);
		return e;
	}

	public void testEtagGeneration() throws Exception {
		ContentExchange e1 = run("blah");
		String etag1 = e1.getResponseFields().getStringField("etag");
		assertNotNull("e1 has etag", etag1);

		ContentExchange e2 = run("blih");
		String etag2 = e2.getResponseFields().getStringField("etag");
		assertNotNull("e2 has etag", etag2);

		assertTrue("etags are different", !etag1.equals(etag2));
	}

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
