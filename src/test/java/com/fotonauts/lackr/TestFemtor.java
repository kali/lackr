package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.StringTokenizer;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;
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
    	assertTrue(e.getResponseContent().contains("catch me or you're dead.\n"));
	}
	
	@Test
	public void testFemtorQuery() throws Exception {
		ContentExchange e = new ContentExchange(true);
		e.addRequestHeader("X-Ftn-OperationId", "someid");
		e.setURL("http://localhost:" + lackrServer.getConnectors()[0].getLocalPort() + "/femtor/dump?blah=12&blih=42");
		client.send(e);
    	while (!e.isDone())
    		Thread.sleep(10);
//    	System.err.println(e.getResponseContent());
    	assertEquals(200, e.getResponseStatus());
    	StringTokenizer tokenizer = new StringTokenizer(e.getResponseContent(), "\n");
    	assertEquals("Hi from dummy femtor", tokenizer.nextToken());
    	assertEquals("method: GET", tokenizer.nextToken());
    	assertEquals("pathInfo: /femtor/dump", tokenizer.nextToken());
    	assertEquals("getQueryString: blah=12&blih=42", tokenizer.nextToken());
    	assertEquals("getRequestURI: /femtor/dump", tokenizer.nextToken());
        assertEquals("X-Ftn-OperationId: someid", tokenizer.nextToken());
        assertEquals("x-ftn-operationid: someid", tokenizer.nextToken());
    	assertEquals("parameterNames: [blah, blih]", tokenizer.nextToken());
	}

	@Test
	public void testFemtorBodyQuery() throws Exception {
		ContentExchange e = new ContentExchange(true);
		e.addRequestHeader("X-Ftn-OperationId", "someid");
		e.setURL("http://localhost:" + lackrServer.getConnectors()[0].getLocalPort() + "/echobody");
		e.setRequestContent(new ByteArrayBuffer("yop yop yop".getBytes()));
		client.send(e);
    	while (!e.isDone())
    		Thread.sleep(10);
    	System.err.println(e.getResponseContent());
    	assertEquals(200, e.getResponseStatus());
    	assertEquals("yop yop yop", e.getResponseContent());
	}

	@Test
	public void testFemtorESIQuery() throws Exception {
		ContentExchange e = new ContentExchange(true);
        e.addRequestHeader("X-Ftn-OperationId", "someid");
		e.setURL("http://localhost:" + lackrServer.getConnectors()[0].getLocalPort() + "/femtor/dumpwrapper");
		client.send(e);
    	while (!e.isDone())
    		Thread.sleep(10);
//    	System.err.println(e.getResponseContent());
    	assertEquals(200, e.getResponseStatus());
    	StringTokenizer tokenizer = new StringTokenizer(e.getResponseContent(), "\n");
    	assertEquals("Hi from dummy femtor", tokenizer.nextToken());
    	assertEquals("method: GET", tokenizer.nextToken());
    	assertEquals("pathInfo: /femtor/dump", tokenizer.nextToken());
    	assertEquals("getQueryString: tut=pouet", tokenizer.nextToken());
    	assertEquals("getRequestURI: /femtor/dump", tokenizer.nextToken());
        assertEquals("X-Ftn-OperationId: someid", tokenizer.nextToken());
        assertEquals("x-ftn-operationid: someid", tokenizer.nextToken());
    	assertEquals("parameterNames: [tut]", tokenizer.nextToken());
	}
	
}
