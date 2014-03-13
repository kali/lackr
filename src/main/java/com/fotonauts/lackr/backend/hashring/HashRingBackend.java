package com.fotonauts.lackr.backend.hashring;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.Backend;
import com.fotonauts.lackr.LackrBackendRequest;
import com.fotonauts.lackr.backend.BaseRoutingBackend;

public class HashRingBackend extends BaseRoutingBackend implements Backend {

    static Logger log = LoggerFactory.getLogger(HashRingBackend.class);

    @SuppressWarnings("serial")
    public static class NotAvailableException extends Exception {
    };

    private int bucketPerHost = 128;
    AtomicInteger up = new AtomicInteger(0);
    private RingMember[] hosts;
    private int sleepMS = 100; 

    private NavigableMap<Integer, RingMember> ring;

    private AtomicBoolean mustStop = new AtomicBoolean(false);
    private Thread proberThread;

    public HashRingBackend(Backend... backends) {
        hosts = new RingMember[backends.length];
        for (int i = 0; i < backends.length; i++)
            hosts[i] = new RingMember(backends[i]);
    }

    public void doStart() throws Exception {
        ring = new TreeMap<Integer, RingMember>();
        for (RingMember h : hosts) {
            h.setRing(this);
            Random random = new Random(h.getBackend().getName().hashCode());
            for (int i = 0; i < bucketPerHost; i++) {
                ring.put(random.nextInt(), h);
            }
        }
        for (RingMember h : hosts)
            h.start();
        up.set(hosts.length);
        proberThread = new Thread() {
            public void run() {
                while (!mustStop.get()) {
                    try {
                        for (RingMember h : hosts)
                            h.probe();
                    } catch (Throwable e) {
                        log.warn("Caught exception in probing thread: ", e);
                    }
                    try {
                        Thread.sleep(getSleepMS());
                    } catch (InterruptedException e) {
                    }
                }
            };
        };
        proberThread.setName("ProberThread: " + getName());
        proberThread.setDaemon(true);
        proberThread.start();
    }

    public boolean up() {
        return up.intValue() > 0;
    }

    @Override
    public Backend chooseBackendFor(LackrBackendRequest request) throws NotAvailableException {
        return getBackendFor(request.getQuery()); 
    }

    public Backend getBackendFor(String url) throws NotAvailableException {
        RingMember member = getMemberFor(url);
        if (member == null)
            return null;
        else
            return member.getBackend();
    }

    public RingMember getMember(int i) {
        return hosts[i];
    }

    public RingMember getMemberFor(String value) throws NotAvailableException {
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
        SortedMap<Integer, RingMember> tail = ring.tailMap(bb.getInt());
        for (Entry<Integer, RingMember> entry : tail.entrySet()) {
            if (entry.getValue().isUp())
                return entry.getValue();
        }
        for (Entry<Integer, RingMember> entry : ring.entrySet()) {
            if (entry.getValue().isUp())
                return entry.getValue();
        }
        throw new NotAvailableException();
    }

    public void refreshStatus() {
        int ups = 0;
        for (RingMember host : hosts) {
            if (host.isUp())
                ups++;
        }
        up.set(ups);
        String message = "Ring has " + up.get() + " backend up among " + hosts.length + ".";
        log.warn(message);
    }

    @Override
    public String getName() {
        if (hosts.length == 0)
            return "Empty hashring.";
        else
            return "HashRingBackend: " + hosts[0].getBackend().getName() + " et al.";
    }

    @Override
    public void doStop() throws Exception {
        mustStop.set(true);
        proberThread.join();
        for (RingMember host : hosts)
            host.stop();
    }

    @Override
    public boolean probe() {
        return up.get() > 0;
    }

    public int getSleepMS() {
        return sleepMS;
    }

    public void setSleepMS(int sleep) {
        this.sleepMS = sleep;
    }

}
