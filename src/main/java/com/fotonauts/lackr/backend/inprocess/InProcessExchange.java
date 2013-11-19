package com.fotonauts.lackr.backend.inprocess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.LackrBackendRequest;

public class InProcessExchange extends LackrBackendExchange {

    static Logger log = LoggerFactory.getLogger(InProcessExchange.class);
    
	private InProcessRequest request;
	private InProcessResponse response;
	private InProcessBackend inProcessBackend;

	public InProcessExchange(InProcessBackend inProcessBackend, LackrBackendRequest spec) {
		super(inProcessBackend, spec);
		this.request = new InProcessRequest(spec.getFrontendRequest().getRequest(), spec);
		this.response = new InProcessResponse(this);
		this.inProcessBackend = inProcessBackend;
	}

	@Override
	protected void doStart() throws Exception {
        inProcessBackend.getFilter().doFilter(request, response, null);
        onComplete();
	}

	public InProcessResponse getResponse() {
		return response;
	}

}
