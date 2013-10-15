package com.fotonauts.lackr.backend.trypass;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.BaseGatewayMetrics;
import com.fotonauts.lackr.backend.Backend;
import com.fotonauts.lackr.backend.LackrBackendExchange;
import com.fotonauts.lackr.backend.LackrBackendRequest;
import com.fotonauts.lackr.backend.LackrBackendRequest.Listener;
import com.fotonauts.lackr.backend.LackrBackendResponse;
import com.fotonauts.lackr.backend.hashring.HashRing.NotAvailableException;

public class TryPassBackendExchange extends LackrBackendExchange {

    static Logger log = LoggerFactory.getLogger(TryPassBackendExchange.class);

    private AtomicInteger triedBackend = new AtomicInteger(0);

    private AtomicReference<LackrBackendExchange> lastExchange = new AtomicReference<LackrBackendExchange>();

    public TryPassBackendExchange(TryPassBackend backend, LackrBackendRequest spec) {
        super(backend, spec);
    }

    protected void tryNext() throws NotAvailableException {
        int next = triedBackend.get();
        final Backend be = ((TryPassBackend) backend).getBackends()[next];
        log.debug("Trying {} for {}", be, getBackendRequest());
        final LackrBackendExchange subExchange = be.createExchange(getBackendRequest());
        lastExchange.set(subExchange);
        final LackrBackendExchange that = this;
        subExchange.setCompletionListener(new Listener() {

            @Override
            public void complete() {
                try {
                    log.debug("entering subExchange {} completion handler", subExchange);
                    if (subExchange.getResponse().getStatus() == 501
                            && triedBackend.incrementAndGet() < ((TryPassBackend) backend).getBackends().length) {
                        tryNext();
                    } else {
                        log.debug("{} handled {}, calling my own completion listener {}.", be, getBackendRequest(),
                                that.getCompletionListener());
                        that.getCompletionListener().complete();
                    }
                } catch (Throwable e) {
                    System.err.println("1");
                    that.getCompletionListener().fail(e);
                }
            }
            
            @Override
            public void fail(Throwable t) {
                System.err.println("2");
                that.getCompletionListener().fail(t);
            }

        });
        subExchange.start();
    }

    @Override
    public BaseGatewayMetrics getUpstream() throws NotAvailableException {
        return null;
    }

    @Override
    protected void doStart() throws Exception {
        log.debug("doStart {}, completion listener: {}", this, getCompletionListener());
        tryNext();
    }

    @Override
    public LackrBackendResponse getResponse() {
        return lastExchange.get().getResponse();
    }
}
