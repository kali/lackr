package com.fotonauts.lackr.esi;

import java.util.List;

import com.fotonauts.lackr.LackrContentExchange;
import com.fotonauts.lackr.MimeType;
import com.fotonauts.lackr.interpolr.Chunk;

public class JSMLESIRule extends ESIIncludeRule {

	public JSMLESIRule() {
		super("<!--# include virtual=\\\"*\\\" -->");
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

	@Override
	public void check(LackrContentExchange exchange, List<InterpolrException> exceptions) {
		String mimeType = getMimeType(exchange);
		if(!MimeType.isML(mimeType)) {
			exceptions.add(new InterpolrException("unsupported ESI type (js* in js(*ML) context)", exchange));
		}
	}
}
