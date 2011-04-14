package com.fotonauts.lackr.interpolr;

import java.util.List;

import com.fotonauts.lackr.LackrContentExchange;

public interface Rule {
	
	@SuppressWarnings("serial")
    public class InterpolrException extends Exception {
		public InterpolrException(String message) {
			super(message);
        }

		public InterpolrException(String message, LackrContentExchange exchange) {
			super(message + " (for fragment: " + exchange.getURI() + " )");
        }
	}
	
	List<Chunk> parse(DataChunk chunk, Object context);
	
}
