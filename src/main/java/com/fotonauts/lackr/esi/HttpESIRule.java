package com.fotonauts.lackr.esi;

import java.util.List;

import com.fotonauts.lackr.LackrContentExchange;
import com.fotonauts.lackr.interpolr.Chunk;

public class HttpESIRule extends ESIIncludeRule {

	public HttpESIRule() {
		super("http://esi.include.virtual*#");
	}
	
	@Override
	public String getSyntaxIdentifier() {
		return "ML";
	}

	@Override
    public Chunk filterDocumentAsChunk(LackrContentExchange exchange) {
		return exchange.getParsedDocument();
    }

	@Override
    public void check(LackrContentExchange exchange, List<InterpolrException> exceptions) {
    }
	
}
