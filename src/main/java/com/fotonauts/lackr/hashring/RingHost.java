/**
 * 
 */
package com.fotonauts.lackr.hashring;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jetty.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.commons.RapportrInterface;
import com.fotonauts.lackr.HttpHost;

public class RingHost extends HttpHost {

    static Logger log = LoggerFactory.getLogger(RingHost.class);

    protected String hostname;
    private HashRing ring;
    protected RapportrInterface rapportr;

    private String probeString;
    private AtomicReference<URL> probeURL = new AtomicReference<URL>();

    public static String getMBeanNameFromUrlPrefix(String prefix) {
        String baseName;
        if (StringUtil.isNotBlank(prefix))
            if (prefix.startsWith("http://") || prefix.startsWith("https://"))
                baseName = prefix;
            else
                baseName = "http://" + prefix;
        else
            baseName = "http://dummy.host.name:80/";
        URI uri = URI.create(baseName);
        return uri.getHost().split("\\.")[0] + "-" + uri.getPort();
    }

    public RingHost() {
    }

    public RingHost(String backend) {
        this.hostname = backend;
    }

    public RingHost(RapportrInterface rapportr, String hostname, String probeUrl) {
        this.rapportr = rapportr;
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
        if (ring != null)
            ring.refreshStatus();
    }

    public void setUp() {
        up.set(true);
        if (ring != null)
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
        if (StringUtil.isBlank(probeString))
            return;
        boolean after = false;
        try {
            if (probeURL.get() == null)
                buildURL();
            HttpURLConnection connection = (HttpURLConnection) probeURL.get().openConnection();
            connection.setConnectTimeout(100 * 1000);
            after = connection.getResponseCode() == 200;
        } catch (IOException e) {
            String message = "Can not probe " + probeURL.toString() + "(" + e.toString() + ")";
            log.warn(message);
            probeURL.set(null);
        }
        boolean before = up.getAndSet(after);
        if (before != after) {
            log.warn("Status change: " + toString());
            if (rapportr != null)
                rapportr.warnMessage("Backend status change: " + toString(), null);
            if (ring != null)
                ring.refreshStatus();
        }
    }

    private void buildURL() throws MalformedURLException {
        probeURL.set(new URL((hostname.startsWith("http://") ? hostname : ("http://" + hostname)) + probeString));
    }

    @Override
    public String getMBeanName() {
        return getMBeanNameFromUrlPrefix(hostname);
    }
    
}