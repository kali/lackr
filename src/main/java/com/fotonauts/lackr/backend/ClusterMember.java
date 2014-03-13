/**
 * 
 */
package com.fotonauts.lackr.backend;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.Backend;

public class ClusterMember extends AbstractLifeCycle {

    static Logger log = LoggerFactory.getLogger(ClusterMember.class);

    private Cluster cluster;
    protected Backend backend;
    private int id;

    public ClusterMember(Cluster cluster, Backend backend, int id) {
        this.cluster = cluster;
        this.backend = backend;
        this.id = id;
    }

    public Backend getBackend() {
        return backend;
    }

    public int getId() {
        return id;
    }

    public boolean isUp() {
        return up.get();
    }

    private AtomicBoolean up = new AtomicBoolean(true);

    @Override
    public String toString() {
        return getBackend().getName() + ' ' + (isUp() ? "UP" : "DOWN");
    }

    public void setDown() {
        up.set(false);
        if (cluster != null)
            cluster.refreshStatus();
    }

    public void setUp() {
        up.set(true);
        if (cluster != null)
            cluster.refreshStatus();
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public boolean probe() {
        boolean after = false;
        try {
            after = backend.probe();
        } catch (Throwable e) {
            String message = "Can not probe " + backend.getName();
            log.warn(message);
        }
        boolean before = up.getAndSet(after);
        if (before != after) {
            log.warn("Status change: " + toString());
            if (cluster != null)
                cluster.refreshStatus();
        }
        return after;
    }

    @Override
    protected void doStart() throws Exception {
        backend.start();
    }

    @Override
    protected void doStop() throws Exception {
        backend.stop();
    }

}