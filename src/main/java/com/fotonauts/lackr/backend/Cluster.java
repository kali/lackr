package com.fotonauts.lackr.backend;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.Backend;

public class Cluster extends AbstractLifeCycle {

    static Logger log = LoggerFactory.getLogger(Cluster.class);

    protected AtomicInteger up = new AtomicInteger(0);

    private AtomicBoolean mustStop = new AtomicBoolean(false);
    private Thread proberThread;

    public ClusterMember[] members;
    private int sleepMS = 100; 
    
    public Cluster(Backend... backends) {
        this.members = new ClusterMember[backends.length];
        for(int i = 0 ; i<backends.length; i++) {
            members[i] = new ClusterMember(this, backends[i], i);
        }
    }
    
    public void refreshStatus() {
        int ups = 0;
        for (ClusterMember host : members) {
            if (host.isUp())
                ups++;
        }
        up.set(ups);
        String message = "Cluster has " + up.get() + " backend up among " + members.length + ".";
        log.warn(message);
    }

    public ClusterMember[] getMembers() {
        return members;
    }

    @Override
    protected void doStart() throws Exception {
        for (ClusterMember host : members)
            host.start();
        up.set(members.length);
        proberThread = new Thread() {
            public void run() {
                while (!mustStop.get()) {
                    try {
                        for (ClusterMember h : members)
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

    public ClusterMember getMember(int i) {
        return members[i];
    }

    public String getName() {
        if (members.length == 0)
            return "Empty hashring.";
        else
            return "HashRingBackend: " + members[0].getBackend().getName() + " et al.";
    }

    @Override
    public void doStop() throws Exception {
        mustStop.set(true);
        proberThread.join();
        for (ClusterMember host : members)
            host.stop();
    }
    
    public boolean oneUp() {
        return up.get() > 0;
    }

    public boolean allUp() {
        return up.get() == members.length;
    }

    public int getSleepMS() {
        return sleepMS;
    }

    public void setSleepMS(int sleep) {
        this.sleepMS = sleep;
    }



}
