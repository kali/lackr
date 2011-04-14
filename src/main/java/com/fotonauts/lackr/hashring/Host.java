/**
 * 
 */
package com.fotonauts.lackr.hashring;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Host {

	static Logger log = LoggerFactory.getLogger(Host.class);

	protected String hostname;
	private HashRing ring;

	private String probeString;
	private URL probeURL;

	public Host() {
	}

	public Host(String backend) {
		this.hostname = backend;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public boolean isUp() {
		return up.get();
	}

	private AtomicBoolean up = new AtomicBoolean(true);

	@Override
	public String toString() {
		return getHostname() + ' ' + (isUp() ? "UP" : "DOWN");
	}

	public void setDown() {
		up.set(false);
		if(ring != null)
			ring.refreshStatus();
	}

	public void setUp() {
		up.set(true);
		if(ring != null)
			ring.refreshStatus();
	}

	public void setRing(HashRing ring) {
		this.ring = ring;
	}

	public HashRing getRing() {
		return ring;
	}

	public void setProbe(String probe) {
		this.probeString = probe;
	}

	public String getProbe() {
		return probeString;
	}

	@PostConstruct
	public void init() throws MalformedURLException {
		if(probeString != null)
			probeURL = new URL("http://" + hostname + probeString);
	}
	
	public void probe() {
		if(probeURL == null)
			return;
		boolean after = false;
        try {
			HttpURLConnection connection = (HttpURLConnection) probeURL.openConnection();
			connection.setConnectTimeout(100*1000);
			after = connection.getResponseCode() == 200;
        } catch (IOException e) {
        }
        boolean before = up.getAndSet(after);
        if(before != after) {
        	log.warn("Status change: " + toString());
        	if(ring != null)
        		ring.refreshStatus();
        }
    }

}