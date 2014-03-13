package com.fotonauts.lackr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;

public abstract class LackrBackendExchange {

    static Logger log = LoggerFactory.getLogger(LackrBackendExchange.class);

    public abstract LackrBackendResponse getResponse();

    protected BasicDBObject logLine;
    protected long startTimestamp;
    protected LackrBackendRequest lackrBackendRequest;
    protected Backend backend;
    private CompletionListener completionListener;

    public LackrBackendRequest getBackendRequest() {
        return lackrBackendRequest;
    }

    public LackrBackendExchange(Backend backend, LackrBackendRequest spec) {
        this.backend = backend;
        this.lackrBackendRequest = spec;
    }

    public void start() {
        log.debug("Starting exchange {}", this);
        startTimestamp = System.currentTimeMillis();
        try {
            doStart();
        } catch (Throwable e) {
            log.debug("Error starting incomingServletRequest", e);
            getCompletionListener().fail(e);
        }

    }

    public void onComplete() {
        log.debug("Enqueue post-processing {} (status: {})", this, getResponse() != null ? getResponse().getStatus() : null);
        lackrBackendRequest.getFrontendRequest().getProxy().getExecutor().execute(new Runnable() {

            @Override
            public void run() {
                getCompletionListener().complete();
            }
        });

    }

    protected abstract void doStart() throws Exception;

    @Override
    public String toString() {
        return String.format("%s:%s", this.getClass().getSimpleName(), getBackendRequest());
    }

    public CompletionListener getCompletionListener() {
        return completionListener;
    }

    public void setCompletionListener(CompletionListener completionListener) {
        this.completionListener = completionListener;
    }
}
