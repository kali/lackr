package com.fotonauts.lackr.interpolr.esi;

import com.fotonauts.lackr.MimeType;
import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.InterpolrScope;
import com.fotonauts.lackr.interpolr.esi.codec.JsonQuotingChunk;

public class AbstractJSESIRule extends ESIIncludeRule {

	public AbstractJSESIRule(String pattern) {
		super(pattern);
	}

	@Override
	public Chunk filterDocumentAsChunk(InterpolrScope scope) {
	    String mimeType = scope.getResultMimeType();
		if (MimeType.isJS(mimeType))
			return scope.getParsedDocument();
		else if (MimeType.isML(mimeType)) {
			if (scope.getParsedDocument() == null || scope.getParsedDocument().length() == 0)
				return NULL_CHUNK;
			else
				return new JsonQuotingChunk(scope.getParsedDocument(), true);
		} else if (MimeType.isTextPlain(mimeType)) {
			return new JsonQuotingChunk(scope.getParsedDocument(), true);
		}
		return NULL_CHUNK;
	}

	@Override
	public String getSyntaxIdentifier() {
		return "JS";
	}

	@Override
	public void check(InterpolrScope scope) {
		if(!MimeType.isJS(scope.getResultMimeType()))
			return;
		/* FIXME: disabled, as some ESI are actualy json string (for instance: '"disabled"')
		JsonParseUtils.parse(scope.getParsedDocument(), scope.getInterpolrContext(), "[esi fragment]");
		*/
	}

}
