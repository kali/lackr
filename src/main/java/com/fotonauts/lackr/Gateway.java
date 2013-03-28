package com.fotonauts.lackr;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.MetricName;

public abstract class Gateway implements GatewayMBean {

    private Counter runningRequests;
    private Counter requestCount;
    private Counter elapsedMillis;

    public abstract String getMBeanName();
    
    public void start() {
        runningRequests = Metrics.newCounter(new MetricName("lackr", "gw", getMBeanName(), "running-requests"));
        requestCount = Metrics.newCounter(new MetricName("lackr", "gw", getMBeanName(), "request-count"));
        elapsedMillis = Metrics.newCounter(new MetricName("lackr", "gw", getMBeanName(), "elapsedMillis"));
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
