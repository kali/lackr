package com.fotonauts.lackr;

import java.io.PrintStream;

public interface Backend {

	public LackrBackendExchange createExchange(BackendRequest request);

	public void dumpStatus(PrintStream ps);
	
	public void stop() throws Exception;
	
	public Gateway[] getUpstreamServices();
}
