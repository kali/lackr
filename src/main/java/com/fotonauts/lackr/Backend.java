package com.fotonauts.lackr;

import java.io.PrintStream;

import com.fotonauts.lackr.hashring.HashRing.NotAvailableException;

public interface Backend {

	public LackrBackendExchange createExchange(BackendRequest request) throws NotAvailableException;

	public void dumpStatus(PrintStream ps);
	
	public void stop() throws Exception;
	
	public Gateway[] getGateways();
	
	public String getName();
}
