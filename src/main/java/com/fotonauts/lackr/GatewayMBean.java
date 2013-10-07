package com.fotonauts.lackr;

/**
 * JMX interface over {@link Gateway}.
 * @author kali
 *
 */
public interface GatewayMBean {

    /**
     * Total number of millisecond elapsed in processed requests.
     */
    public abstract long getElapsedMillis();

    /**
     * Total number of processed requests.
     */
    public abstract long getRunningRequests();

    /**
     * Current number of requests being concurrently processed.
     */
    public abstract long getRequestCount();

}
