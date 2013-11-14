package com.fotonauts.lackr;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BaseProxy extends AbstractHandler {

    public static enum EtagMode {
        DISCARD, COPY, SUM
    }
    
    private String LACKR_STATE_ATTRIBUTE = "lackr.state.attribute";
    static Logger log = LoggerFactory.getLogger(BaseProxy.class);

    private int timeout;

    private Backend backend;

    private ExecutorService executor;

    public BaseProxy() {
    }

    @Override
    protected void doStart() throws Exception {
        log.debug("Starting...");
        backend.start();
        setExecutor(Executors.newFixedThreadPool(64));
        log.debug("Started.");
    }

    protected BaseFrontendRequest createLackrFrontendRequest(HttpServletRequest request) {
        return new BaseFrontendRequest(this, request);
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        BaseFrontendRequest state = (BaseFrontendRequest) request.getAttribute(LACKR_STATE_ATTRIBUTE);
        if (state == null) {
            log.debug("starting processing for: " + request.getRequestURL());
            state = createLackrFrontendRequest(request);
            request.setAttribute(LACKR_STATE_ATTRIBUTE, state);
            state.kick();
        } else {
            log.debug("resuming processing for: " + request.getRequestURL());
            state.writeResponse(response);
        }
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getTimeout() {
        return timeout;
    }

    public Backend getBackend() {
        return backend;
    }

    public void setBackend(Backend backend) {
        this.backend = backend;
    }

    @Override
    public void doStop() throws Exception {
        log.info("Stopping, thread count: " + Thread.getAllStackTraces().size());
        backend.stop();
        getExecutor().shutdown();
        getExecutor().awaitTermination(1, TimeUnit.SECONDS);
        log.info("Stopped thread count: " + Thread.getAllStackTraces().size());
    }
}
