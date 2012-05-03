package com.fotonauts.lackr;

import java.io.PrintStream;

import com.fotonauts.lackr.hashring.HashRing.NotAvailableException;

public class ConstantHttpDirector implements HttpDirectorInterface {

	final private String direction;
	private Gateway[] gateways;
	private HttpHost host;
	
	public ConstantHttpDirector(final String direction) {
		this.direction = direction;
		host = new HttpHost() {

            @Override
            public String getMBeanName() {
                return "constantDirector";
            }
            @Override
            public String getHostname() {
                return direction;
            }
        };
        
		this.gateways = new Gateway[] { host };
    }
	
	@Override
    public HttpHost getHostFor(BackendRequest request) throws NotAvailableException {
	    return host;
    }

	@Override
    public void dumpStatus(PrintStream ps) {
		ps.format("ConstantHttpDirector %s\n", direction);
    }

    @Override
    public Gateway[] getGateways() {
        return gateways;
    }
	

}
