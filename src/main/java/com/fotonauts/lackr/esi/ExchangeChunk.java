package com.fotonauts.lackr.esi;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.interpolr.Chunk;

public class ExchangeChunk implements Chunk {

	private LackrBackendExchange exchange;
	
	private ESIIncludeRule rule;
	
	public ExchangeChunk(LackrBackendExchange exchange, ESIIncludeRule rule) {
		this.exchange = exchange;
		this.rule = rule;
    }
	
	@Override
    public int length() {
		return rule.filterDocumentAsChunk(exchange).length();
    }

	@Override
    public String toDebugString() {
	    return "{{{" + rule.getClass().getSimpleName() + ":" + exchange.getBackendRequest().getQuery() + "}}}";
    }

	@Override
    public void writeTo(OutputStream stream) throws IOException {
		rule.filterDocumentAsChunk(exchange).writeTo(stream);
    }
	
	@Override
	public void check(List<Throwable> exceptions) {
		rule.check(exchange, exceptions);
	}

}
