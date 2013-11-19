package com.fotonauts.lackr.backend.client;

import java.io.PrintStream;

import com.fotonauts.lackr.LackrBackendRequest;
import com.fotonauts.lackr.backend.HttpHost;
import com.fotonauts.lackr.backend.hashring.HashRing.NotAvailableException;

public class ConstantHttpDirector implements HttpDirectorInterface {

	final private String direction;
	private HttpHost host;
	
	public ConstantHttpDirector(final String direction) throws Exception {
		this.direction = direction;
		host = new HttpHost() {

            @Override
            public String getHostname() {
                return direction;
            }
        };
        host.start();
    }
	
	@Override
    public HttpHost getHostFor(LackrBackendRequest request) throws NotAvailableException {
	    return host;
    }

	@Override
    public void dumpStatus(PrintStream ps) {
		ps.format("ConstantHttpDirector %s\n", direction);
    }

    @Override
    public String getName() {
        return direction.replaceAll("[.:]","_");
    }
    
    @Override
    public void stop() {
    }

}
