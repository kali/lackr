package com.fotonauts.lackr;

import java.io.PrintStream;

import com.fotonauts.lackr.hashring.HashRing.NotAvailableException;

public interface HttpDirectorInterface {
	
	String getHostnameFor(BackendRequest request) throws NotAvailableException;

	void dumpStatus(PrintStream ps);

    UpstreamService[] getUpstreamServices();
	
}
