package com.fotonauts.lackr.esi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;

import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.MimeType;
import com.fotonauts.lackr.esi.filters.JsonQuotingChunk;
import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.InterpolrScope;

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
        ObjectMapper mapper = scope.getInterpolr().getJacksonObjectMapper();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			baos.write("{ \"placeholder\" : ".getBytes());
			scope.getParsedDocument().writeTo(baos);
			baos.write("}".getBytes());
			mapper.readValue(baos.toByteArray(), Map.class);
		} catch (JsonParseException e) {
			StringBuilder builder = new StringBuilder();
			builder.append("JsonParseException: a fragment supposed to be a json value does not parse:\n");
			builder.append("url: " + scope.toString() + "\n");
			builder.append(e.getMessage() + "\n");
			builder.append("\n");
			try {
				builder.append(baos.toString("UTF-8"));
			} catch (UnsupportedEncodingException e2) {
				// no way
			} 
			builder.append("\n\n");
			scope.getInterpolrContext().addBackendExceptions(new LackrPresentableError(builder.toString()));
		} catch (IOException e) {
		    scope.getInterpolrContext().addBackendExceptions(LackrPresentableError.fromThrowable(e));
		}
	}

}
