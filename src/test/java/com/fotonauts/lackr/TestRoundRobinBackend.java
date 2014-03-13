package com.fotonauts.lackr;

import static org.junit.Assert.*;

import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.junit.Test;

import com.fotonauts.lackr.backend.ClusterMember;
import com.fotonauts.lackr.backend.RoundRobinBackend;


public class TestRoundRobinBackend {
    
    static class Stub extends AbstractLifeCycle implements Backend {

        private String name;
        private boolean up = true;

        public Stub(String name) {
            this.name = name;
        }
        
        @Override
        public LackrBackendExchange createExchange(LackrBackendRequest request) {
            throw new RuntimeException("Bouh!");
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean probe() {
            return up;
        }
        
    }
    
    public static RoundRobinBackend buildRoundRobin(String... names) {
        Backend[] backends = new Backend[names.length];
        for(int i = 0; i<names.length; i++)
            backends[i] = new Stub(names[i]);
        RoundRobinBackend ring = new RoundRobinBackend(backends);
        return ring;
    }

    @Test
    public void testSpread() throws Exception {
        RoundRobinBackend ring = buildRoundRobin("h1", "h2","h3", "h4", "h5", "h6", "h7", "h8");
        ring.start();
        for(int i = 0; i < 10000; i++) {
            ClusterMember h = ring.chooseMemberFor(null);
            assertEquals(i % 8, h.getId());
        }
        ring.stop();
    }

    @Test
    public void testDodge() throws Exception {
        RoundRobinBackend ring = buildRoundRobin("h1", "h2","h3", "h4", "h5", "h6", "h7", "h8");
        int[] counters = new int[8];
        ring.start();
        assertTrue(ring.getCluster().allUp());
        ((Stub) ring.getCluster().getMember(3).getBackend()).up = false;
        Thread.sleep(ring.getCluster().getSleepMS()*2);
        assertTrue(ring.probe());
        assertFalse(ring.getCluster().allUp());
        for(int i = 0; i < 70; i++) {
            ClusterMember h = ring.chooseMemberFor(null);
            counters[h.getId()] += 1;
            assertNotSame(h.getId(), 3);
        }
        assertArrayEquals(new int[] {10,10,10,0,10,10,10,10}, counters);
        ring.stop();
    }

}
