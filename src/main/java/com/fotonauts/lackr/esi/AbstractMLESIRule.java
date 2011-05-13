package com.fotonauts.lackr.esi;

import java.util.List;

import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.MimeType;
import com.fotonauts.lackr.esi.filters.AmpersandEscapeChunk;
import com.fotonauts.lackr.interpolr.Chunk;

public abstract class AbstractMLESIRule extends ESIIncludeRule {

	public AbstractMLESIRule(String markup) {
	    super(markup);
    }

	@Override
	public String getSyntaxIdentifier() {
		return "ML";
	}
	
	@Override
	public Chunk filterDocumentAsChunk(LackrBackendExchange exchange) {
		String mimeType = getMimeType(exchange);
		if(MimeType.isML(mimeType))
			return exchange.getParsedDocument();
		else 
			return new AmpersandEscapeChunk(exchange.getParsedDocument());
	}

	@Override
    public void check(LackrBackendExchange exchange, List<Throwable> exceptions) {
		String mimeType = getMimeType(exchange);
		if(MimeType.isJS(mimeType))
			exceptions.add(new InterpolrException("unsupported ESI type (js* in *ML context)", exchange));
    }
}
