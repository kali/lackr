/**
 * 
 */
package com.fotonauts.lackr.backend.hashring;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.Backend;

public class RingMember extends AbstractLifeCycle {

    static Logger log = LoggerFactory.getLogger(RingMember.class);

    protected Backend backend;
    private HashRingBackend ring;

    public RingMember(Backend backend) {
        this.backend = backend;
    }

    public Backend getBackend() {
        return backend;
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
        if (ring != null)
            ring.refreshStatus();
    }

    public void setUp() {
        up.set(true);
        if (ring != null)
            ring.refreshStatus();
    }

    public void setRing(HashRingBackend ring) {
        this.ring = ring;
    }

    public HashRingBackend getRing() {
        return ring;
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
            if (ring != null)
                ring.refreshStatus();
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