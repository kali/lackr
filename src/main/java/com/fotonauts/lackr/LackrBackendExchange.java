package com.fotonauts.lackr;

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
import com.fotonauts.lackr.backend.client.ClientLackrBackendExchange;
import com.fotonauts.lackr.backend.hashring.HashRing.NotAvailableException;
import com.mongodb.BasicDBObject;

public abstract class LackrBackendExchange {

    static Logger log = LoggerFactory.getLogger(ClientLackrBackendExchange.class);

    public abstract List<String> getResponseHeaderValues(String name);

    protected abstract List<String> getResponseHeaderNames();

    public abstract void addRequestHeader(String name, String value);

    protected abstract String getResponseHeader(String name);

    protected abstract byte[] getResponseContentBytes();

    protected abstract int getResponseStatus();
    
    protected BasicDBObject logLine;
    protected long startTimestamp;
    protected byte[] rawResponseContent;
    protected BackendRequest backendRequest;

    public BackendRequest getBackendRequest() {
        return backendRequest;
    }

    public LackrBackendExchange(BackendRequest spec) {
        this.backendRequest = spec;
    }

    public abstract Gateway getUpstream() throws NotAvailableException;
    
    public void start() throws IOException, NotAvailableException, NullPointerException {
        addRequestHeader("X-NGINX-SSI", "yes");
        addRequestHeader("X-SSI-ROOT", backendRequest.getFrontendRequest().getRequest().getRequestURI());
        addRequestHeader("X-FTN-NORM-USER-AGENT", backendRequest.getFrontendRequest().getUserAgent().toString());
        addRequestHeader("X-FTN-INLINE-IMAGES", backendRequest.getFrontendRequest().getUserAgent()
                .supportsInlineImages() ? "yes" : "no");
        for(Entry<String, String> h : backendRequest.getFrontendRequest().getAncilliaryHeaders().entrySet()) {
            addRequestHeader(h.getKey(), h.getValue());
        }
        if (backendRequest.getParentQuery() != null)
            addRequestHeader("X-SSI-PARENT", backendRequest.getParentQuery());
        if (backendRequest.getSyntax() != null)
            addRequestHeader("X-SSI-INCLUDE-SYNTAX", backendRequest.getSyntax());

        for (Enumeration<?> e = backendRequest.getFrontendRequest().getRequest().getHeaderNames(); e.hasMoreElements();) {
            String header = (String) e.nextElement();
            if (!LackrFrontendRequest.skipHeader(header)) {
                addRequestHeader(header, backendRequest.getFrontendRequest().getRequest().getHeader(header));
            }
            // content type is skipped, but we MUST copy it for the root request, or else...
            if("Content-Type".equalsIgnoreCase(header) && backendRequest.getParentQuery() == null) {
                addRequestHeader(header, backendRequest.getFrontendRequest().getRequest().getHeader(header));                
            }
        }
        startTimestamp = System.currentTimeMillis();
        logLine = RapportrService.accessLogLineTemplate(backendRequest.getFrontendRequest().getRequest(), "lackr-back");

        // ESI logline overides
        logLine.put(METHOD.getPrettyName(), backendRequest.getMethod());
        logLine.put(PATH.getPrettyName(), backendRequest.getPath());
        logLine.put(QUERY_PARMS.getPrettyName(), backendRequest.getParams());
        logLine.put(FRAGMENT_ID.getPrettyName(), backendRequest.hashCode());
        if (backendRequest.getParentId() != 0) {
            logLine.put(PARENT_ID.getPrettyName(), backendRequest.getParentId());
        }
        if (backendRequest.getParentQuery() != null) {
            logLine.put(PARENT.getPrettyName(), backendRequest.getParentQuery());
        }

        getUpstream().getRequestCountHolder().inc();
        getUpstream().getRunningRequestsHolder().inc();

        doStart();
    }

    public void onResponseComplete(boolean sync) {
        rawResponseContent = getResponseContentBytes();
        long endTimestamp = System.currentTimeMillis();
        
        try {
            getUpstream().getRunningRequestsHolder().dec();
            getUpstream().getElapsedMillisHolder().inc(endTimestamp - startTimestamp);
        } catch (NotAvailableException e) {
            // At this stage, this is highly unlikely as it has been picked at start time
        }

        logLine.put(STATUS.getPrettyName(), getResponseStatus());
        final LackrBackendExchange exchange = this;
        if (rawResponseContent != null)
            logLine.put(SIZE.getPrettyName(), rawResponseContent.length);
        logLine.put(DATE.getPrettyName(), new Date().getTime());
        logLine.put(ELAPSED.getPrettyName(), 0.001 * (endTimestamp - startTimestamp));
        if(getResponseHeader("X-Ftn-Picor-Endpoint") != null) {
            logLine.put("picorEP", getResponseHeader("X-Ftn-Picor-Endpoint"));
        }
        backendRequest.getFrontendRequest().getService().getRapportr().log(logLine);
        if (sync)
            exchange.postProcess();
        else
            backendRequest.getFrontendRequest().getService().getExecutor().execute(new Runnable() {

                @Override
                public void run() {
                    exchange.postProcess();
                }
            });

    }

    protected abstract void doStart() throws IOException, NotAvailableException;

    protected void postProcess() {
        backendRequest.postProcess();
    }

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

    public byte[] getRawResponseContent() {
        return rawResponseContent;
    }
}
