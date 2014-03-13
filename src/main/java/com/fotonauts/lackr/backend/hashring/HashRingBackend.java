package com.fotonauts.lackr.backend.hashring;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.Backend;
import com.fotonauts.lackr.LackrBackendRequest;
import com.fotonauts.lackr.backend.BaseRoutingBackend;
import com.fotonauts.lackr.backend.Cluster;
import com.fotonauts.lackr.backend.ClusterMember;

public class HashRingBackend extends BaseRoutingBackend implements Backend {

    static Logger log = LoggerFactory.getLogger(HashRingBackend.class);

    @SuppressWarnings("serial")
    public static class NotAvailableException extends Exception {
    };

    private int bucketPerHost = 128;
    private Cluster cluster;

    private NavigableMap<Integer, ClusterMember> ring;

    public HashRingBackend(Backend... backends) {
        cluster = new Cluster(backends);
    }

    public void doStart() throws Exception {
        ring = new TreeMap<Integer, ClusterMember>();
        for (ClusterMember h : cluster.getMembers()) {
            Random random = new Random(h.getBackend().getName().hashCode());
            for (int i = 0; i < bucketPerHost; i++) {
                ring.put(random.nextInt(), h);
            }
        }
        cluster.start();
    }

    @Override
    protected void doStop() throws Exception {
        cluster.stop();
    }
    
    @Override
    public Backend chooseBackendFor(LackrBackendRequest request) throws NotAvailableException {
        return getBackendFor(request.getQuery()); 
    }

    public Backend getBackendFor(String url) throws NotAvailableException {
        ClusterMember member = getMemberFor(url);
        if (member == null)
            return null;
        else
            return member.getBackend();
    }

    public ClusterMember getMemberFor(String value) throws NotAvailableException {
        if (!cluster.oneUp())
            throw new NotAvailableException();
        MessageDigest m = null;
        try {
            m = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            // nope.
        }
        m.update(value.getBytes());
        ByteBuffer bb = ByteBuffer.wrap(m.digest());
        SortedMap<Integer, ClusterMember> tail = ring.tailMap(bb.getInt());
        for (Entry<Integer, ClusterMember> entry : tail.entrySet()) {
            if (entry.getValue().isUp())
                return entry.getValue();
        }
        for (Entry<Integer, ClusterMember> entry : ring.entrySet()) {
            if (entry.getValue().isUp())
                return entry.getValue();
        }
        throw new NotAvailableException();
    }

    @Override
    public String getName() {
        return cluster.getName();
    }

    @Override
    public boolean probe() {
        return cluster.oneUp();
    }

    public Cluster getCluster() {
        return cluster;
    }

}
