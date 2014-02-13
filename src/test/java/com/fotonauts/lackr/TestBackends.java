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
import com.fotonauts.lackr.testutils.ServletFilterDummyStub;
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
            InProcessBackend inprocess = new InProcessBackend(new ServletFilterDummyStub());
            inprocess.setTimeout(500);
            proxyServer = Factory.buildSimpleProxyServer(inprocess);
        } else {
            remote = new Server();
            ServerConnector connector = new ServerConnector(remote);
            remote.addConnector(connector);

            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            remote.setHandler(context);

            context.addFilter(com.fotonauts.lackr.testutils.ServletFilterDummyStub.class.getCanonicalName(), "/*",
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
    public void testFilter() throws Exception {
        Request r = client.createRequest("/sfds/hi");
        ContentResponse e = r.send();
        assertEquals("Hi from dummy filter\n", e.getContentAsString());
    }

    @Test(timeout = 500)
    public void testFilterCrashServlet() throws Exception {
        Request r = client.createRequest("/sfds/crash/servlet");
        ContentResponse e = r.send();
        assertEquals(50, e.getStatus() / 10); // expect 50x
    }

    @Test(timeout = 500)
    public void testFilterCrashRE() throws Exception {
        Request r = client.createRequest("/sfds/crash/re");
        ContentResponse e = r.send();
        assertEquals(50, e.getStatus() / 10); // expect 50x
    }

    @Test(timeout = 500)
    public void testFilterCrashError() throws Exception {
        Request r = client.createRequest("/sfds/crash/error");
        ContentResponse e = r.send();
        assertEquals(50, e.getStatus() / 10); // expect 50x
    }

    @Test
    public void testFilterQuery() throws Exception {
        Request r = client.createRequest("/sfds/dump?blah=12&blih=42");
        r.getHeaders().add("X-Ftn-OperationId", "someid");
        ContentResponse e = r.send();
        //    	System.err.println(e.getResponseContent());
        assertEquals(200, e.getStatus());
        StringTokenizer tokenizer = new StringTokenizer(e.getContentAsString(), "\n");
        assertEquals("Hi from dummy filter", tokenizer.nextToken());
        assertEquals("method: GET", tokenizer.nextToken());
        assertEquals("pathInfo: /sfds/dump", tokenizer.nextToken());
        assertEquals("getQueryString: blah=12&blih=42", tokenizer.nextToken());
        assertEquals("getRequestURI: /sfds/dump", tokenizer.nextToken());
        assertEquals("X-Ftn-OperationId: someid", tokenizer.nextToken());
        assertEquals("x-ftn-operationid: someid", tokenizer.nextToken());
        assertEquals("parameterNames: [blah, blih]", tokenizer.nextToken());
        assertFalse(tokenizer.hasMoreElements());
    }

    @Test
    public void testFilterBodyQuery() throws Exception {
        Request r = client.createRequest("/echobody");
        r.getHeaders().add("X-Ftn-OperationId", "someid");
        r.content(new StringContentProvider("yop yop yop", "UTF-8"));
        ContentResponse e = r.send();
        assertEquals(200, e.getStatus());
        assertEquals("yop yop yop", e.getContentAsString());
    }

    @Test(timeout = 1000)
    public void testTimeoutInProcess() throws Exception {
        if(!inProcess)
            return;
        Request r = client.createRequest("/wait");
        assertEquals(50, r.send().getStatus() / 10);
    }
    
    @Ignore
    // ESI + backends
    @Test(timeout = 500)
    public void testFilterESItoInvalidUrl() throws Exception {
        Request r = client.createRequest("/sfds/esiToInvalidUrl");
        ContentResponse e = r.send();
        assertTrue("invalid url from ESI should cleanly crash.", e.getStatus() >= 500);
    }

}
