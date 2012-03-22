package com.fotonauts.lackr.femtor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import com.fotonauts.lackr.BackendRequest;
import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.hashring.HashRing.NotAvailableException;

public class FemtorExchange extends LackrBackendExchange {

	private FemtorRequest request;
	private FemtorResponse response;
	private InProcessFemtor inProcessFemtor;

	public FemtorExchange(InProcessFemtor inProcessFemtor, BackendRequest spec) {
		super(spec);
		this.request = new FemtorRequest(spec, inProcessFemtor.holder);
		this.response = new FemtorResponse(this);
		this.inProcessFemtor = inProcessFemtor;
	}

	@Override
	public List<String> getResponseHeaderValues(String name) {
		return response.getHeaders().get(name);
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected List<String> getResponseHeaderNames() {
		return new ArrayList(response.getHeaders().keySet());
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
			inProcessFemtor.servletContextHandler.handle(getBackendRequest().getPath(), request, request, response);
			rawResponseContent = response.getContentBytes();
	        postProcess();
        } catch (ServletException e) {
        	getBackendRequest().getFrontendRequest().addBackendExceptions(new LackrPresentableError("error in femtor call: " + e.getMessage()));
        }
	}

	public FemtorResponse getResponse() {
		return response;
	}
	
}
