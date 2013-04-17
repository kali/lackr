package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;
import org.junit.Test;

public class TestProlongUidCookieLife extends BaseTestLackrFullStack {

    public TestProlongUidCookieLife() throws Exception {
        super();
    }


    @Test()
    public void testProlong() throws Exception {
        Request e = client.newRequest("http://localhost:" + lackrPort + "/femtor/hi");
        e.header(HttpHeader.COOKIE.asString(), "uid=zob");
        ContentResponse r = e.send();
        System.err.println(r.getHeaders().getStringField("Set-Cookie"));
        assertTrue(r.getHeaders().containsKey("Set-Cookie"));
        assertTrue(r.getHeaders().getStringField("Set-Cookie").contains("2037"));
        assertEquals("Hi from dummy femtor\n", r.getContentAsString());
    }
    
}
