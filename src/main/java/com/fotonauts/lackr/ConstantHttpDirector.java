package com.fotonauts.lackr;

import java.io.PrintStream;

import com.fotonauts.lackr.hashring.HashRing.NotAvailableException;

public class ConstantHttpDirector implements HttpDirectorInterface {

	private String direction;
	
	public ConstantHttpDirector(String direction) {
		this.direction = direction;
    }
	
	@Override
    public String getHostnameFor(BackendRequest request) throws NotAvailableException {
	    return direction;
    }

	@Override
    public void dumpStatus(PrintStream ps) {
		ps.format("ConstantHttpDirector %s\n", direction);
    }

}
