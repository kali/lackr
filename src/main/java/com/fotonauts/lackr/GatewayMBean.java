package com.fotonauts.lackr;

public interface GatewayMBean {

    public abstract long getElapsedMillis();

    public abstract long getRunningRequests();

    public abstract long getRequestCount();

}
