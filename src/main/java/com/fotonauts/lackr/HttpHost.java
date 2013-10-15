package com.fotonauts.lackr;

public abstract class HttpHost extends BaseGatewayMetrics {

    @Override
    public abstract String getMBeanName();
    
    public abstract String getHostname();
}
