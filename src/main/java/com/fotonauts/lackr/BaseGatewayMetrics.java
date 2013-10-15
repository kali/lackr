package com.fotonauts.lackr;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.MetricName;

/**
 * Store raw statistical data at boundaries of lackr for monitoring purposes using {@link Metrics}.
 * 
 * @author kali
 *
 */
public abstract class BaseGatewayMetrics implements GatewayMetrics {

    private Counter runningRequests;
    private Counter requestCount;
    private Counter elapsedMillis;

    public abstract String getMBeanName();
    
    public void start() {
        runningRequests = Metrics.newCounter(new MetricName("lackr", "gw", "running-requests", getMBeanName()));
        requestCount = Metrics.newCounter(new MetricName("lackr", "gw", "request-count", getMBeanName()));
        elapsedMillis = Metrics.newCounter(new MetricName("lackr", "gw", "elapsed-millis", getMBeanName()));
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
