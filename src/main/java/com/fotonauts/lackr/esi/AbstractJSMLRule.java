package com.fotonauts.lackr.esi;

import com.fotonauts.lackr.BackendRequest;
import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.MimeType;
import com.fotonauts.lackr.esi.filters.JsonQuotingChunk;
import com.fotonauts.lackr.interpolr.Chunk;

public class AbstractJSMLRule extends ESIIncludeRule {

	public AbstractJSMLRule(String pattern) {
		super(pattern);
	}

	@Override
	public String getSyntaxIdentifier() {
		return "ML";
	}

	@Override
	public Chunk filterDocumentAsChunk(BackendRequest request) {
		if (request.getParsedDocument() == null || request.getParsedDocument().length() == 0)
			return NULL_CHUNK;
		return new JsonQuotingChunk(request.getParsedDocument(), false);
	}

	@Override
	public void check(BackendRequest request) {
		String mimeType = getMimeType(request.getExchange());
		if (MimeType.isJS(mimeType)) {
			request.getFrontendRequest().addBackendExceptions(
			        new LackrPresentableError("unsupported ESI type (js* in js(*ML) context", request.getExchange()));
		}
	}
}
