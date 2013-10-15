package com.fotonauts.lackr;

import java.io.PrintStream;

import com.fotonauts.lackr.backend.LackrBackendRequest;
import com.fotonauts.lackr.backend.hashring.HashRing.NotAvailableException;

public class ConstantHttpDirector implements HttpDirectorInterface {

	final private String direction;
	private BaseGatewayMetrics[] gateways;
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
        host.start();
		this.gateways = new BaseGatewayMetrics[] { host };
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
    public BaseGatewayMetrics[] getGateways() {
        return gateways;
    }

    @Override
    public String getName() {
        return direction.replaceAll("[.:]","_");
    }
    
    @Override
    public void stop() {
    }

}
