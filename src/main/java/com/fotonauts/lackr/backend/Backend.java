package com.fotonauts.lackr.backend;

import java.io.PrintStream;

import com.fotonauts.lackr.BackendRequest;
import com.fotonauts.lackr.Gateway;
import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.Service;
import com.fotonauts.lackr.backend.hashring.HashRing.NotAvailableException;

/**
 * Represents a backend server or any other upstream system lackr will try to delegate a {@link BackendRequest} too. 
 *  
 * @author kali
 *
 */
public interface Backend {

    /**
     * The "interesting" method of the backend.
     * 
     * @param request the request specification to process
     * @return a {@link LackrBackendExchange} materialising the transaction with the Backend.
     * @throws NotAvailableException if the Backend is not available.
     */
	public LackrBackendExchange createExchange(BackendRequest request) throws NotAvailableException;

	/**
	 * Dumps current state in a {@link PrintStream} or monitoring and/or debugging.
	 * @param ps the {@link PrintStream} to dump to.
	 */
	public void dumpStatus(PrintStream ps);
	
	/**
	 * Will be called by {@link Service} at its end of life.
	 * 
	 * @throws Exception
	 */
	public void stop() throws Exception;
	
	/**
	 * One or several statistics accumulator representing one or several upstream servers.
	 * 
	 * @return the gateways
	 */
	public Gateway[] getGateways();
	
	/**
	 * For debugging purposes.
	 * 
	 * @return
	 */
	public String getName();
}
