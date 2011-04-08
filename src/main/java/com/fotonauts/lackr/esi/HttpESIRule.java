package com.fotonauts.lackr.esi;

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
	
}
