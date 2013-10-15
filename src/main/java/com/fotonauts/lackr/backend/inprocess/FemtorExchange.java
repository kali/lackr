package com.fotonauts.lackr.backend.inprocess;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.BaseGatewayMetrics;
import com.fotonauts.lackr.backend.LackrBackendExchange;
import com.fotonauts.lackr.backend.LackrBackendRequest;
import com.fotonauts.lackr.backend.hashring.HashRing.NotAvailableException;

public class FemtorExchange extends LackrBackendExchange {

    static Logger log = LoggerFactory.getLogger(FemtorExchange.class);
    
	private FemtorRequest request;
	private FemtorResponse response;
	private InProcessFemtor inProcessFemtor;

	public FemtorExchange(InProcessFemtor inProcessFemtor, LackrBackendRequest spec) {
		super(inProcessFemtor, spec);
		this.request = new FemtorRequest(spec.getFrontendRequest().getRequest(), spec);
		this.response = new FemtorResponse(this);
		this.inProcessFemtor = inProcessFemtor;
	}

	@Override
	public List<String> getResponseHeaderValues(String name) {
		return response.getHeaders().get(name);
	}

	@Override
	public List<String> getResponseHeaderNames() {
		return new ArrayList<String>(response.getHeaders().keySet());
	}

	@Override
	public void addRequestHeader(String name, String value) {
		request.addHeader(name, value);
	}

	@Override
	public String getResponseHeader(String name) {
		return response.getHeaders().containsKey(name) ? response.getHeaders().get(name).get(0) : null;
	}

	@Override
	public byte[] getResponseBodyBytes() {
		return response.getContentBytes();
	}

	@Override
	public int getResponseStatus() {
		return response.getStatus();
	}

	@Override
	protected void doStart() throws Exception {
        inProcessFemtor.filter.doFilter(request, response, null);
        onComplete();
	}

	public FemtorResponse getResponse() {
		return response;
	}

    @Override
    public BaseGatewayMetrics getUpstream() throws NotAvailableException {
        return inProcessFemtor.getGateways()[0];
    }

}
