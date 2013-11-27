package com.fotonauts.lackr.interpolr.esi;

import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.MimeType;
import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.InterpolrScope;
import com.fotonauts.lackr.interpolr.esi.codec.JsonQuotingChunk;

public class AbstractJSMLRule extends ESIIncludeRule {

	public AbstractJSMLRule(String pattern) {
		super(pattern);
	}

	@Override
	public String getSyntaxIdentifier() {
		return "ML";
	}

	@Override
	public Chunk filterDocumentAsChunk(InterpolrScope scope) {
		if (scope.getParsedDocument() == null || scope.getParsedDocument().length() == 0)
			return NULL_CHUNK;
		return new JsonQuotingChunk(scope.getParsedDocument(), false);
	}

	@Override
	public void check(InterpolrScope scope) {
		String mimeType = scope.getResultMimeType();
		if (MimeType.isJS(mimeType)) {
			scope.getInterpolrContext().addError(
			        new LackrPresentableError("unsupported ESI type (js* in js(*ML) context", scope));
		}
	}
}
