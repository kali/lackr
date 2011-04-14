package com.fotonauts.lackr;

import java.util.HashMap;
import java.util.Map.Entry;

import junit.framework.TestCase;

import com.fotonauts.lackr.hashring.HashRing;
import com.fotonauts.lackr.hashring.Host;
import com.fotonauts.lackr.hashring.HashRing.NotAvailableException;

public class TestRing extends TestCase {

	public void testEmptyRing() throws Exception {
		HashRing ring = new HashRing(new String[0]);
		ring.init();
		try {
			ring.getHostFor("titi");
			assertTrue("should raise NAE here", false);
		} catch (NotAvailableException e) {
		}
	}

	public void testTrivialRing() throws Exception {
		HashRing ring = new HashRing("localhost");
		ring.init();
		assertEquals("localhost", ring.getHostFor("titi").getHostname());
	}

	public void testSingleBackendDown() throws Exception {
		HashRing ring = new HashRing("titi");
		ring.init();
		ring.getHostFor("toto").setDown();
		try {
			ring.getHostFor("titi");
			assertTrue("should raise NAE here", false);
		} catch (NotAvailableException e) {
		}
	}

	public void testConsistency() throws Exception {
		HashRing ring1 = new HashRing("host1", "host2");
		ring1.init();
		HashRing ring2 = new HashRing("host2", "host1");
		ring2.init();
		for (int i = 0; i < 10; i++) {
			assertEquals("ring 1 and 2 should give same host", ring1.getHostFor("url" + i).getHostname(), ring2
			        .getHostFor("url" + i).getHostname());
		}
	}
	
	public void testSpread() throws Exception {
		HashRing ring = new HashRing("h1", "h2","h3", "h4", "h5", "h6", "h7", "h8");
		ring.init();
		HashMap<Host, Integer> result = new HashMap<Host, Integer>();
		for(int i = 0; i < 10000; i++) {
			String url = "blahblah" + i;
			Host h = ring.getHostFor(url);
			if(!result.containsKey(h))
				result.put(h, 1);
			else
				result.put(h, result.get(h) + 1);
		}
		assertEquals("queried backends", 8, result.size());
		for(Entry<Host, Integer> entry : result.entrySet()) {
			assertTrue(entry.getKey().getHostname() + " got its share", entry.getValue() > 500);
		}
	}

	public void testAvoidDead() throws Exception {
		HashRing ring = new HashRing("h1", "h2","h3", "h4", "h5", "h6", "h7", "h8");
		ring.init();
		Host[] result1 = new Host[10000];
		for(int i = 0; i < 10000; i++) {
			String url = "blahblah" + i;
			Host h = ring.getHostFor(url);
			result1[i] = h;
		}
		Host dead = ring.getHostFor("dead");
		dead.setDown();
		Host[] result2 = new Host[10000];
		for(int i = 0; i < 10000; i++) {
			String url = "blahblah" + i;
			Host h = ring.getHostFor(url);
			result2[i] = h;
		}
		for(int i = 0; i < 10000; i++) {
			if(result1[i] != dead)
				assertEquals(result2[i], result1[i]);
		}
 	}
}
