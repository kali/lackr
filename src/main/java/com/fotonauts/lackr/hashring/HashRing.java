package com.fotonauts.lackr.hashring;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.fotonauts.commons.RapportrInterface;
import com.fotonauts.lackr.BackendRequest;
import com.fotonauts.lackr.HttpDirectorInterface;
import com.fotonauts.lackr.HttpHost;
import com.fotonauts.lackr.Gateway;

public class HashRing implements HttpDirectorInterface {

	static Logger log = LoggerFactory.getLogger(RingHost.class);

	@SuppressWarnings("serial")
	public static class NotAvailableException extends Exception {
	};

	int bucketPerHost = 128;
	AtomicInteger up = new AtomicInteger(0);
	RingHost[] hosts;

	private NavigableMap<Integer, RingHost> ring;

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

	public void setHosts(RingHost[] hosts) {
		this.hosts = hosts;
	}

	public HashRing(String... backends) {
		hostnames = backends;
		init();
	}

	public HashRing(RingHost... backends) {
		hosts = backends;
		init();
	}

	@PostConstruct
	public void init() {
		if (hosts == null && hostnames != null) {
			hosts = new RingHost[hostnames.length];
			for (int i = 0; i < hostnames.length; i++) {
				hosts[i] = new RingHost(rapportrInterface, hostnames[i], probeUrl);
			}
		}
		ring = new TreeMap<Integer, RingHost>();
		for (RingHost h : hosts) {
			h.setRing(this);
			Random random = new Random(h.getHostname().hashCode());
			for (int i = 0; i < bucketPerHost; i++) {
				ring.put(random.nextInt(), h);
			}
		}
        for (RingHost h : hosts)
            h.start();
		up.set(hosts.length);
		Thread prober = new Thread() {
			public void run() {
				boolean running = true;
				while (running) {
					for (RingHost h : hosts) {
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

	public RingHost getHostFor(String value) throws NotAvailableException {
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
		SortedMap<Integer, RingHost> tail = ring.tailMap(bb.getInt());
		for (Entry<Integer, RingHost> entry : tail.entrySet()) {
			if (entry.getValue().isUp())
				return entry.getValue();
		}
		for (Entry<Integer, RingHost> entry : ring.entrySet()) {
			if (entry.getValue().isUp())
				return entry.getValue();
		}
		throw new NotAvailableException();
	}

	public void refreshStatus() {
		int ups = 0;
		for (RingHost host : hosts) {
			if (host.isUp())
				ups++;
		}
		up.set(ups);
		String message = "Ring has " + up.get() + " backend up among " + hosts.length + ".";
		log.warn(message);
		if (rapportrInterface != null)
			rapportrInterface.warnMessage(message, null);
	}

	public NavigableMap<Integer, RingHost> getRing() {
		return ring;

	}

	public Gateway[] getGateways() {
		return hosts;
	}

	public RingHost[] getHosts() {
        return hosts;
    }

	@Override
	public HttpHost getHostFor(BackendRequest request) throws NotAvailableException {
		return getHostFor(request.getQuery());
	}

	public RapportrInterface getRapportrInterface() {
		return rapportrInterface;
	}

	public void setRapportrInterface(RapportrInterface rapportrInterface) {
		this.rapportrInterface = rapportrInterface;
	}

	@Override
	public void dumpStatus(PrintStream ps) {
		Map<RingHost, Long> weights = new HashMap<RingHost, Long>();
		for (RingHost h : getHosts())
			weights.put(h, 0L);
		Entry<Integer, RingHost> previous = null;
		for (Entry<Integer, RingHost> e : getRing().entrySet()) {
			if (previous != null)
				weights.put(previous.getValue(), weights.get(previous.getValue()) + e.getKey() - previous.getKey());
			ps.format("ring-boundary\t%08x\t\n", e.getKey(), e.getValue().getHostname());
			previous = e;
		}
		weights.put(previous.getValue(), weights.get(previous.getValue()) + (getRing().firstKey() - Integer.MIN_VALUE));
		weights.put(previous.getValue(), weights.get(previous.getValue()) + (Integer.MAX_VALUE - getRing().lastKey()));
		for (RingHost h : getHosts()) {
			ps.format("picor-ring-weight\t%s\t%s\t%d\n", h.getHostname(), h.isUp() ? "UP" : "DOWN", weights.get(h));
		}
	}
	
    @Override
    public String getName() {
        return StringUtils.arrayToDelimitedString(hostnames, "_").replaceAll("[.:]","_");
    }


}
