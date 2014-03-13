package com.fotonauts.lackr;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fotonauts.lackr.backend.Cluster;
import com.fotonauts.lackr.backend.ClusterMember;
import com.fotonauts.lackr.testutils.Factory;
import com.fotonauts.lackr.testutils.RemoteControlledStub;

public class TestCluster {

    @Before
    public void setUp() throws Exception {
        System.setProperty("logback.configurationFile", "logback.debug.xml");
    }

    @Test
    public void testHostProbeNoConnection() throws Exception {
        ClusterMember h = new ClusterMember(null, Factory.buildFullClientBackend(54321, "/"), 0);
        h.start();
        h.probe();
        assertFalse("h is down", h.isUp());
        h.stop();
    }

    @Test
    public void testHostProbeWrongHostname() throws Exception {
        ClusterMember h = new ClusterMember(null, Factory.buildFullClientBackend("something.that.does.not.exists:1212", "/"), 0);
        h.probe();
        assertFalse("h is down", h.isUp());
    }

    @Test
    public void testHostProbe500() throws Exception {
        RemoteControlledStub backend = new RemoteControlledStub();
        backend.start();
        backend.up.set(false);
        ClusterMember h = new ClusterMember(null, Factory.buildFullClientBackend(backend.getPort(), "/"), 0);
        h.start();
        assertFalse(h.probe());
        assertEquals("server has been probed", 1, backend.requestCount.get());
        assertFalse("h is down", h.isUp());
        h.stop();
        backend.stop();
    }

    @Test
    public void testHostProbe200() throws Exception {
        RemoteControlledStub backend = new RemoteControlledStub();
        backend.start();
        ClusterMember h = new ClusterMember(null, Factory.buildFullClientBackend(backend.getPort(), "/"), 0);
        h.start();
        assertTrue(h.probe());
        assertEquals("server has been probed", 1, backend.requestCount.get());
        assertTrue("h is up", h.isUp());
        h.stop();
        backend.stop();
    }

    @Test
    public void testRingStatusDiscovery() throws Exception {
        RemoteControlledStub server1 = new RemoteControlledStub();
        server1.start();
        RemoteControlledStub server2 = new RemoteControlledStub();
        server2.start();
        Cluster cluster = new Cluster(Factory.buildFullClientBackend(server1.getPort(), "/"), Factory.buildFullClientBackend(server2
                .getPort(), "/"));
        cluster.start();
        Thread.sleep(500);
        assertTrue("server has been probed", server1.requestCount.get() > 0);
        assertTrue("server has been probed", server2.requestCount.get() > 0);
        assertTrue("cluster is all up", cluster.allUp());
        server1.up.set(false);
        Thread.sleep(1500);
        assertTrue("cluster is still one up", cluster.oneUp());
        assertTrue("backend1 is down", !cluster.getMember(0).isUp());
        assertTrue("backend2 is up", cluster.getMember(1).isUp());
        server2.up.set(false);
        Thread.sleep(1500);
        assertTrue("cluster is now down", !cluster.oneUp());
        assertTrue("backend1 is down", !cluster.getMember(0).isUp());
        assertTrue("backend2 is down", !cluster.getMember(1).isUp());
        server1.up.set(true);
        Thread.sleep(1500);
        assertTrue("cluster is back one up", cluster.oneUp());
        assertTrue("backend1 is up", cluster.getMember(0).isUp());
        assertTrue("backend2 is down", !cluster.getMember(1).isUp());
        server1.stop();
        server2.stop();
        cluster.stop();
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