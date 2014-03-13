package com.fotonauts.lackr.backend.inprocess;

import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.LackrBackendRequest;

public class InProcessExchange extends LackrBackendExchange {

    static Logger log = LoggerFactory.getLogger(InProcessExchange.class);

    private InProcessRequest request;
    private InProcessResponse response;
    private InProcessBackend inProcessBackend;
    // package visibility on purpose
    AtomicReference<Thread> thread = new AtomicReference<>();

    public InProcessExchange(InProcessBackend inProcessBackend, LackrBackendRequest spec) {
        super(inProcessBackend, spec);
        this.request = new InProcessRequest(spec.getFrontendRequest().getIncomingServletRequest(), spec);
        this.response = new InProcessResponse(this);
        this.inProcessBackend = inProcessBackend;
    }

    @Override
    protected void doStart() throws Exception {
        try {
            inProcessBackend.registerTimeout(this);
            inProcessBackend.getFilter().doFilter(request, response, null);
        } catch (Throwable t) {
            response.setStatus(500);
            throw t;
        } finally {
            inProcessBackend.unregisterTimeout(this);
            onComplete();
        }
    }

    public InProcessResponse getResponse() {
        return response;
    }

}
