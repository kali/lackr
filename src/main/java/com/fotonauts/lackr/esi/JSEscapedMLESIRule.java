package com.fotonauts.lackr.esi;

import java.util.List;

import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.MimeType;
import com.fotonauts.lackr.interpolr.Chunk;

public class JSEscapedMLESIRule extends ESIIncludeRule {

	public JSEscapedMLESIRule() {
		super("\\u003C!--# include virtual=\\\"*\\\" --\\u003E");
	}

	@Override
	public String getSyntaxIdentifier() {
		return "JS";
	}

	@Override
	public Chunk filterDocumentAsChunk(LackrBackendExchange exchange) {
		String mimeType = getMimeType(exchange);
		if (MimeType.isML(mimeType)) {
			if (exchange.getParsedDocument() == null || exchange.getParsedDocument().length() == 0)
				return NULL_CHUNK;
			return new JsonQuotingChunk(exchange.getParsedDocument(), false);
		}
		throw new RuntimeException("unsupported ESI type (js* in js(*ML) context) for " + exchange.getBackendRequest().getQuery());
	}

	@Override
    public void check(LackrBackendExchange exchange, List<InterpolrException> exceptions) {
		String mimeType = getMimeType(exchange);
		if (MimeType.isJS(mimeType))
			exceptions.add(new InterpolrException("unsupported ESI type (js* in js(*ML) context)", exchange));
    }

}
