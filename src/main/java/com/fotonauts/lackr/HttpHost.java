package com.fotonauts.lackr;

public abstract class HttpHost extends Gateway {

    @Override
    public abstract String getMBeanName();
    
    public abstract String getHostname();
}
