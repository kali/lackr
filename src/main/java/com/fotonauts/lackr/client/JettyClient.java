package com.fotonauts.lackr.client;

import org.eclipse.jetty.client.HttpClient;

import com.fotonauts.lackr.BackendClient;
import com.fotonauts.lackr.BackendRequest;
import com.fotonauts.lackr.LackrBackendExchange;

public class JettyClient implements BackendClient {

	private HttpClient actualClient;
	
	public void setActualClient(HttpClient actualClient) {
		this.actualClient = actualClient;
	}

	@Override
	public LackrBackendExchange createExchange(BackendRequest request) {
		return new JettyLackrBackendExchange(actualClient, request);
	}

}
