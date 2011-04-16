package com.fotonauts.lackr.esi;

import java.util.List;

import com.fotonauts.lackr.LackrContentExchange;
import com.fotonauts.lackr.MimeType;
import com.fotonauts.lackr.interpolr.Chunk;

public class MLESIRule extends ESIIncludeRule {

	public MLESIRule() {
		super("<!--# include virtual=\"*\" -->");
	}

	@Override
	public String getSyntaxIdentifier() {
		return "ML";
	}
	
	@Override
	public Chunk filterDocumentAsChunk(LackrContentExchange exchange) {
		String mimeType = getMimeType(exchange);
		if(MimeType.isML(mimeType))
			return exchange.getParsedDocument();
		throw new RuntimeException("unsupported ESI type (js* in *ML context) " + exchange.getURI());
	}

	@Override
    public void check(LackrContentExchange exchange, List<InterpolrException> exceptions) {
		String mimeType = getMimeType(exchange);
		if(MimeType.isJS(mimeType))
			exceptions.add(new InterpolrException("unsupported ESI type (js* in *ML context)", exchange));
    }
}
