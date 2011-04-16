package com.fotonauts.lackr.esi;

import java.util.List;

import com.fotonauts.lackr.LackrBackendExchange;
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
    public Chunk filterDocumentAsChunk(LackrBackendExchange exchange) {
		return exchange.getParsedDocument();
    }

	@Override
    public void check(LackrBackendExchange exchange, List<InterpolrException> exceptions) {
    }
	
}
