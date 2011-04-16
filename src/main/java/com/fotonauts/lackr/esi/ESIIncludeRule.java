package com.fotonauts.lackr.esi;

import java.io.UnsupportedEncodingException;
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

	@Override
	public Chunk substitute(byte[] buffer, int start, int stop, Object context) {
		LackrBackendExchange exchange = (LackrBackendExchange) context;
		String url = null;
		try {
			url = new String(buffer, start, stop - start, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// nope, thank you
		}
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
