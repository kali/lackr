package com.fotonauts.lackr.backend.trypass;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.Backend;
import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.LackrBackendRequest;
import com.fotonauts.lackr.LackrBackendResponse;
import com.fotonauts.lackr.CompletionListener;
import com.fotonauts.lackr.backend.hashring.HashRingBackend.NotAvailableException;

public class TryPassBackendExchange extends LackrBackendExchange {

    static Logger log = LoggerFactory.getLogger(TryPassBackendExchange.class);
    
    private LackrBackendRequest effectiveBackendRequest; 
    
    private AtomicInteger triedBackend = new AtomicInteger(0);

    private AtomicReference<LackrBackendExchange> lastExchange = new AtomicReference<LackrBackendExchange>();

    public TryPassBackendExchange(TryPassBackend backend, LackrBackendRequest spec) {
        super(backend, spec);
        effectiveBackendRequest = spec;
    }

    protected void tryNext() throws NotAvailableException {
        int next = triedBackend.get();
        final Backend be = ((TryPassBackend) backend).getBackends()[next];
        log.debug("Trying {} for {}", be, getBackendRequest());
        final LackrBackendExchange subExchange = be.createExchange(effectiveBackendRequest);
        lastExchange.set(subExchange);
        final TryPassBackendExchange that = this;
        final CompletionListener previousListener = subExchange.getCompletionListener();
        subExchange.setCompletionListener(new CompletionListener() {

            @Override
            public void complete() {
                try {
                    if(previousListener != null)
                        previousListener.complete();
                    log.debug("entering subExchange {} completion handler", subExchange);
                    ((TryPassBackend) backend).handleComplete(that, subExchange);
                } catch (Throwable e) {
                    if(previousListener != null)
                        previousListener.fail(e);
                    log.debug("Exception in completion handler", e);
                    that.getCompletionListener().fail(e);
                }
            }
            
            @Override
            public void fail(Throwable t) {
                if(previousListener != null)
                    previousListener.fail(t);
                log.debug("Failure handler for", t);
                that.getCompletionListener().fail(t);
            }

        });
        try {
            subExchange.start();
        } catch (Throwable e) {
            log.debug("Exception when starting query", e);
            that.getCompletionListener().fail(e);
        }
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

    // package visibility is intended
    AtomicInteger getTriedBackend() {
        return triedBackend;
    }

    public LackrBackendRequest getEffectiveBackendRequest() {
        return effectiveBackendRequest;
    }

    void setEffectiveBackendRequest(LackrBackendRequest effectiveBackendRequest) {
        this.effectiveBackendRequest = effectiveBackendRequest;
    }
}
