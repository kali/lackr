package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
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
        System.err.println(e.getResponseContent());	}

}
