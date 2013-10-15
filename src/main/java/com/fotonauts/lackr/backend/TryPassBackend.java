package com.fotonauts.lackr.backend;

import java.io.PrintStream;

import com.fotonauts.lackr.BaseGatewayMetrics;
import com.fotonauts.lackr.backend.hashring.HashRing.NotAvailableException;

public class TryPassBackend implements Backend {

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
    public void stop() throws Exception {
        for(Backend b: backends)
            b.stop();
    }

    @Override
    public BaseGatewayMetrics[] getGateways() {
        return new BaseGatewayMetrics[0];
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
