package com.fotonauts.lackr.backend.hashring;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.Backend;
import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.LackrBackendRequest;
import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.backend.Cluster;
import com.fotonauts.lackr.backend.ClusterMember;

public class HashRingBackend extends AbstractLifeCycle implements Backend {

    static Logger log = LoggerFactory.getLogger(HashRingBackend.class);

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
    public LackrBackendExchange createExchange(LackrBackendRequest request) {
        return getBackendFor(request.getQuery()).createExchange(request);
    }

    public Backend getBackendFor(String url) {
        ClusterMember member = getMemberFor(url);
        if (member == null)
            return null;
        else
            return member.getBackend();
    }

    public ClusterMember getMemberFor(String value) {
        if (!cluster.oneUp())
            throw new LackrPresentableError("HashRing " + getName() + " has no working backends.");
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
        throw new LackrPresentableError("HashRing " + getName() + " has no working backends.");
    }

    @Override
    public String getName() {
        return "HashRing: " + cluster.getName();
    }

    @Override
    public boolean probe() {
        return cluster.oneUp();
    }

    public Cluster getCluster() {
        return cluster;
    }

}
