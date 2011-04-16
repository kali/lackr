package com.fotonauts.lackr.esi;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import com.fotonauts.lackr.LackrBackRequest;
import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.Rule.InterpolrException;

public class ExchangeChunk implements Chunk {

	private LackrBackRequest exchange;
	
	private ESIIncludeRule rule;
	
	public ExchangeChunk(LackrBackRequest exchange, ESIIncludeRule rule) {
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
	
	@Override
	public void check(List<InterpolrException> exceptions) {
		rule.check(exchange, exceptions);
	}

}
