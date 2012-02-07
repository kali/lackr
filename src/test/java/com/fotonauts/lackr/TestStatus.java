package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.jetty.client.ContentExchange;
import org.junit.Test;

public class TestStatus extends BaseTestLackrFullStack {

	public TestStatus(String clientImplementation) throws Exception {
	    super(clientImplementation);
    }


	@Test
	public void testStatus() throws Exception {
        ContentExchange e = new ContentExchange(true);
        e.setURL("http://localhost:" + lackrServer.getConnectors()[0].getLocalPort() + "/_lackr_status");
        client.send(e);
        while (!e.isDone())
            Thread.sleep(10);
        String[] lines = e.getResponseContent().split("\n");
        assertEquals(129, lines.length);
        assertTrue("last line format", lines[lines.length - 1].matches("picor-ring-weight\thttp://localhost:[0-9]+\tUP\t[0-9]+"));
    }
}
