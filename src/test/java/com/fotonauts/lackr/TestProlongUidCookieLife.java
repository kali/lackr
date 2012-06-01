package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.http.HttpHeaders;
import org.junit.Test;

public class TestProlongUidCookieLife extends BaseTestLackrFullStack {

    public TestProlongUidCookieLife() throws Exception {
        super();
    }


    @Test()
    public void testFemtor() throws Exception {
        ContentExchange e = new ContentExchange(true);
        e.setRequestHeader(HttpHeaders.COOKIE, "uid=zob");
        e.setURL("http://localhost:" + lackrServer.getConnectors()[0].getLocalPort() + "/femtor/hi");
        client.send(e);
        while (!e.isDone())
            Thread.sleep(10);
        assertTrue(e.getResponseFields().containsKey("Set-Cookie"));
        System.err.println(e.getResponseFields().getStringField("Set-Cookie"));
        assertTrue(e.getResponseFields().getStringField("Set-Cookie").contains("2037"));
        assertEquals("Hi from dummy femtor\n", e.getResponseContent());
    }
    
}
