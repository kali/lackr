package com.fotonauts.lackr;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.http.HttpFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the specification and current state of a incomingServletRequest to be tried against one or more {@link Backend}.
 * 
 * @author kali
 *
 */
public class LackrBackendRequest {

    static Logger log = LoggerFactory.getLogger(LackrBackendRequest.class);

    private final byte[] body;

    private final String method;
    private final String query;
    private final BaseFrontendRequest frontendRequest;
    private LackrBackendExchange exchange;
    private final HttpFields fields;
    private final CompletionListener completionListener;
    private final Map<String, Object> attributes;

    public LackrBackendRequest(BaseFrontendRequest frontendRequest, String method, String query, byte[] body, HttpFields fields,
            Map<String, Object> attributes, CompletionListener completionListener) {
        super();
        this.frontendRequest = frontendRequest;
        this.method = method;
        this.query = query;
        this.body = body;
        this.fields = fields;
        this.completionListener = completionListener;
        if (attributes == null)
            this.attributes = new HashMap<>();
        else
            this.attributes = attributes;
    }

    /**
     * Returns the HTTP body of the incomingServletRequest.
     * @return the body (null if the incomingServletRequest does not have a body).
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * Returns the HTTP method of the incomingServletRequest.
     * @return the method name as the usual capitalized string.
     */
    public String getMethod() {
        return method;
    }

    /**
     * Return the full query to send (both path and servlet "query string").  
     * @return the query 
     */
    public String getQuery() {
        return query;
    }

    public BaseFrontendRequest getFrontendRequest() {
        return frontendRequest;
    }

    /**
     * Path part of the query.
     * @return the path.
     */
    public String getPath() {
        return query.indexOf('?') == -1 ? query : query.substring(0, query.indexOf('?'));
    }

    /**
     * Parameters part of the incomingServletRequest.
     * @return the parameters (can be null, matching the Servlet API convention).
     */
    public String getParams() {
        return query.indexOf('?') == -1 ? null : query.substring(query.indexOf('?') + 1);
    }

    /**
     * Get the {@link LackrBackendExchange} for the current {@link Backend} being tries (or the last one tried).
     * @return the exchange.
     */
    public LackrBackendExchange getExchange() {
        return exchange;
    }

    // TODO: "parsedDocument": that is interpolr crap
    //    public void postProcess() {
    /*
    LackrBackendExchange exchange = getExchange();
    try {
    */
    /*
    if (log.isDebugEnabled()) {
        log.debug(String.format("%s %s backend %s returned %d (?)", getMethod(), getQuery(), getFrontendRequest()
                .getService().getBackends()[triedBackend.get()].getClass().getName(), exchange.getResponseStatus()));
    }
    */
    /*
    if(exchange.getResponseHeader("X-Ftn-Picor-Endpoint") != null) {
        getFrontendRequest().getBackendRequestEndpointsCounters().putIfAbsent(exchange.getResponseHeader("X-Ftn-Picor-Endpoint"), new AtomicInteger(0));
        getFrontendRequest().getBackendRequestEndpointsCounters().get(exchange.getResponseHeader("X-Ftn-Picor-Endpoint")).incrementAndGet();
    }
    */
    /*
                if (this != getFrontendRequest().getRootRequest()
                        && (exchange.getResponse().getStatus() / 100 == 4 || exchange.getResponse().getStatus() / 100 == 5)
                        && exchange.getResponse().getHeader("X-SSI-AWARE") == null)
                    getFrontendRequest().addBackendExceptions(
                            new LackrPresentableError("Fragment " + getQuery() + " returned code " + exchange.getResponse().getStatus()));
                if (exchange.getResponse().getBodyBytes() != null && exchange.getResponse().getBodyBytes().length > 0) {
                    parsedDocument = getFrontendRequest().postProcessBodyToDocument(exchange);
                } else
                    parsedDocument = new Document(new DataChunk(new byte[0]));
    */
    /*
    } catch (Throwable e) {
        e.printStackTrace();
        getFrontendRequest().addBackendExceptions(LackrPresentableError.fromThrowable(e));
    }
    */
    //    }

    public void start() throws IOException {
        log.debug("Starting incomingServletRequest on fragment {} {}", getMethod(), getQuery());
        exchange = getFrontendRequest().getProxy().getBackend().createExchange(this);
        log.debug("Created exchange {}", exchange);
        exchange.setCompletionListener(completionListener);
        exchange.start();
    }

    @Override
    public String toString() {
        return String.format("%s %s", getMethod(), getQuery());
    }

    public HttpFields getFields() {
        return fields;
    }

    public CompletionListener getCompletionListener() {
        return completionListener;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
