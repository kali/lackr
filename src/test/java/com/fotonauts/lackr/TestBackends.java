package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.StringTokenizer;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.NoJspServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.fotonauts.lackr.backend.inprocess.InProcessBackend;
import com.fotonauts.lackr.testutils.DummyInProcessStub;
import com.fotonauts.lackr.testutils.Factory;
import com.fotonauts.lackr.testutils.TestClient;

@RunWith(value = Parameterized.class)
public class TestBackends {

    Server remote;
    Server proxyServer;
    TestClient client;

    private boolean inProcess;

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { { false }, { true } });
    }

    public TestBackends(boolean inProcess) throws Exception {
        this.inProcess = inProcess;
    }

    @Before
    public void setup() throws Exception {
        if (inProcess) {
            InProcessBackend inprocess = new InProcessBackend(new DummyInProcessStub());
            proxyServer = Factory.buildSimpleProxyServer(inprocess);
        } else {
            remote = new Server();
            ServerConnector connector = new ServerConnector(remote);
            remote.addConnector(connector);

            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            remote.setHandler(context);

            context.addFilter(com.fotonauts.lackr.testutils.DummyInProcessStub.class.getCanonicalName(), "/*",
                    EnumSet.of(DispatcherType.REQUEST));
            context.addServlet(new ServletHolder(new NoJspServlet()), "/*");

            remote.start();

            proxyServer = Factory.buildSimpleProxyServer(connector.getLocalPort());
        }

        proxyServer.start();
        client = new TestClient(proxyServer);

        client.start();
    }

    @After
    public void tearDown() throws Exception {
        client.stop();
        proxyServer.stop();
        if (remote != null)
            remote.stop();
        assertTrue(Thread.getAllStackTraces().size() < 10);
    }

    @Test(timeout = 500)
    public void testFemtor() throws Exception {
        Request r = client.createExchange("/femtor/hi");
        ContentResponse e = r.send();
        assertEquals("Hi from dummy femtor\n", e.getContentAsString());
    }

    @Test(timeout = 500)
    public void testFemtorCrashServlet() throws Exception {
        Request r = client.createExchange("/femtor/crash/servlet");
        ContentResponse e = r.send();
        assertEquals(50, e.getStatus() / 10); // expect 50x
        assertTrue(e.getContentAsString().contains("catch me or you're dead."));
    }

    @Test(timeout = 500)
    public void testFemtorCrashRE() throws Exception {
        Request r = client.createExchange("/femtor/crash/re");
        ContentResponse e = r.send();
        assertEquals(50, e.getStatus() / 10); // expect 50x
        assertTrue(e.getContentAsString().contains("catch me or you're dead."));
    }

    @Test(timeout = 500)
    public void testFemtorCrashError() throws Exception {
        Request r = client.createExchange("/femtor/crash/error");
        ContentResponse e = r.send();
        assertEquals(50, e.getStatus() / 10); // expect 50x
        assertTrue(e.getContentAsString().contains("catch me or you're dead."));
    }

    @Test
    public void testFemtorQuery() throws Exception {
        Request r = client.createExchange("/femtor/dump?blah=12&blih=42");
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
        assertFalse(tokenizer.hasMoreElements());
    }

    @Test
    public void testFemtorBodyQuery() throws Exception {
        Request r = client.createExchange("/echobody");
        r.getHeaders().add("X-Ftn-OperationId", "someid");
        r.content(new StringContentProvider("yop yop yop", "UTF-8"));
        ContentResponse e = r.send();
        assertEquals(200, e.getStatus());
        assertEquals("yop yop yop", e.getContentAsString());
    }

    @Ignore
    // ESI + backends
    @Test(timeout = 500)
    public void testFemtorESItoInvalidUrl() throws Exception {
        Request r = client.createExchange("/femtor/esiToInvalidUrl");
        ContentResponse e = r.send();
        assertTrue("invalid url from ESI should cleanly crash.", e.getStatus() >= 500);
    }

    // FIXME
    @Test(timeout = 500)
    @Ignore
    // FIXME specific
    public void testFemtorRewrite() throws Exception {
        Request r = client.createExchange("/rewrite");
        ContentResponse e = r.send();
        assertEquals("Hi from dummy femtor\n", new String(e.getContent()));
    }

    // FIXME
    @Test(timeout = 500)
    @Ignore
    // FIXME specific
    public void testProxy() throws Exception {
        Request r = client.createExchange("/femtor/asyncProxy?lackrPort=" + 1212 /* was lackrPort */);
        ContentResponse e = r.send();
        assertEquals("Hi from dummy femtor\n", e.getContentAsString());
    }

}
