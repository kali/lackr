package com.fotonauts.lackr;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;

public abstract class Gateway implements GatewayMBean {

    private Counter runningRequests;
    private Counter requestCount;
    private Counter elapsedMillis;

    public abstract String getMBeanName();
    
    public void start() {
        runningRequests = Metrics.newCounter(getClass(), "running-requests", getMBeanName());
        requestCount = Metrics.newCounter(getClass(), "request-count", getMBeanName());
        elapsedMillis = Metrics.newCounter(getClass(), "ellapsedMillis", getMBeanName());
    }
    
    @Override
    public long getRequestCount() {
        return requestCount.count();
    }

    @Override
    public long getRunningRequests() {
        return runningRequests.count();
    }

    @Override
    public long getElapsedMillis() {
        return elapsedMillis.count();
    }

    public Counter getRequestCountHolder() {
        return requestCount;
    }

    public Counter getElapsedMillisHolder() {
        return elapsedMillis;
    }
    
    public Counter getRunningRequestsHolder() {
        return runningRequests;
    }
   
}
