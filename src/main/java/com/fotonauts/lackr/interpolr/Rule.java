package com.fotonauts.lackr.interpolr;

import java.util.List;

import com.fotonauts.lackr.LackrBackRequest;

public interface Rule {
	
	@SuppressWarnings("serial")
    public class InterpolrException extends Exception {
		public InterpolrException(String message) {
			super(message);
        }

		public InterpolrException(String message, LackrBackRequest exchange) {
			super(message + " (for fragment: " + exchange.getURI() + " )");
        }
	}
	
	List<Chunk> parse(DataChunk chunk, Object context);
	
}
