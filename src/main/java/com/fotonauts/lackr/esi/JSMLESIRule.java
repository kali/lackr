package com.fotonauts.lackr.esi;

import com.fotonauts.lackr.LackrContentExchange;
import com.fotonauts.lackr.MimeType;
import com.fotonauts.lackr.interpolr.Chunk;

public class JSMLESIRule extends ESIIncludeRule {

	public JSMLESIRule() {
		super("\\u003C!--# include virtual=\\\"*\\\" --\\u003E");
	}

	@Override
	public String getSyntaxIdentifier() {
		return "JS";
	}

	@Override
	public Chunk filterDocumentAsChunk(LackrContentExchange exchange) {
		String mimeType = getMimeType(exchange);
		if (MimeType.isML(mimeType)) {
			if (exchange.getParsedDocument() == null || exchange.getParsedDocument().length() == 0)
				return NULL_CHUNK;
			return new JsonQuotingChunk(exchange.getParsedDocument(), false);
		}
		throw new RuntimeException("unsupported ESI type (js* in js(*ML) context)");
	}

}
