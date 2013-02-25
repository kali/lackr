package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.StringTokenizer;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(value = Parameterized.class)
public class TestFemtor extends BaseTestLackrFullStack {

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { { false }, { true } });
    }
   
    
	public TestFemtor(boolean inProcess) throws Exception {
		super(inProcess);
	}

	@Test(timeout = 500)
	public void testFemtor() throws Exception {
		ContentExchange e = new ContentExchange(true);
		e.setURL("http://localhost:" + lackrServer.getConnectors()[0].getLocalPort() + "/femtor/hi");
		client.send(e);
    	while (!e.isDone())
    		Thread.sleep(10);
    	assertEquals("Hi from dummy femtor\n", e.getResponseContent());
	}

	@Test(timeout = 500)
	public void testFemtorCrashServlet() throws Exception {
		ContentExchange e = new ContentExchange(true);
		e.setURL("http://localhost:" + lackrServer.getConnectors()[0].getLocalPort() + "/femtor/crash/servlet");
		client.send(e);
    	while (!e.isDone())
    		Thread.sleep(10);
    	assertEquals(50, e.getResponseStatus() / 10); // expect 50x
    	assertTrue(e.getResponseContent().contains("catch me or you're dead."));
	}
	
    @Test(timeout = 500)
    public void testFemtorCrashRE() throws Exception {
        ContentExchange e = new ContentExchange(true);
        e.setURL("http://localhost:" + lackrServer.getConnectors()[0].getLocalPort() + "/femtor/crash/re");
        client.send(e);
        while (!e.isDone())
            Thread.sleep(10);
        assertEquals(50, e.getResponseStatus() / 10); // expect 50x
        assertTrue(e.getResponseContent().contains("catch me or you're dead."));
    }

    @Test(timeout = 500)
    public void testFemtorCrashError() throws Exception {
        ContentExchange e = new ContentExchange(true);
        e.setURL("http://localhost:" + lackrServer.getConnectors()[0].getLocalPort() + "/femtor/crash/error");
        client.send(e);
        while (!e.isDone())
            Thread.sleep(10);
        assertEquals(50, e.getResponseStatus() / 10); // expect 50x
        assertTrue(e.getResponseContent().contains("catch me or you're dead."));
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
        assertEquals("X-Ftn-IID: null", tokenizer.nextToken());
        assertFalse(tokenizer.hasMoreElements());
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
        assertEquals("X-Ftn-IID: TheIIDValue", tokenizer.nextToken());
        assertFalse(tokenizer.hasMoreElements());
	}

    @Test(timeout = 500)
    public void testFemtorRewrite() throws Exception {
        ContentExchange e = new ContentExchange(true);
        e.setURL("http://localhost:" + lackrServer.getConnectors()[0].getLocalPort() + "/rewrite");
        client.send(e);
        while (!e.isDone())
            Thread.sleep(10);
        assertEquals("Hi from dummy femtor\n", e.getResponseContent());
    }
    
}
