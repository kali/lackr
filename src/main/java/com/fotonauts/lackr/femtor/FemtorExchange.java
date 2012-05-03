package com.fotonauts.lackr.femtor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fotonauts.lackr.BackendRequest;
import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.UpstreamService;
import com.fotonauts.lackr.hashring.HashRing.NotAvailableException;

public class FemtorExchange extends LackrBackendExchange {

	private FemtorRequest request;
	private FemtorResponse response;
	private InProcessFemtor inProcessFemtor;

	public FemtorExchange(InProcessFemtor inProcessFemtor, BackendRequest spec) {
		super(spec);
		this.request = new FemtorRequest(spec.getFrontendRequest().getRequest(), spec);
		this.response = new FemtorResponse(this);
		this.inProcessFemtor = inProcessFemtor;
	}

	@Override
	public List<String> getResponseHeaderValues(String name) {
		return response.getHeaders().get(name);
	}

	@Override
	protected List<String> getResponseHeaderNames() {
		return new ArrayList<String>(response.getHeaders().keySet());
	}

	@Override
	public void addRequestHeader(String name, String value) {
		request.addHeader(name, value);
	}

	@Override
	protected String getResponseHeader(String name) {
		return response.getHeaders().containsKey(name) ? response.getHeaders().get(name).get(0) : null;
	}

	@Override
	protected byte[] getResponseContentBytes() {
		return response.getContentBytes();
	}

	@Override
	protected int getResponseStatus() {
		return response.getStatus();
	}

	@Override
	protected void doStart() throws IOException, NotAvailableException {
		try {
			inProcessFemtor.filter.doFilter(request, response, null);
		} catch (Exception e) {
			response.setStatus(500);
			getBackendRequest().getFrontendRequest().addBackendExceptions(e);
		} finally {
            onResponseComplete(true);
		}
	}

	public FemtorResponse getResponse() {
		return response;
	}

}
