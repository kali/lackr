package com.fotonauts.lackr.esi;

import java.util.List;

import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.MimeType;
import com.fotonauts.lackr.esi.filters.JsonQuotingChunk;
import com.fotonauts.lackr.interpolr.Chunk;

public class JSESIRule extends ESIIncludeRule {

	public JSESIRule() {
		super("\"ssi:include:virtual:*\"");
	}

	@Override
    public Chunk filterDocumentAsChunk(LackrBackendExchange exchange) {
		String mimeType = getMimeType(exchange);
		if (MimeType.isJS(mimeType))
			return exchange.getParsedDocument();
		else if (MimeType.isML(mimeType)) {
			if (exchange.getParsedDocument() == null || exchange.getParsedDocument().length() == 0)
				return NULL_CHUNK;
			else
				return new JsonQuotingChunk(exchange.getParsedDocument(), true);
		}
		return NULL_CHUNK;		
    }

	@Override
	public String getSyntaxIdentifier() {
		return "JS";
	}

	@Override
    public void check(LackrBackendExchange exchange, List<Throwable> exceptions) {
    }

}
