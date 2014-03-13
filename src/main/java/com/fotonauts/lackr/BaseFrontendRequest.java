package com.fotonauts.lackr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * State object holding the progression of a request.
 * 
 * <p>Of particular notice, the asynchronous context from the container,
 * the incoming request, and a list of errors.
 * 
 * <p>Presents the reDispatchOnce() method, outcome of the asynchronous processing.
 * 
 * <p>Will be extended as Interpolr needs more variables to keep track of things.
 * 
 * @author kali
 *
 */
public class BaseFrontendRequest {

    static Logger log = LoggerFactory.getLogger(BaseFrontendRequest.class);

    protected HttpServletRequest incomingServletRequest;

    protected BaseProxy proxy;

    protected LackrBackendRequest backendRequest;

    private AsyncContext continuation;

    private List<LackrPresentableError> backendExceptions = Collections.synchronizedList(new ArrayList<LackrPresentableError>(5));

    private boolean dispatched = false;

    protected BaseFrontendRequest(final BaseProxy baseProxy, HttpServletRequest request) {
        this.proxy = baseProxy;
        this.incomingServletRequest = request;
        this.continuation = request.startAsync();
        this.continuation.setTimeout(getProxy().getTimeout() * 1000);
    }

    public void addError(LackrPresentableError x) {
        log.debug("Request {}, registering error: {}", this, x);
        backendExceptions.add(x);
    }

    public void addBackendExceptions(Throwable x) {
        addError(LackrPresentableError.fromThrowable(x));
    }

    public HttpServletRequest getIncomingServletRequest() {
        return incomingServletRequest;
    }

    public LackrBackendRequest getBackendRequest() {
        return backendRequest;
    }

    public BaseProxy getProxy() {
        return proxy;
    }

    public List<LackrPresentableError> getErrors() {
        return backendExceptions;
    }

    public int getContentLength() {
        if (getBackendRequest().getExchange().getResponse().getBodyBytes() != null)
            return getBackendRequest().getExchange().getResponse().getBodyBytes().length;
        return 0;
    }

    public AsyncContext getContinuation() {
        return continuation;
    }

    public void setBackendRequest(LackrBackendRequest rootRequest) {
        this.backendRequest = rootRequest;
    }

    public synchronized void reDispatchOnce() {
        if (!dispatched) {
            dispatched = true;
            getContinuation().dispatch();
        }
    }

}
