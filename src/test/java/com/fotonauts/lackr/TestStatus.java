package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;

import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.Test;

public class TestStatus extends BaseTestLackrFullStack {

	@Test
	public void testStatus() throws Exception {
        ContentResponse r = client.GET("http://localhost:" + lackrPort + "/_lackr_status");
        System.err.println(new String(r.getContent()));
        String[] lines = new String(r.getContent()).split("\n");
        assertEquals(4, lines.length);
    }


    public TestStatus() throws Exception {
        super();
    }
}
