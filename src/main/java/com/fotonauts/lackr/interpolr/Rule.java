package com.fotonauts.lackr.interpolr;

import java.util.List;

import com.fotonauts.lackr.LackrBackendExchange;

public interface Rule {
	
	@SuppressWarnings("serial")
    public class InterpolrException extends Exception {
		public InterpolrException(String message) {
			super(message);
        }

		public InterpolrException(String message, LackrBackendExchange exchange) {
			super(message + " (for fragment: " + exchange.getBackendRequest().getQuery() + " )");
        }
	}
	
	List<Chunk> parse(DataChunk chunk, Object context);
	
}
