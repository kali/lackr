package com.fotonauts.lackr;

import java.io.PrintStream;

import com.fotonauts.lackr.backend.LackrBackendRequest;
import com.fotonauts.lackr.backend.hashring.HashRing.NotAvailableException;

public interface HttpDirectorInterface {
	
	HttpHost getHostFor(LackrBackendRequest request) throws NotAvailableException;

	void dumpStatus(PrintStream ps);

    BaseGatewayMetrics[] getGateways();

    String getName();

    void stop() throws InterruptedException;
	
}
