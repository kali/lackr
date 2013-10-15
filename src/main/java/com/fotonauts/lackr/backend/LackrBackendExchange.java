package com.fotonauts.lackr.backend;

import static com.fotonauts.commons.RapportrLoggingKeys.DATE;
import static com.fotonauts.commons.RapportrLoggingKeys.ELAPSED;
import static com.fotonauts.commons.RapportrLoggingKeys.FRAGMENT_ID;
import static com.fotonauts.commons.RapportrLoggingKeys.METHOD;
import static com.fotonauts.commons.RapportrLoggingKeys.PARENT;
import static com.fotonauts.commons.RapportrLoggingKeys.PARENT_ID;
import static com.fotonauts.commons.RapportrLoggingKeys.PATH;
import static com.fotonauts.commons.RapportrLoggingKeys.QUERY_PARMS;
import static com.fotonauts.commons.RapportrLoggingKeys.SIZE;
import static com.fotonauts.commons.RapportrLoggingKeys.STATUS;

import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.commons.RapportrService;
import com.fotonauts.lackr.BaseGatewayMetrics;
import com.fotonauts.lackr.LackrFrontendRequest;
import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.backend.LackrBackendRequest.CompletionListener;
import com.fotonauts.lackr.backend.hashring.HashRing.NotAvailableException;
import com.mongodb.BasicDBObject;

public abstract class LackrBackendExchange {

    static Logger log = LoggerFactory.getLogger(LackrBackendExchange.class);

    public abstract List<String> getResponseHeaderValues(String name);

    public abstract List<String> getResponseHeaderNames();

    public abstract void addRequestHeader(String name, String value);

    public abstract String getResponseHeader(String name);

    public abstract byte[] getResponseBodyBytes();
    
    public abstract int getResponseStatus();
    
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

    public abstract BaseGatewayMetrics getUpstream() throws NotAvailableException;
    
    public void start() throws NotAvailableException {
        log.debug("Starting exchange {}", this);
        addRequestHeader("X-NGINX-SSI", "yes");
        addRequestHeader("X-SSI-ROOT", lackrBackendRequest.getFrontendRequest().getRequest().getRequestURI());
        addRequestHeader("X-FTN-NORM-USER-AGENT", lackrBackendRequest.getFrontendRequest().getUserAgent().toString());
        addRequestHeader("X-FTN-INLINE-IMAGES", lackrBackendRequest.getFrontendRequest().getUserAgent()
                .supportsInlineImages() ? "yes" : "no");
        for(Entry<String, String> h : lackrBackendRequest.getFrontendRequest().getAncilliaryHeaders().entrySet()) {
            addRequestHeader(h.getKey(), h.getValue());
        }
        if (lackrBackendRequest.getParentQuery() != null)
            addRequestHeader("X-SSI-PARENT", lackrBackendRequest.getParentQuery());
        if (lackrBackendRequest.getSyntax() != null)
            addRequestHeader("X-SSI-INCLUDE-SYNTAX", lackrBackendRequest.getSyntax());

        for (Enumeration<?> e = lackrBackendRequest.getFrontendRequest().getRequest().getHeaderNames(); e.hasMoreElements();) {
            String header = (String) e.nextElement();
            if (!LackrFrontendRequest.skipHeader(header)) {
                addRequestHeader(header, lackrBackendRequest.getFrontendRequest().getRequest().getHeader(header));
            }
            // content type is skipped, but we MUST copy it for the root request, or else...
            if("Content-Type".equalsIgnoreCase(header) && lackrBackendRequest.getParentQuery() == null) {
                addRequestHeader(header, lackrBackendRequest.getFrontendRequest().getRequest().getHeader(header));                
            }
        }
        startTimestamp = System.currentTimeMillis();
        logLine = RapportrService.accessLogLineTemplate(lackrBackendRequest.getFrontendRequest().getRequest(), "lackr-back");

        // ESI logline overides
        logLine.put(METHOD.getPrettyName(), lackrBackendRequest.getMethod());
        logLine.put(PATH.getPrettyName(), lackrBackendRequest.getPath());
        logLine.put(QUERY_PARMS.getPrettyName(), lackrBackendRequest.getParams());
        logLine.put(FRAGMENT_ID.getPrettyName(), lackrBackendRequest.hashCode());
        if (lackrBackendRequest.getParentId() != 0) {
            logLine.put(PARENT_ID.getPrettyName(), lackrBackendRequest.getParentId());
        }
        if (lackrBackendRequest.getParentQuery() != null) {
            logLine.put(PARENT.getPrettyName(), lackrBackendRequest.getParentQuery());
        }

        /*
        getUpstream().getRequestCountHolder().inc();
        getUpstream().getRunningRequestsHolder().inc();
*/
        try {
            doStart();
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            getBackendRequest().getFrontendRequest().addBackendExceptions(LackrPresentableError.fromThrowableAndExchange(e, this));
        }

    }

    /*
    public void onResponseComplete(boolean sync) {
        log.debug("onResponseComplete {} for {}", getResponseStatus(), this);
        long endTimestamp = System.currentTimeMillis();
        
        /*
        try {
            getUpstream().getRunningRequestsHolder().dec();
            getUpstream().getElapsedMillisHolder().inc(endTimestamp - startTimestamp);
        } catch (NotAvailableException e) {
            // At this stage, this is highly unlikely as it has been picked at start time
        }
            */
/*
        logLine.put(STATUS.getPrettyName(), getResponseStatus());
        if (getResponseBodyBytes() != null)
            logLine.put(SIZE.getPrettyName(), getResponseBodyBytes().length);
        logLine.put(DATE.getPrettyName(), new Date().getTime());
        logLine.put(ELAPSED.getPrettyName(), 0.001 * (endTimestamp - startTimestamp));
        if(getResponseHeader("X-Ftn-Picor-Endpoint") != null) {
            logLine.put("picorEP", getResponseHeader("X-Ftn-Picor-Endpoint"));
        }
        lackrBackendRequest.getFrontendRequest().getService().getRapportr().log(logLine);
        */
    public void onComplete() {
        /*
        if (sync) {
            log.debug("Start post-processing {}", this);
            completionListener.run();
        } else
        */
            log.debug("Enqueue post-processing {}", this);
            lackrBackendRequest.getFrontendRequest().getService().getExecutor().execute(new Runnable() {

                @Override
                public void run() {
                    getCompletionListener().run();
                }
            });

    }

    protected abstract void doStart() throws Exception;

    public String getResponseHeaderValue(String name) {
        List<String> values = null;
        try {
            values = getResponseHeaderValues(name);
        } catch (NullPointerException exception) {
            return null;
        }
        if (values == null || values.isEmpty())
            return null;
        else
            return values.get(0);
    }
    
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
