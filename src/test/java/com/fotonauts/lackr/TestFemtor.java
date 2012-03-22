package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.jetty.client.ContentExchange;
import org.junit.Test;

public class TestFemtor extends BaseTestLackrFullStack {

	public TestFemtor() throws Exception {
		super();
	}

	@Test(timeout = 100)
	public void testFemtor() throws Exception {
		ContentExchange e = new ContentExchange(true);
		e.setURL("http://localhost:" + lackrServer.getConnectors()[0].getLocalPort() + "/femtor/hi");
		client.send(e);
    	while (!e.isDone())
    		Thread.sleep(10);
    	assertEquals("Hi from dummy femtor\n", e.getResponseContent());
	}

	@Test(timeout = 100)
	public void testFemtorCrash() throws Exception {
		ContentExchange e = new ContentExchange(true);
		e.setURL("http://localhost:" + lackrServer.getConnectors()[0].getLocalPort() + "/femtor/crash");
		client.send(e);
    	while (!e.isDone())
    		Thread.sleep(10);
    	assertEquals(502, e.getResponseStatus());
    	System.err.println(e.getResponseContent());
    	assertTrue(e.getResponseContent().contains("catch me or you're dead.\n"));
	}

}
