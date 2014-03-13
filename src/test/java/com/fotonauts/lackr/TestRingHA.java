package com.fotonauts.lackr;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.After;

import com.fotonauts.lackr.backend.ClusterMember;
import com.fotonauts.lackr.backend.hashring.HashRingBackend;
import com.fotonauts.lackr.testutils.Factory;

public class TestRingHA extends TestCase {

    class StubServer extends Server {
        public AtomicInteger requestCount = new AtomicInteger(0);
        public AtomicBoolean up = new AtomicBoolean(true);

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            requestCount.incrementAndGet();
            if (up.get()) {
                response.setStatus(200);
                response.getWriter().write("okie");
                response.flushBuffer();
            } else {
                response.setStatus(500);
                response.flushBuffer();
            }
        }

        public StubServer() throws Exception {
            ServerConnector sc = new ServerConnector(this);
            addConnector(sc);
            start();
        }

        public int getPort() {
            return ((ServerConnector) getConnectors()[0]).getLocalPort();
        }

        @Override
        protected void doStop() throws Exception {
            super.doStop();
        }
    }

    @Override
    protected void setUp() throws Exception {
        System.setProperty("logback.configurationFile", "logback.debug.xml");
    }

    public void testHostProbeNoConnection() throws Exception {
        ClusterMember h = new ClusterMember(null, Factory.buildFullClientBackend(54321, "/"), 0);
        h.start();
        h.probe();
        assertFalse("h is down", h.isUp());
        h.stop();
    }

    public void testHostProbeWrongHostname() throws Exception {
        ClusterMember h = new ClusterMember(null, Factory.buildFullClientBackend("something.that.does.not.exists:1212", "/"), 0);
        h.probe();
        assertFalse("h is down", h.isUp());
    }

    public void testHostProbe500() throws Exception {
        StubServer backend = new StubServer();
        backend.up.set(false);
        ClusterMember h = new ClusterMember(null, Factory.buildFullClientBackend(backend.getPort(), "/"), 0);
        h.start();
        assertFalse(h.probe());
        assertEquals("server has been probed", 1, backend.requestCount.get());
        assertFalse("h is down", h.isUp());
        h.stop();
        backend.stop();
    }

    public void testHostProbe200() throws Exception {
        StubServer backend = new StubServer();
        ClusterMember h = new ClusterMember(null, Factory.buildFullClientBackend(backend.getPort(), "/"), 0);
        h.start();
        assertTrue(h.probe());
        assertEquals("server has been probed", 1, backend.requestCount.get());
        assertTrue("h is up", h.isUp());
        h.stop();
        backend.stop();
    }

    public void testRingStatusDiscovery() throws Exception {
        StubServer server1 = new StubServer();
        StubServer server2 = new StubServer();
        HashRingBackend ring = new HashRingBackend(Factory.buildFullClientBackend(server1.getPort(), "/"), Factory.buildFullClientBackend(server2
                .getPort(), "/"));
        ring.start();
        Thread.sleep(500);
        assertTrue("server has been probed", server1.requestCount.get() > 0);
        assertTrue("server has been probed", server2.requestCount.get() > 0);
        assertTrue("ring is up", ring.probe());
        server1.up.set(false);
        Thread.sleep(1500);
        assertTrue("ring is still up", ring.probe());
        assertTrue("backend1 is down", !ring.getCluster().getMember(0).isUp());
        assertTrue("backend2 is up", ring.getCluster().getMember(1).isUp());
        server2.up.set(false);
        Thread.sleep(1500);
        assertTrue("ring is now down", !ring.probe());
        assertTrue("backend1 is down", !ring.getCluster().getMember(0).isUp());
        assertTrue("backend2 is down", !ring.getCluster().getMember(1).isUp());
        server1.up.set(true);
        Thread.sleep(1500);
        assertTrue("ring is back up", ring.probe());
        assertTrue("backend1 is up", ring.getCluster().getMember(0).isUp());
        assertTrue("backend2 is down", !ring.getCluster().getMember(1).isUp());
        server1.stop();
        server2.stop();
        ring.stop();
    }

    @After
    public void tearDown() throws Exception {
        assertTrue("Thread leak!", Thread.getAllStackTraces().size() < 10);
        /*
        if (Thread.getAllStackTraces().size() > 5) {
            throw new RuntimeException("thread leak detected !");
        }
        */
    }
}