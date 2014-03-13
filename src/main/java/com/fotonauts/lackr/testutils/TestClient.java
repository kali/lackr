package com.fotonauts.lackr.testutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.component.AbstractLifeCycle;

public class TestClient extends AbstractLifeCycle {

    HttpClient client;
    int port;

    public TestClient(Server server) {
        this.port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        this.client = Factory.buildFullClient();
    }

    @Override
    protected void doStart() throws Exception {
        client.start();
    }

    @Override
    protected void doStop() throws Exception {
        client.stop();
    }

    public Request createRequest(String path) {
        return client.newRequest("http://localhost:" + port + path);
    }

    public ContentResponse runRequest(Request e, String expect) throws InterruptedException, TimeoutException, ExecutionException {
        ContentResponse response = e.timeout(600, TimeUnit.SECONDS).send();
        //System.err.println(response.getContentAsString());
        assertEquals(200, response.getStatus());
        assertEquals(expect, response.getContentAsString());
        return response;
    }

    public HttpClient getClient() {
        return client;
    }

    public void loadPageAndExpects(String expects) throws InterruptedException, TimeoutException, ExecutionException {
        Request exchange = createRequest("/page.html");
        runRequest(exchange, expects);
    }

    public void loadPageAndExpectsCrash() throws InterruptedException, TimeoutException, ExecutionException {
        Request req = createRequest("/page.html");
        ContentResponse resp = req.send();
        assertTrue("returns an error", resp.getStatus() >= 400);
    }

    public void loadPageAndExpectsContains(String expect) throws InterruptedException, TimeoutException, ExecutionException {
        loadPageAndExpectsContains("/page.html", expect);
    }

    public void loadPageAndExpectsContains(String target, String expect) throws InterruptedException, TimeoutException,
            ExecutionException {
        ContentResponse response = createRequest(target).timeout(600, TimeUnit.SECONDS).send();
        //System.err.println(response.getContentAsString());
        assertEquals(200, response.getStatus());
        assertTrue("response contains `" + expect + "'", response.getContentAsString().contains(expect));
    }

}
