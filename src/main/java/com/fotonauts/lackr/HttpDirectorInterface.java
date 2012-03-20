package com.fotonauts.lackr;

import com.fotonauts.lackr.hashring.HashRing.NotAvailableException;

public interface HttpDirectorInterface {
	
	String getHostnameFor(BackendRequest request) throws NotAvailableException;
	
}
