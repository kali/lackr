package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;

import org.eclipse.jetty.client.ContentExchange;
import org.junit.Test;

public class TestStatus extends BaseTestLackrFullStack {

	public TestStatus() throws Exception {
	    super();
    }


	@Test
	public void testStatus() throws Exception {
        ContentExchange e = new ContentExchange(true);
        e.setURL("http://localhost:" + lackrServer.getConnectors()[0].getLocalPort() + "/_lackr_status");
        client.send(e);
        while (!e.isDone())
            Thread.sleep(10);
        String[] lines = e.getResponseContent().split("\n");
        assertEquals(4, lines.length);
    }
}
