package com.fotonauts.lackr;

import java.io.PrintStream;

import com.fotonauts.lackr.hashring.HashRing.NotAvailableException;

public interface HttpDirectorInterface {
	
	HttpHost getHostFor(BackendRequest request) throws NotAvailableException;

	void dumpStatus(PrintStream ps);

    Gateway[] getGateways();

    String getName();

    void stop() throws InterruptedException;
	
}
