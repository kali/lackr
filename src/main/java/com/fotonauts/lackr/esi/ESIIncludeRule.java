package com.fotonauts.lackr.esi;

import org.eclipse.jetty.http.HttpHeaders;

import com.fotonauts.lackr.BackendRequest;
import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.LackrFrontendRequest;
import com.fotonauts.lackr.hashring.HashRing.NotAvailableException;
import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.ConstantChunk;
import com.fotonauts.lackr.interpolr.MarkupDetectingRule;
import com.fotonauts.lackr.interpolr.Rule;

abstract public class ESIIncludeRule extends MarkupDetectingRule implements
		Rule {

	protected static ConstantChunk NULL_CHUNK = new ConstantChunk(
			"null".getBytes());

	public ESIIncludeRule(String markup) {
		super(markup);
	}
	
	protected BackendRequest.Target getTarget() {
		return BackendRequest.Target.PICOR;
	}

	protected String getMimeType(LackrBackendExchange exchange) {
		return exchange.getResponseHeaderValue(HttpHeaders.CONTENT_TYPE);
	}

	protected String makeUrl(byte[] buffer, int start, int stop) {
		StringBuilder builder = new StringBuilder();
		for(int i = start; i<stop; i++) {
			byte b = buffer[i];
			if(b < 0) {
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
	public Chunk substitute(byte[] buffer, int[] boundPairs, Object context) {
		LackrBackendExchange exchange = (LackrBackendExchange) context;
		String url = makeUrl(buffer, boundPairs[0], boundPairs[1]);
		LackrBackendExchange sub;
		try {
			LackrFrontendRequest front = exchange.getBackendRequest()
					.getFrontendRequest();
			sub = front.getSubBackendExchange(getTarget(), url, getSyntaxIdentifier(), exchange);
		} catch (NotAvailableException e) {
			throw new RuntimeException("no backend available for fragment: "
					+ exchange.getBackendRequest().getQuery());
		}
		return new ExchangeChunk(sub, this);
	}

	public abstract String getSyntaxIdentifier();

	public abstract Chunk filterDocumentAsChunk(LackrBackendExchange exchange);

	public abstract void check(LackrBackendExchange exchange);
}
