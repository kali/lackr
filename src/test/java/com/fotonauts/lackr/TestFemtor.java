package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.StringTokenizer;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
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
		Request r = client.newRequest("http://localhost:" + lackrPort + "/femtor/hi");
		ContentResponse e = r.send();
    	assertEquals("Hi from dummy femtor\n", e.getContentAsString());
	}

	@Test(timeout = 500)
	public void testFemtorCrashServlet() throws Exception {
		Request r = client.newRequest("http://localhost:" + lackrPort +  "/femtor/crash/servlet");
		ContentResponse e = r.send();
    	assertEquals(50, e.getStatus() / 10); // expect 50x
    	assertTrue(e.getContentAsString().contains("catch me or you're dead."));
	}
	
    @Test(timeout = 500)
    public void testFemtorCrashRE() throws Exception {
        Request r = client.newRequest("http://localhost:" + lackrPort +  "/femtor/crash/re");
        ContentResponse e = r.send();
        assertEquals(50, e.getStatus() / 10); // expect 50x
        assertTrue(e.getContentAsString().contains("catch me or you're dead."));
    }

    @Test(timeout = 500)
    public void testFemtorCrashError() throws Exception {
        Request r = client.newRequest("http://localhost:" + lackrPort +  "/femtor/crash/error");
        ContentResponse e = r.send();
        assertEquals(50, e.getStatus() / 10); // expect 50x
        assertTrue(e.getContentAsString().contains("catch me or you're dead."));
    }

    @Test
	public void testFemtorQuery() throws Exception {
        Request r = client.newRequest("http://localhost:" + lackrPort +  "/femtor/dump?blah=12&blih=42");
		r.getHeaders().add("X-Ftn-OperationId", "someid");
        ContentResponse e = r.send();		
//    	System.err.println(e.getResponseContent());
    	assertEquals(200, e.getStatus());
    	StringTokenizer tokenizer = new StringTokenizer(e.getContentAsString(), "\n");
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
        Request r = client.newRequest("http://localhost:" + lackrPort + "/echobody");
		r.getHeaders().add("X-Ftn-OperationId", "someid");
		r.content(new StringContentProvider("yop yop yop", "UTF-8"));
		ContentResponse e = r.send();
    	System.err.println(e.getContentAsString());
    	assertEquals(200, e.getStatus());
    	assertEquals("yop yop yop", e.getContentAsString());
	}

	@Test
	public void testFemtorESIQuery() throws Exception {
        Request r = client.newRequest("http://localhost:" + lackrPort + "/femtor/dumpwrapper");
        r.getHeaders().add("X-Ftn-OperationId", "someid");
        ContentResponse e = r.send();
//    	System.err.println(e.getResponseContent());
    	assertEquals(200, e.getStatus());
    	StringTokenizer tokenizer = new StringTokenizer(e.getContentAsString(), "\n");
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
        Request r = client.newRequest("http://localhost:" + lackrPort + "/rewrite");
        ContentResponse e = r.send();
        assertEquals("Hi from dummy femtor\n", new String(e.getContent()));
    }
    
}
