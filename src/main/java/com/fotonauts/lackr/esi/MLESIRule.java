package com.fotonauts.lackr.esi;

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
		throw new RuntimeException("unsupported ESI type (js* in *ML context)");
	}
}
