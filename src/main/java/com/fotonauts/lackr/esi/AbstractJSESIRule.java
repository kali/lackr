package com.fotonauts.lackr.esi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;

import com.fotonauts.lackr.BackendRequest;
import com.fotonauts.lackr.BackendRequest.Target;
import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.MimeType;
import com.fotonauts.lackr.esi.filters.JsonQuotingChunk;
import com.fotonauts.lackr.interpolr.Chunk;

public class AbstractJSESIRule extends ESIIncludeRule {

	private Target target;

	public AbstractJSESIRule(String pattern, BackendRequest.Target target) {
		super(pattern);
		this.target = target;
	}

	@Override
	protected BackendRequest.Target getTarget() {
		return target;
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
		} else if (MimeType.isTextPlain(mimeType)) {
			return new JsonQuotingChunk(exchange.getParsedDocument(), true);
		}
		return NULL_CHUNK;
	}

	@Override
	public String getSyntaxIdentifier() {
		return "JS";
	}

	@Override
	public void check(LackrBackendExchange exchange) {
		// FIXME this is here to find a bug. it is probably unecessary as it
		// will be parsed later
		if(!MimeType.isJS(exchange.getResponseHeaderValue("Content-Type")))
			return;
		ObjectMapper mapper = exchange.getBackendRequest().getFrontendRequest().getService().getJacksonObjectMapper();
		@SuppressWarnings("unchecked")
		Map data = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			baos.write("{ \"placeholder\" : ".getBytes());
			exchange.getParsedDocument().writeTo(baos);
			baos.write("}".getBytes());
			data = mapper.readValue(baos.toByteArray(), Map.class);
		} catch (JsonParseException e) {
			StringBuilder builder = new StringBuilder();
			builder.append("JsonParseException: a fragment supposed to be a json value does not parse:\n");
			builder.append("url: " + exchange.getBackendRequest().getQuery() + "\n");
			builder.append(e.getMessage() + "\n");
			builder.append("\n");
			try {
				builder.append(baos.toString("UTF-8"));
			} catch (UnsupportedEncodingException e2) {
				// no way
			} 
			builder.append("\n\n");
			exchange.getBackendRequest().getFrontendRequest().addBackendExceptions(new LackrPresentableError(builder.toString()));
		} catch (IOException e) {
			exchange.getBackendRequest().getFrontendRequest().addBackendExceptions(LackrPresentableError.fromThrowable(e));
		}
	}

}
