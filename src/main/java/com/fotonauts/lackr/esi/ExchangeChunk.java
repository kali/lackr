package com.fotonauts.lackr.esi;

import java.io.IOException;
import java.io.OutputStream;

import com.fotonauts.lackr.LackrContentExchange;
import com.fotonauts.lackr.interpolr.Chunk;

public class ExchangeChunk implements Chunk {

	private LackrContentExchange exchange;
	
	private ESIIncludeRule rule;
	
	public ExchangeChunk(LackrContentExchange exchange, ESIIncludeRule rule) {
		this.exchange = exchange;
		this.rule = rule;
    }
	
	@Override
    public int length() {
		return rule.filterDocumentAsChunk(exchange).length();
    }

	@Override
    public String toDebugString() {
	    return "{{{" + rule.getClass().getSimpleName() + ":" + exchange.getURI() + "}}}";
    }

	@Override
    public void writeTo(OutputStream stream) throws IOException {
		rule.filterDocumentAsChunk(exchange).writeTo(stream);
    }

}
