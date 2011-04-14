package com.fotonauts.lackr.hashring;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.NavigableMap;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HashRing {

	static Logger log = LoggerFactory.getLogger(Host.class);
	
	@SuppressWarnings("serial")
	public static class NotAvailableException extends Exception {
	};

	int bucketPerHost = 32;
	AtomicInteger up = new AtomicInteger(0);
	Host[] hosts;

	private NavigableMap<Integer, Host> ring;

	public HashRing() {
		// TODO Auto-generated constructor stub
	}

	public HashRing(String... backends) {
		hosts = new Host[backends.length];
		for (int i = 0; i < backends.length; i++)
			hosts[i] = new Host(backends[i]);
		init();
	}

	public HashRing(Host... backends) {
		hosts = backends;
		init();
    }

	public void setHosts(Host[] hosts) {
		this.hosts = hosts;
	}

	@PostConstruct
	public void init() {
		ring = new TreeMap<Integer, Host>();
		for (Host h : hosts) {
			h.setRing(this);
			Random random = new Random(h.getHostname().hashCode());
			for (int i = 0; i < bucketPerHost; i++) {
				ring.put(random.nextInt(), h);
			}
		}
		up.set(hosts.length);
		Thread prober = new Thread() {
			public void run() {
				boolean running = true;
				while(running) {
					for(Host h : hosts) {
						h.probe();
					}
					try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    	running = false;
                    }	
				}
			};
		};
		prober.start();
	}

	public boolean up() {
		return up.intValue() > 0;
	}

	public Host getHostFor(String value) throws NotAvailableException {
		if (!up())
			throw new NotAvailableException();
		MessageDigest m = null;
		try {
			m = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// nope.
		}
		m.update(value.getBytes());
		ByteBuffer bb = ByteBuffer.wrap(m.digest());
		SortedMap<Integer, Host> tail = ring.tailMap(bb.getInt());
		for (Entry<Integer, Host> entry : tail.entrySet()) {
			if (entry.getValue().isUp())
				return entry.getValue();
		}
		for (Entry<Integer, Host> entry : ring.entrySet()) {
			if (entry.getValue().isUp())
				return entry.getValue();
		}
		throw new NotAvailableException();
	}

	public void refreshStatus() {
		int ups = 0;
		for (Host host : hosts) {
			if (host.isUp())
				ups++;
		}
		up.set(ups);
		log.warn("Ring has " + up.get() + " backend up among " + hosts.length + ".");
	}
}
