package com.fotonauts.lackr.esi;

import java.util.List;

import com.fotonauts.lackr.LackrBackRequest;
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
    public Chunk filterDocumentAsChunk(LackrBackRequest exchange) {
		return exchange.getParsedDocument();
    }

	@Override
    public void check(LackrBackRequest exchange, List<InterpolrException> exceptions) {
    }
	
}
