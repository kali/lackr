package com.fotonauts.lackr.hashring;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.commons.RapportrInterface;
import com.fotonauts.lackr.BackendRequest;
import com.fotonauts.lackr.HttpDirectorInterface;

public class HashRing implements HttpDirectorInterface {

	static Logger log = LoggerFactory.getLogger(Host.class);

	@SuppressWarnings("serial")
	public static class NotAvailableException extends Exception {
	};

	int bucketPerHost = 128;
	AtomicInteger up = new AtomicInteger(0);
	Host[] hosts;

	private NavigableMap<Integer, Host> ring;

	private RapportrInterface rapportrInterface;
	private String[] hostnames;
	private String probeUrl;

	public String getProbeUrl() {
		return probeUrl;
	}

	public void setProbeUrl(String probeUrl) {
		this.probeUrl = probeUrl;
	}

	/*
	 * 
	 * public HashRing(String backendString, RapportrInterface rapportr, String
	 * probeUrl) { String[] backends = backendString.split(","); Host[] hosts =
	 * new Host[backends.length]; for (int i = 0; i < backends.length; i++)
	 * hosts[i] = new Host(rapportr, backends[i], probeUrl); }
	 * 
	 * public HashRing(String... backends) { hosts = new Host[backends.length];
	 * for (int i = 0; i < backends.length; i++) hosts[i] = new
	 * Host(backends[i]); init(); }
	 */
	public HashRing() {
	}

	public void setHostnames(String hostnames) {
		this.hostnames = hostnames.split(",");
	}

	public void setHosts(Host[] hosts) {
		this.hosts = hosts;
	}

	public HashRing(String... backends) {
		hostnames = backends;
		init();
	}

	public HashRing(Host... backends) {
		hosts = backends;
		init();
	}

	@PostConstruct
	public void init() {
		if (hosts == null && hostnames != null) {
			hosts = new Host[hostnames.length];
			for (int i = 0; i < hostnames.length; i++)
				hosts[i] = new Host(rapportrInterface, hostnames[i], probeUrl);
		}
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
				while (running) {
					for (Host h : hosts) {
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
		String message = "Ring has " + up.get() + " backend up among " + hosts.length + ".";
		log.warn(message);
		if (rapportrInterface != null)
			rapportrInterface.warnMessage(message, null);
	}

	public NavigableMap<Integer, Host> getRing() {
		return ring;

	}

	public Host[] getHosts() {
		return hosts;
	}

	@Override
	public String getHostnameFor(BackendRequest request) throws NotAvailableException {
		return getHostFor(request.getQuery()).getHostname();
	}

	public RapportrInterface getRapportrInterface() {
		return rapportrInterface;
	}

	public void setRapportrInterface(RapportrInterface rapportrInterface) {
		this.rapportrInterface = rapportrInterface;
	}

}
