package com.fotonauts.lackr.client;

import org.apache.http.impl.nio.client.DefaultHttpAsyncClient;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.nio.reactor.IOReactorException;

import com.fotonauts.lackr.BackendClient;
import com.fotonauts.lackr.BackendRequest;
import com.fotonauts.lackr.LackrBackendExchange;

public class ApacheClient implements BackendClient {

	protected final HttpAsyncClient client; 
	
	public ApacheClient() throws IOReactorException {
		client = new DefaultHttpAsyncClient();
		client.start();
    }
	
	@Override
    public LackrBackendExchange createExchange(BackendRequest request) {
		return new ApacheBackendExchange(client, request);
    }

}
