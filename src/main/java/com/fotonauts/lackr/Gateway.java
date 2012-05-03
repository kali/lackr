package com.fotonauts.lackr;

import java.util.concurrent.atomic.AtomicLong;

public abstract class Gateway implements GatewayMBean {

    private AtomicLong runningRequests = new AtomicLong();
    private AtomicLong requestCount = new AtomicLong();
    private AtomicLong elapsedMillis = new AtomicLong();

    public abstract String getMBeanName();
    
    @Override
    public long getRequestCount() {
        return requestCount.get();
    }

    @Override
    public long getRunningRequests() {
        return runningRequests.get();
    }

    @Override
    public long getElapsedMillis() {
        return elapsedMillis.get();
    }

    public AtomicLong getRequestCountHolder() {
        return requestCount;
    }

    public AtomicLong getElapsedMillisHolder() {
        return elapsedMillis;
    }
    
    public AtomicLong getRunningRequestsHolder() {
        return runningRequests;
    }
   
}
