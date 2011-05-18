/**
 * 
 */
package com.fotonauts.lackr.hashring;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public class Host {

	static Logger log = LoggerFactory.getLogger(Host.class);

	protected String hostname;
	private HashRing ring;

	private String probeString;
	private AtomicReference<URL> probeURL = new AtomicReference<URL>();

	public Host() {
	}

	public Host(String backend) {
		this.hostname = backend;
	}

	public Host(String hostname, String probeUrl) {
		this.hostname = hostname;
		this.probeString = probeUrl;
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
	
	public void probe() {
		if(!StringUtils.hasText(probeString))
			return;
		boolean after = false;
        try {
    		if(probeURL.get() == null)
    			buildURL();
			HttpURLConnection connection = (HttpURLConnection) probeURL.get().openConnection();
			connection.setConnectTimeout(100*1000);
			after = connection.getResponseCode() == 200;
        } catch (IOException e) {
        	probeURL.set(null);
        }
        boolean before = up.getAndSet(after);
        if(before != after) {
        	log.warn("Status change: " + toString());
        	if(ring != null)
        		ring.refreshStatus();
        }
    }

	private void buildURL() throws MalformedURLException {
		probeURL.set(new URL("http://" + hostname + probeString));
    }

}