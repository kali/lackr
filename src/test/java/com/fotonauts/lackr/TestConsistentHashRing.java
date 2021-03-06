package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map.Entry;

import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.junit.Test;

import com.fotonauts.lackr.backend.ClusterMember;
import com.fotonauts.lackr.backend.hashring.HashRingBackend;

public class TestConsistentHashRing {

    static class Stub extends AbstractLifeCycle implements Backend {

        private String name;

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
            return true;
        }
        
    }
    
    public static HashRingBackend buildHashRing(String... names) {
        Backend[] backends = new Backend[names.length];
        for(int i = 0; i<names.length; i++)
            backends[i] = new Stub(names[i]);
        HashRingBackend ring = new HashRingBackend(backends);
        return ring;
    }
    
    @Test
	public void testEmptyRing() throws Exception {
		HashRingBackend ring = new HashRingBackend(new Backend[0]);
		ring.start();
		try {
			ring.getBackendFor("titi");
            assertTrue("should raise before getting here", false);
		} catch (LackrPresentableError e) {
		}
		ring.stop();
	}

    @Test
	public void testTrivialRing() throws Exception {
		HashRingBackend ring = buildHashRing("localhost");
		ring.start();
		assertEquals("localhost", ring.getBackendFor("titi").getName());
        ring.stop();
	}

    @Test
	public void testSingleBackendDown() throws Exception {
		HashRingBackend ring = buildHashRing("titi");
		ring.start();
		ring.getMemberFor("toto").setDown();
		try {
			ring.getBackendFor("titi");
			assertTrue("should raise before getting here", false);
        } catch (LackrPresentableError e) {
		}
        ring.stop();
	}

    @Test
	public void testConsistency() throws Exception {
		HashRingBackend ring1 = buildHashRing("host1", "host2");
		ring1.start();
		HashRingBackend ring2 = buildHashRing("host2", "host1");
		ring2.start();
		for (int i = 0; i < 10; i++) {
			assertEquals("ring 1 and 2 should give same host", ring1.getBackendFor("url" + i).getName(), ring2
			        .getBackendFor("url" + i).getName());
		}
        ring1.stop();
        ring2.stop();
	}
	
    @Test
	public void testSpread() throws Exception {
		HashRingBackend ring = buildHashRing("h1", "h2","h3", "h4", "h5", "h6", "h7", "h8");
		ring.start();
		HashMap<ClusterMember, Integer> result = new HashMap<ClusterMember, Integer>();
		for(int i = 0; i < 10000; i++) {
			String url = "blahblah" + i;
			ClusterMember h = ring.getMemberFor(url);
			if(!result.containsKey(h))
				result.put(h, 1);
			else
				result.put(h, result.get(h) + 1);
		}
		assertEquals("queried backends", 8, result.size());
		for(Entry<ClusterMember, Integer> entry : result.entrySet()) {
			assertTrue(entry.getKey().getBackend() + " got its share", entry.getValue() > 500);
		}
		ring.stop();
	}

    @Test
	public void testAvoidDead() throws Exception {
		HashRingBackend ring = buildHashRing("h1", "h2","h3", "h4", "h5", "h6", "h7", "h8");
		ring.start();
		ClusterMember[] result1 = new ClusterMember[10000];
		for(int i = 0; i < 10000; i++) {
			String url = "blahblah" + i;
			ClusterMember h = ring.getMemberFor(url);
			result1[i] = h;
		}
		ClusterMember dead = ring.getMemberFor("dead");
		dead.setDown();
		ClusterMember[] result2 = new ClusterMember[10000];
		for(int i = 0; i < 10000; i++) {
			String url = "blahblah" + i;
			ClusterMember h = ring.getMemberFor(url);
			result2[i] = h;
		}
		for(int i = 0; i < 10000; i++) {
			if(result1[i] != dead)
				assertEquals(result2[i], result1[i]);
		}
        ring.stop();
 	}
}
