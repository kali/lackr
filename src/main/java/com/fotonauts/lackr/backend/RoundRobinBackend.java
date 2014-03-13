package com.fotonauts.lackr.backend;

import java.util.concurrent.atomic.AtomicInteger;

import com.fotonauts.lackr.Backend;
import com.fotonauts.lackr.LackrBackendRequest;

public class RoundRobinBackend extends BaseRoutingBackend implements Backend {

    private Cluster cluster;
    private AtomicInteger i = new AtomicInteger();

    public RoundRobinBackend(Backend... backends) {
        cluster = new Cluster(backends);
    }

    @Override
    public String getName() {
        return "RoundRobin: " + cluster.getName();
    }

    @Override
    public boolean probe() {
        return cluster.oneUp();
    }

    public ClusterMember chooseMemberFor(LackrBackendRequest req) {
        while(true) {
            int node = i.getAndIncrement();
            ClusterMember member = cluster.getMember(node % cluster.getMembers().length);
            if(member.isUp())
                return member;
            if(!probe())
                throw new RuntimeException("No backend up");
        }
    }
    
    @Override
    public Backend chooseBackendFor(LackrBackendRequest request) {
        return chooseMemberFor(request).getBackend();
    }
    
    @Override
    protected void doStart() throws Exception {
        cluster.start();
    }

    @Override
    protected void doStop() throws Exception {
        cluster.stop();
    }

    public Cluster getCluster() {
        return cluster;
    }
}
