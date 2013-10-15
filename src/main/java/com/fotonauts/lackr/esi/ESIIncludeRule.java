package com.fotonauts.lackr.esi;

import org.eclipse.jetty.http.HttpHeader;

import com.fotonauts.lackr.LackrFrontendRequest;
import com.fotonauts.lackr.backend.LackrBackendExchange;
import com.fotonauts.lackr.backend.LackrBackendRequest;
import com.fotonauts.lackr.backend.hashring.HashRing.NotAvailableException;
import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.ConstantChunk;
import com.fotonauts.lackr.interpolr.MarkupDetectingRule;
import com.fotonauts.lackr.interpolr.Rule;

abstract public class ESIIncludeRule extends MarkupDetectingRule implements Rule {

	protected static ConstantChunk NULL_CHUNK = new ConstantChunk("null".getBytes());

	public ESIIncludeRule(String markup) {
		super(markup);
	}

	protected String getMimeType(LackrBackendExchange exchange) {
		return exchange.getResponseHeaderValue(HttpHeader.CONTENT_TYPE.asString());
	}

	protected String makeUrl(byte[] buffer, int start, int stop) {
		StringBuilder builder = new StringBuilder();
		for (int i = start; i < stop; i++) {
			byte b = buffer[i];
			if (b < 0) {
				builder.append('%');
				builder.append(Integer.toHexString(b + 256).toUpperCase());
			} else if (b < 32) {
				builder.append('%');
				builder.append(Integer.toHexString(b).toUpperCase());
			} else {
				builder.append((char) b);
			}
		}
		return builder.toString();
	}

	@Override
	public Chunk substitute(byte[] buffer, int start, int[] boundPairs, int stop, Object context) {
		LackrBackendRequest request = (LackrBackendRequest) context;
		String url = makeUrl(buffer, boundPairs[0], boundPairs[1]);
		LackrBackendRequest sub;
		try {
			LackrFrontendRequest front = request.getFrontendRequest();
			sub = front.getSubBackendExchange(url, getSyntaxIdentifier(), request);
		} catch (NotAvailableException e) {
			throw new RuntimeException("no backend available for fragment: " + request.getQuery());
		}
		return new RequestChunk(sub, this);
	}

	public abstract String getSyntaxIdentifier();

	public abstract Chunk filterDocumentAsChunk(LackrBackendRequest exchange);

	public abstract void check(LackrBackendRequest request);
}
