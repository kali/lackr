package com.fotonauts.lackr.client;

import com.fotonauts.lackr.BackendClient;
import com.fotonauts.lackr.BackendRequest;
import com.fotonauts.lackr.LackrBackendExchange;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

public class AHCClient implements BackendClient {
	
	private AsyncHttpClient actualClient;
	
	public AHCClient() {
		AsyncHttpClientConfig cf = new AsyncHttpClientConfig.Builder()
			.setIdleConnectionInPoolTimeoutInMs(60000)
			.setMaximumConnectionsPerHost(100)
			.setMaximumConnectionsTotal(1000)
			.setUserAgent(null).build();
		actualClient = new AsyncHttpClient(cf);
	}
	
	@Override
	public LackrBackendExchange createExchange(BackendRequest request) {
		return new AHCBackendExchange(actualClient, request);
	}

}
