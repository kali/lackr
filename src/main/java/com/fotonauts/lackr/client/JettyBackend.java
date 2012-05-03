package com.fotonauts.lackr.client;

import java.io.PrintStream;

import org.eclipse.jetty.client.HttpClient;

import com.fotonauts.lackr.Backend;
import com.fotonauts.lackr.BackendRequest;
import com.fotonauts.lackr.HttpDirectorInterface;
import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.Gateway;

public class JettyBackend implements Backend {

	private HttpClient actualClient;
	
	private HttpDirectorInterface director;
		
	public void setActualClient(HttpClient actualClient) {
		this.actualClient = actualClient;
	}

	@Override
	public LackrBackendExchange createExchange(BackendRequest request) {
		return new JettyLackrBackendExchange(actualClient, director, request);
	}

	public void setDirector(HttpDirectorInterface director) {
	    this.director = director;
    }

	@Override
	public void stop() throws Exception {
		actualClient.stop();
	}
	
	@Override
    public void dumpStatus(PrintStream ps) {
		ps.format("Jetty HTTP Client\n");
		director.dumpStatus(ps);
    }

    @Override
    public Gateway[] getGateways() {
        return director.getGateways();
    }

}
