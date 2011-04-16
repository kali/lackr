package com.fotonauts.lackr.esi;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.HttpMethods;

import com.fotonauts.lackr.LackrBackRequest;
import com.fotonauts.lackr.hashring.HashRing.NotAvailableException;
import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.ConstantChunk;
import com.fotonauts.lackr.interpolr.MarkupDetectingRule;
import com.fotonauts.lackr.interpolr.Rule;

abstract public class ESIIncludeRule extends MarkupDetectingRule implements Rule {

	protected static ConstantChunk NULL_CHUNK = new ConstantChunk("null".getBytes());
	
	public ESIIncludeRule(String markup) {
		super(markup);
    }

	protected String getMimeType(LackrBackRequest exchange) {
		return exchange.getResponseFields().getStringField(HttpHeaders.CONTENT_TYPE);
	}

	@Override
    public Chunk substitute(byte[] buffer, int start, int stop, Object context) {
		LackrBackRequest exchange = (LackrBackRequest) context;
		String url = null ;
        try {
	        url = new String(buffer, start, stop - start, "UTF-8");
        } catch (UnsupportedEncodingException e) {
        	// nope, thank you
        }
        LackrBackRequest sub;
        try {
	        sub = exchange.getLackrRequest().scheduleUpstreamRequest(url, HttpMethods.GET, null, exchange.getURI(), getSyntaxIdentifier());
        } catch (NotAvailableException e) {
        	throw new RuntimeException("no backend available for fragment: " + exchange.getURI());
        }
        return new ExchangeChunk(sub, this);        
    }

	public abstract String getSyntaxIdentifier();

	public abstract Chunk filterDocumentAsChunk(LackrBackRequest exchange);

	public abstract void check(LackrBackRequest exchange, List<InterpolrException> exceptions);
	}
