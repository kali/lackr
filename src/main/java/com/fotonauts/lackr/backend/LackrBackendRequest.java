package com.fotonauts.lackr.backend;

import java.io.IOException;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.LackrFrontendRequest;
import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.MimeType;
import com.fotonauts.lackr.backend.hashring.HashRing.NotAvailableException;
import com.fotonauts.lackr.interpolr.DataChunk;
import com.fotonauts.lackr.interpolr.Document;

/**
 * Represents the specification and current state of a request to be tried against one or more {@link Backend}.
 * 
 * @author kali
 *
 */
public class LackrBackendRequest {

    public static interface Listener {
        public void complete();
        public void fail(Throwable t);
    }
    
    static Logger log = LoggerFactory.getLogger(LackrBackendRequest.class);

    private final byte[] body;
    private Document parsedDocument;

    private final String method;
    private final String parentQuery;
    private final int parentId;
    private final String query;
    private final LackrFrontendRequest frontendRequest;
    private LackrBackendExchange exchange;
    private final String syntax;
    private final HttpFields fields;

    public LackrBackendRequest(LackrFrontendRequest frontendRequest, String method, String query, String parentQuery, int parentId,
            String syntax, byte[] body, HttpFields fields) {
        super();
        this.frontendRequest = frontendRequest;
        this.method = method;
        this.query = query;
        this.parentQuery = parentQuery;
        this.parentId = parentId;
        this.syntax = syntax;
        this.body = body;
        this.fields = fields;
    }

    /**
     * Returns the HTTP body of the request.
     * @return the body (null if the request does not have a body).
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * Returns the HTTP method of the request.
     * @return the method name as the usual capitalized string.
     */
    public String getMethod() {
        return method;
    }

    /**
     * Returns the parent query string.
     * @return the parent query string.
     */
    public String getParentQuery() {
        return parentQuery;
    }

    /**
     * For tracing purposes.
     * @return the parent id.
     */
    public int getParentId() {
        return parentId;
    }

    /**
     * Return the full query to send (both path and parameters).  
     * @return the query 
     */
    public String getQuery() {
        return query;
    }

    public LackrFrontendRequest getFrontendRequest() {
        return frontendRequest;
    }

    /**
     * For ESI queries, denote the context kind in where the request was done (ml-like, or json based).
     * @return the ESI syntax.
     */
    public String getSyntax() {
        return syntax;
    }

    /**
     * Path part of the query.
     * @return the path.
     */
    public String getPath() {
        return query.indexOf('?') == -1 ? query : query.substring(0, query.indexOf('?'));
    }

    /**
     * Parameters part of the request.
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
    public void postProcess() {
        LackrBackendExchange exchange = getExchange();
        try {
            /*
            if (log.isDebugEnabled()) {
                log.debug(String.format("%s %s backend %s returned %d (?)", getMethod(), getQuery(), getFrontendRequest()
                        .getService().getBackends()[triedBackend.get()].getClass().getName(), exchange.getResponseStatus()));
            }
            */
            /*
            if (exchange.getResponseHeaderValues("X-Ftn-Set-Request-Header") != null) {
                for (String setRequestHeaders : exchange.getResponseHeaderValues("X-Ftn-Set-Request-Header")) {
                    int index = setRequestHeaders.indexOf(':');
                    if (index > 0) {
                        getFrontendRequest().addAncilliaryHeader(setRequestHeaders.substring(0, index),
                                setRequestHeaders.substring(index + 1).trim());
                    }
                }
            }
*/
            /*
            if(exchange.getResponseHeader("X-Ftn-Picor-Endpoint") != null) {
                getFrontendRequest().getBackendRequestEndpointsCounters().putIfAbsent(exchange.getResponseHeader("X-Ftn-Picor-Endpoint"), new AtomicInteger(0));
                getFrontendRequest().getBackendRequestEndpointsCounters().get(exchange.getResponseHeader("X-Ftn-Picor-Endpoint")).incrementAndGet();
            }
            */

            if (this != getFrontendRequest().getRootRequest()
                    && (exchange.getResponse().getStatus() / 100 == 4 || exchange.getResponse().getStatus() / 100 == 5)
                    && exchange.getResponse().getHeader("X-SSI-AWARE") == null)
                getFrontendRequest().addBackendExceptions(
                        new LackrPresentableError("Fragment " + getQuery() + " returned code " + exchange.getResponse().getStatus()));
            if (exchange.getResponse().getBodyBytes() != null && exchange.getResponse().getBodyBytes().length > 0) {
                String mimeType = exchange.getResponse().getHeader(HttpHeader.CONTENT_TYPE.asString());
                if (MimeType.isML(mimeType) || MimeType.isJS(mimeType))
                    parsedDocument = getFrontendRequest().getService().getInterpolr().parse(exchange.getResponse().getBodyBytes(), this);
                else
                    parsedDocument = new Document(new DataChunk(exchange.getResponse().getBodyBytes()));
            } else
                parsedDocument = new Document(new DataChunk(new byte[0]));

        } catch (Throwable e) {
            e.printStackTrace();
            getFrontendRequest().addBackendExceptions(LackrPresentableError.fromThrowable(e));
        }
    }

    public Document getParsedDocument() {
        return parsedDocument;
    }

    public void start() throws NotAvailableException, IOException {
        log.debug("Starting request on fragment {} {}", getMethod(), getQuery());
        exchange = getFrontendRequest().getService().getBackend().createExchange(this);
        log.debug("Created exchange {}", exchange);
        exchange.setCompletionListener(new Listener() {
            
            @Override
            public void complete() {
                try {
                    postProcess();
                } finally {
                    getFrontendRequest().notifySubRequestDone();
                }
            }

            @Override
            public void fail(Throwable t) {
                getFrontendRequest().addBackendExceptions(t instanceof LackrPresentableError ? t : LackrPresentableError.fromThrowableAndExchange(t, exchange));
                getFrontendRequest().notifySubRequestDone();
            }
        });
        exchange.start();
    }

    @Override
    public String toString() {
        return String.format("%s %s", getMethod(), getQuery());
    }

    public HttpFields getFields() {
        return fields;
    }
}
