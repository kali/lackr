package com.fotonauts.lackr.backend.trypass;

import java.io.PrintStream;

import org.eclipse.jetty.util.component.AbstractLifeCycle;

import com.fotonauts.lackr.Backend;
import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.LackrBackendRequest;
import com.fotonauts.lackr.backend.hashring.HashRing.NotAvailableException;

public class TryPassBackend extends AbstractLifeCycle implements Backend {

    private Backend[] backends;
    
    public TryPassBackend(Backend... backends) {
        this.backends = backends;
    }

    @Override
    public LackrBackendExchange createExchange(LackrBackendRequest request) throws NotAvailableException {
        return new TryPassBackendExchange(this, request);
    }

    @Override
    public void dumpStatus(PrintStream ps) {
        for(Backend b: backends)
            b.dumpStatus(ps);
    }

    @Override
    public void doStart() throws Exception {
        for(Backend b: backends)
            b.start();
    }

    @Override
    public void doStop() throws Exception {
        for(Backend b: backends)
            b.stop();
    }

    @Override
    public String getName() {
        StringBuilder builder = new StringBuilder();
        for(Backend b: backends) {
            if(builder.toString().length() != 0)
                builder.append("-");
            builder.append(b);
        }
        return builder.toString();
    }

    public Backend[] getBackends() {
        return backends;
    }

    @Override
    public String toString() {
        return getName();
    }
}
