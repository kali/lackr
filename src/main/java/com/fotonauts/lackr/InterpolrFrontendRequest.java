package com.fotonauts.lackr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.backend.LackrBackendExchange;
import com.fotonauts.lackr.backend.LackrBackendRequest;
import com.fotonauts.lackr.backend.hashring.HashRing.NotAvailableException;
import com.fotonauts.lackr.interpolr.Document;
import com.fotonauts.lackr.interpolr.Interpolr;
import com.fotonauts.lackr.mustache.MustacheContext;
import com.mongodb.BasicDBObject;

public class InterpolrFrontendRequest extends BaseFrontendRequest {

    AtomicInteger pendingCount;

    static Logger log = LoggerFactory.getLogger(InterpolrFrontendRequest.class);

    protected Map<String, String> ancillaryHeaders = Collections.synchronizedMap(new HashMap<String, String>(5));

    protected InterpolrProxy service;

    protected LackrBackendRequest rootRequest;

    private List<LackrPresentableError> backendExceptions = Collections.synchronizedList(new ArrayList<LackrPresentableError>(5));

    protected BasicDBObject logLine;

    protected long startTimestamp;

    private MustacheContext mustacheContext;

    private ConcurrentHashMap<String, LackrBackendRequest> backendRequestCache = new ConcurrentHashMap<String, LackrBackendRequest>();

    InterpolrFrontendRequest(final InterpolrProxy baseProxy, HttpServletRequest request) {
        super(baseProxy, request);
        this.service = baseProxy;
        this.request = request;
        this.pendingCount = new AtomicInteger(0);
        this.mustacheContext = new MustacheContext(this);
    }

    public LackrBackendRequest getSubBackendExchange(String url, String format, LackrBackendRequest dad)
            throws NotAvailableException {
        String key = format + "::" + url;
        LackrBackendRequest ex = backendRequestCache.get(key);
        if (ex != null)
            return ex;
        ex = new LackrBackendRequest(this, "GET", url, dad.getQuery(), dad.hashCode(), format, null, dad.getFields());
        backendRequestCache.put(key, ex);
        scheduleUpstreamRequest(ex);
        return ex;
    }

    protected void preflightCheck() {
        try {
            getMustacheContext().checkAndCompileAll();
            if (rootRequest.getParsedDocument() != null) {
                rootRequest.getParsedDocument().check();
            }
        } catch (Throwable e) {
            backendExceptions.add(LackrPresentableError.fromThrowable(e));
        }
    }

    public void writeResponse(HttpServletResponse response) throws IOException {
        if (pendingCount.get() > 0)
            backendExceptions.add(new LackrPresentableError("There is unfinished business with backends..."));

        super.writeResponse(response);
    }

    @Override
    protected void scheduleUpstreamRequest(LackrBackendRequest request) throws NotAvailableException {
        pendingCount.incrementAndGet();
        super.scheduleUpstreamRequest(request);
    }
    
    public void notifySubRequestDone() {
        log.debug("notifySubRequestDone (pending was: {})", pendingCount.get());
        if (pendingCount.decrementAndGet() <= 0) {
            log.debug("Gathered all fragments for {} with {} exceptions. Re-dispatching request.", getPathAndQuery(request),
                    backendExceptions.size());
            super.notifySubRequestDone();
        }
    }

    public MustacheContext getMustacheContext() {
        return mustacheContext;
    }

    public Interpolr getInterpolr() {
        return service.getInterpolr();
    }

    @Override
    public Document postProcessBodyToDocument(LackrBackendExchange exchange) {
        String mimeType = exchange.getResponse().getHeader(HttpHeader.CONTENT_TYPE.asString());
        if (MimeType.isML(mimeType) || MimeType.isJS(mimeType))
            return getInterpolr().parse(exchange.getResponse().getBodyBytes(), exchange.getBackendRequest());
        else
            return super.postProcessBodyToDocument(exchange);
    }
}
