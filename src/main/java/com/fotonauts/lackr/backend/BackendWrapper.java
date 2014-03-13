package com.fotonauts.lackr.backend;

import org.eclipse.jetty.util.component.AbstractLifeCycle;

import com.fotonauts.lackr.Backend;
import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.LackrBackendRequest;

public class BackendWrapper extends AbstractLifeCycle implements Backend {

    protected Backend wrapped;
    
    public BackendWrapper(Backend wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public LackrBackendExchange createExchange(LackrBackendRequest request) {
        return wrapped.createExchange(request);
    }

    @Override
    public String getName() {
        return wrapped.getName();
    }

    @Override
    public boolean probe() {
        return wrapped.probe();
    }
    
    @Override
    protected void doStart() throws Exception {
        wrapped.start();
    }
    
    @Override
    protected void doStop() throws Exception {
        wrapped.stop();
    }
}
