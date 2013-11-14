package com.fotonauts.lackr.backend;

import java.io.PrintStream;

import com.fotonauts.lackr.LackrBackendRequest;
import com.fotonauts.lackr.backend.hashring.HashRing.NotAvailableException;

public interface HttpDirectorInterface {
	
	HttpHost getHostFor(LackrBackendRequest request) throws NotAvailableException;

	void dumpStatus(PrintStream ps);

    String getName();

    void stop() throws InterruptedException;
	
}
