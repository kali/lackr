package com.fotonauts.lackr;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.fotonauts.lackr.hashring.HashRing.NotAvailableException;

public class BackendRequest {

	private final byte[] body;

	private final String method;
	private final String parent;
	private final int parentId;
	private final String query;
	private final LackrFrontendRequest frontendRequest;

	private AtomicReference<LackrBackendExchange> lastExchange = new AtomicReference<LackrBackendExchange>();
	
	private final String syntax;

	private AtomicInteger triedBackend = new AtomicInteger(0);
	
	public BackendRequest(LackrFrontendRequest frontendRequest, String method,
			String query, String parent, int parentId, String syntax,
			byte[] body) {
		super();
		this.frontendRequest = frontendRequest;
		this.method = method;
		this.query = query;
		this.parent = parent;
		this.parentId = parentId;
		this.syntax = syntax;
		this.body = body;
	}

	public byte[] getBody() {
		return body;
	}

	public String getMethod() {
		return method;
	}

	public String getParent() {
		return parent;
	}

	public int getParentId() {
		return parentId;
	}

	public String getQuery() {
		return query;
	}

	public LackrFrontendRequest getFrontendRequest() {
		return frontendRequest;
	}

	public String getSyntax() {
		return syntax;
	}

	public String getPath() {
		return query.indexOf('?') == -1 ? query : query.substring(0, query.indexOf('?'));
	}

	public String getParams() {
		return query.indexOf('?') == -1 ? null : query.substring(query.indexOf('?') + 1);
	}

	public void start() throws IOException, NotAvailableException {
		tryNext();
    }
	
	protected void tryNext() throws IOException, NotAvailableException {
		int next = triedBackend.get();
		LackrBackendExchange exchange = getFrontendRequest().getService().getBackends()[next].createExchange(this);
		lastExchange.set(exchange);
		exchange.start();
	}
	public LackrBackendExchange getExchange() {
		return lastExchange.get();
    }

	public void postProcess() {
/*		LackrBackendExchange exchange = getExchange();
		if(exchange.getResponseStatus() == 404) {
			
		} else { */
			getFrontendRequest().notifySubRequestDone();
//		}
    }

}
