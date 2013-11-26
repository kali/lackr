package com.fotonauts.lackr;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fotonauts.lackr.backend.trypass.TryPassBackend;
import com.fotonauts.lackr.backend.trypass.TryPassBackendExchange;
import com.fotonauts.lackr.testutils.Factory;
import com.fotonauts.lackr.testutils.RemoteControlledStub;
import com.fotonauts.lackr.testutils.TestClient;

public class TestTryPass {

    RemoteControlledStub server1;
    RemoteControlledStub server2;
    private TestClient client;
    private Server server;

    private static Handler createHandler(final String identity) {
        return new AbstractHandler() {

            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                    throws IOException, ServletException {
                if (target.equals("/" + identity)) {
                    response.setStatus(200);
                    response.getWriter().println("Hey! I'm " + identity);
                    response.flushBuffer();
                } else if (identity.equals("server2") && target.equals("/send/399/to/server1")) {
                    response.setStatus(399);
                    response.setHeader("Location", "/server1");
                    response.flushBuffer();
                } else {
                    response.setStatus(501);
                    response.flushBuffer();
                }
            }

        };
    }

    @Test
    public void testSimple() throws Exception {
        TryPassBackend trypass = new TryPassBackend(Factory.buildFullClientBackend(server1.getPort()),
                Factory.buildFullClientBackend(server2.getPort()));
        server = Factory.buildSimpleProxyServer(trypass);
        server.start();
        client = new TestClient(server);
        client.start();
        client.loadPageAndExpectsContains("/server1", "Hey! I'm server1");
        client.loadPageAndExpectsContains("/server2", "Hey! I'm server2");
    }

    @Test
    public void testRestart() throws Exception {
        TryPassBackend trypass = new TryPassBackend(Factory.buildFullClientBackend(server1.getPort()),
                Factory.buildFullClientBackend(server2.getPort())) {

            @Override
            protected LackrBackendRequest alterBackendRequestAndRestart(TryPassBackendExchange exchange,
                    LackrBackendExchange subExchange) {
                if (subExchange.getResponse().getStatus() == 399
                        && subExchange.getResponse().getHeader(HttpHeader.LOCATION.asString()) != null) {
                    LackrBackendRequest req = exchange.getEffectiveBackendRequest();
                    String location = subExchange.getResponse().getHeader(HttpHeader.LOCATION.asString());
                    return new LackrBackendRequest(req.getFrontendRequest(), req.getMethod(), location, req.getParentQuery(),
                            req.getParentId(), req.getSyntax(), req.getBody(), req.getFields(), req.getCompletionListener());
                } else
                    return null;
            }

        };
        server = Factory.buildSimpleProxyServer(trypass);
        server.start();
        client = new TestClient(server);
        client.start();
        client.loadPageAndExpectsContains("/send/399/to/server1", "Hey! I'm server1");
    }

    @Before
    public void setup() throws Exception {
        server1 = new RemoteControlledStub();
        server1.start();
        server2 = new RemoteControlledStub();
        server2.start();

        server1.getCurrentHandler().set(createHandler("server1"));
        server2.getCurrentHandler().set(createHandler("server2"));
    }

    @After
    public void teardown() throws Exception {
        server1.stop();
        server2.stop();
        if (client != null)
            client.stop();
        if (server != null)
            server.stop();

        assertTrue("Thread leak!", Thread.getAllStackTraces().size() < 10);
    }

}
