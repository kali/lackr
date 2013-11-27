package com.fotonauts.lackr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BaseFrontendRequest {

    static Logger log = LoggerFactory.getLogger(BaseFrontendRequest.class);

    protected HttpServletRequest incomingServletRequest;

    protected BaseProxy proxy;

    protected LackrBackendRequest backendRequest;

    private AsyncContext continuation;

    private List<LackrPresentableError> backendExceptions = Collections.synchronizedList(new ArrayList<LackrPresentableError>(5));

    protected BaseFrontendRequest(final BaseProxy baseProxy, HttpServletRequest request) {
        this.proxy = baseProxy;
        this.incomingServletRequest = request;
        this.continuation = request.startAsync();
        this.continuation.setTimeout(getProxy().getTimeout() * 1000);
        /*
        this.continuation.addListener(new AsyncListener() {

            @Override
            public void onComplete(AsyncEvent event) throws IOException {
            }

            @Override
            public void onTimeout(AsyncEvent event) throws IOException {
                // TODO Auto-generated method stub

            }

            @Override
            public void onError(AsyncEvent event) throws IOException {
                // TODO Auto-generated method stub

            }

            @Override
            public void onStartAsync(AsyncEvent event) throws IOException {
                // TODO Auto-generated method stub

            }
        });
        */
    }

    public void addError(LackrPresentableError x) {
        log.debug("Registering error:", x);
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

}
