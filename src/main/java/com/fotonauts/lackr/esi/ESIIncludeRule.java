package com.fotonauts.lackr.esi;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

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
	public Chunk substitute(byte[] buffer, int start, int stop, Object context) {
		LackrBackendExchange exchange = (LackrBackendExchange) context;
		String url = makeUrl(buffer, start, stop);
		LackrBackendExchange sub;
		try {
			LackrFrontendRequest front = exchange.getBackendRequest()
					.getFrontendRequest();
			BackendRequest esi = new BackendRequest(front, "GET", url, exchange
					.getBackendRequest().getQuery(), exchange.getBackendRequest().hashCode(),
					getSyntaxIdentifier(), null);
			sub = front.scheduleUpstreamRequest(esi);
		} catch (NotAvailableException e) {
			throw new RuntimeException("no backend available for fragment: "
					+ exchange.getBackendRequest().getQuery());
		}
		return new ExchangeChunk(sub, this);
	}

	public abstract String getSyntaxIdentifier();

	public abstract Chunk filterDocumentAsChunk(LackrBackendExchange exchange);

	public abstract void check(LackrBackendExchange exchange,
			List<InterpolrException> exceptions);
}
