package com.fotonauts.lackr.backend;

import org.eclipse.jetty.util.component.AbstractLifeCycle;

public abstract class HttpHost extends AbstractLifeCycle {

    public abstract String getHostname();
}
