package com.fotonauts.lackr;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jetty.http.HttpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.interpolr.DataChunk;
import com.fotonauts.lackr.interpolr.Document;

/**
 * Represents the specification and current state of a request to be tried against one or more {@link Backend}.
 * 
 * @author kali
 *
 */
public class BackendRequest {

    static Logger log = LoggerFactory.getLogger(BackendRequest.class);

    private final byte[] body;
    private Document parsedDocument;

    private final String method;
    private final String parentQuery;
    private final int parentId;
    private String query;
    private final LackrFrontendRequest frontendRequest;

    private AtomicReference<LackrBackendExchange> lastExchange = new AtomicReference<LackrBackendExchange>();

    private final String syntax;

    private AtomicInteger triedBackend = new AtomicInteger(0);

    public BackendRequest(LackrFrontendRequest frontendRequest, String method, String query, String parentQuery, int parentId,
            String syntax, byte[] body) {
        super();
        this.frontendRequest = frontendRequest;
        this.method = method;
        this.query = query;
        this.parentQuery = parentQuery;
        this.parentId = parentId;
        this.syntax = syntax;
        this.body = body;
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
     * Start processing by trying the first possible {@link Backend}.
     * @throws Throwable
     */
    public void start() throws Throwable {
        tryNext();
    }

    protected void tryNext() throws Throwable {
        try {
            int next = triedBackend.get();
            LackrBackendExchange exchange = getFrontendRequest().getService().getBackends()[next].createExchange(this);
            lastExchange.set(exchange);
            exchange.start();
        } catch(Throwable e) {
            getFrontendRequest().addBackendExceptions(e);
            throw e;
        }
    }

    /**
     * Get the {@link LackrBackendExchange} for the current {@link Backend} being tries (or the last one tried).
     * @return the exchange.
     */
    public LackrBackendExchange getExchange() {
        return lastExchange.get();
    }

    protected void postProcess() {
        LackrBackendExchange exchange = getExchange();
        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format("%s %s backend %s returned %d (?)", getMethod(), getQuery(), getFrontendRequest()
                        .getService().getBackends()[triedBackend.get()].getClass().getName(), exchange.getResponseStatus()));
            }
            if (exchange.getResponseHeaderValues("X-Ftn-Set-Request-Header") != null) {
                for (String setRequestHeaders : exchange.getResponseHeaderValues("X-Ftn-Set-Request-Header")) {
                    int index = setRequestHeaders.indexOf(':');
                    if (index > 0) {
                        getFrontendRequest().addAncilliaryHeader(setRequestHeaders.substring(0, index),
                                setRequestHeaders.substring(index + 1).trim());
                    }
                }
            }
            if (exchange.getResponseStatus() == 501) {
                if (triedBackend.incrementAndGet() < getFrontendRequest().getService().getBackends().length) {
                    tryNext();
                    return;
                }
            }

            getFrontendRequest().getBackendRequestCounts()[triedBackend.get()].incrementAndGet();
            if(exchange.getResponseHeader("X-Ftn-Picor-Endpoint") != null) {
                getFrontendRequest().getBackendRequestEndpointsCounters().putIfAbsent(exchange.getResponseHeader("X-Ftn-Picor-Endpoint"), new AtomicInteger(0));
                getFrontendRequest().getBackendRequestEndpointsCounters().get(exchange.getResponseHeader("X-Ftn-Picor-Endpoint")).incrementAndGet();
            }
            
            if (exchange.getResponseStatus() == 399) {
                this.query = exchange.getResponseHeaderValue(HttpHeader.LOCATION.asString());
                triedBackend.set(0);
                tryNext();
                return;
            }

            if (this != getFrontendRequest().getRootRequest()
                    && (exchange.getResponseStatus() / 100 == 4 || exchange.getResponseStatus() / 100 == 5)
                    && exchange.getResponseHeader("X-SSI-AWARE") == null)
                getFrontendRequest().addBackendExceptions(
                        new LackrPresentableError("Fragment " + getQuery() + " returned code " + exchange.getResponseStatus()));
            if (exchange.getRawResponseContent() != null && exchange.getRawResponseContent().length > 0) {
                String mimeType = exchange.getResponseHeader(HttpHeader.CONTENT_TYPE.asString());
                if (MimeType.isML(mimeType) || MimeType.isJS(mimeType))
                    parsedDocument = getFrontendRequest().getService().getInterpolr().parse(exchange.getRawResponseContent(), this);
                else
                    parsedDocument = new Document(new DataChunk(exchange.getRawResponseContent()));
            } else
                parsedDocument = new Document(new DataChunk(new byte[0]));

        } catch (Throwable e) {
            e.printStackTrace();
            getFrontendRequest().addBackendExceptions(LackrPresentableError.fromThrowable(e));
        }

        getFrontendRequest().notifySubRequestDone();
    }

    public Document getParsedDocument() {
        return parsedDocument;
    }

}
