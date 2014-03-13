package com.fotonauts.lackr.backend;

import org.eclipse.jetty.util.component.AbstractLifeCycle;

import com.fotonauts.lackr.Backend;
import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.LackrBackendRequest;

public abstract class BaseRoutingBackend extends AbstractLifeCycle implements Backend {

    public BaseRoutingBackend() {
        super();
    }

    public abstract Backend chooseBackendFor(LackrBackendRequest request);

    @Override
    public LackrBackendExchange createExchange(LackrBackendRequest request) {
        return chooseBackendFor(request).createExchange(request);
    }

    @Override
    public abstract boolean probe();

}