package com.fotonauts.lackr;

import java.util.HashMap;
import java.util.Map.Entry;

import junit.framework.TestCase;

import org.eclipse.jetty.util.component.AbstractLifeCycle;

import com.fotonauts.lackr.backend.hashring.HashRingBackend;
import com.fotonauts.lackr.backend.hashring.HashRingBackend.NotAvailableException;
import com.fotonauts.lackr.backend.hashring.RingMember;

public class TestRing extends TestCase {

    static class Stub extends AbstractLifeCycle implements Backend {

        private String name;

        public Stub(String name) {
            this.name = name;
        }
        
        @Override
        public LackrBackendExchange createExchange(LackrBackendRequest request) throws NotAvailableException {
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
    
	public void testEmptyRing() throws Exception {
		HashRingBackend ring = new HashRingBackend(new Backend[0]);
		ring.start();
		try {
			ring.getHostFor("titi");
			assertTrue("should raise NAE here", false);
		} catch (NotAvailableException e) {
		}
		ring.stop();
	}

	public void testTrivialRing() throws Exception {
		HashRingBackend ring = buildHashRing("localhost");
		ring.start();
		assertEquals("localhost", ring.getHostFor("titi").getName());
        ring.stop();
	}

	public void testSingleBackendDown() throws Exception {
		HashRingBackend ring = buildHashRing("titi");
		ring.start();
		ring.getMemberFor("toto").setDown();
		try {
			ring.getHostFor("titi");
			assertTrue("should raise NAE here", false);
		} catch (NotAvailableException e) {
		}
        ring.stop();
	}

	public void testConsistency() throws Exception {
		HashRingBackend ring1 = buildHashRing("host1", "host2");
		ring1.start();
		HashRingBackend ring2 = buildHashRing("host2", "host1");
		ring2.start();
		for (int i = 0; i < 10; i++) {
			assertEquals("ring 1 and 2 should give same host", ring1.getHostFor("url" + i).getName(), ring2
			        .getHostFor("url" + i).getName());
		}
        ring1.stop();
        ring2.stop();
	}
	
	public void testSpread() throws Exception {
		HashRingBackend ring = buildHashRing("h1", "h2","h3", "h4", "h5", "h6", "h7", "h8");
		ring.start();
		HashMap<RingMember, Integer> result = new HashMap<RingMember, Integer>();
		for(int i = 0; i < 10000; i++) {
			String url = "blahblah" + i;
			RingMember h = ring.getMemberFor(url);
			if(!result.containsKey(h))
				result.put(h, 1);
			else
				result.put(h, result.get(h) + 1);
		}
		assertEquals("queried backends", 8, result.size());
		for(Entry<RingMember, Integer> entry : result.entrySet()) {
			assertTrue(entry.getKey().getBackend() + " got its share", entry.getValue() > 500);
		}
		ring.stop();
	}

	public void testAvoidDead() throws Exception {
		HashRingBackend ring = buildHashRing("h1", "h2","h3", "h4", "h5", "h6", "h7", "h8");
		ring.start();
		RingMember[] result1 = new RingMember[10000];
		for(int i = 0; i < 10000; i++) {
			String url = "blahblah" + i;
			RingMember h = ring.getMemberFor(url);
			result1[i] = h;
		}
		RingMember dead = ring.getMemberFor("dead");
		dead.setDown();
		RingMember[] result2 = new RingMember[10000];
		for(int i = 0; i < 10000; i++) {
			String url = "blahblah" + i;
			RingMember h = ring.getMemberFor(url);
			result2[i] = h;
		}
		for(int i = 0; i < 10000; i++) {
			if(result1[i] != dead)
				assertEquals(result2[i], result1[i]);
		}
        ring.stop();
 	}
}
