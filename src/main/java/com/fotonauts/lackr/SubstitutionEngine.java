package com.fotonauts.lackr;

import java.io.IOException;

public interface SubstitutionEngine {

	public static class IncludeException extends Exception {

        public IncludeException(String string) {
        	super(string);
        }

		private static final long serialVersionUID = -1206277582043579749L;
		
	}	

	public byte[] generateContent(LackrFrontendRequest rootRequest, byte[] byteContent) throws IncludeException;

	public void scheduleSubQueries(LackrContentExchange lackrContentExchange, LackrFrontendRequest lackrRequest) throws IOException;
}
