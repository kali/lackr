package com.fotonauts.lackr;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jetty.http.HttpHeaders;

import com.fotonauts.lackr.hashring.HashRing.NotAvailableException;
import com.fotonauts.lackr.interpolr.DataChunk;
import com.fotonauts.lackr.interpolr.Document;

public class BackendRequest {

	private final byte[] body;
	private Document parsedDocument;

	private final String method;
	private final String parent;
	private final int parentId;
	private final String query;
	private final LackrFrontendRequest frontendRequest;

	private AtomicReference<LackrBackendExchange> lastExchange = new AtomicReference<LackrBackendExchange>();

	private final String syntax;

	private AtomicInteger triedBackend = new AtomicInteger(0);

	public BackendRequest(LackrFrontendRequest frontendRequest, String method, String query, String parent, int parentId,
	        String syntax, byte[] body) {
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
		LackrBackendExchange exchange = getExchange();
		try {
			System.err.format("backend %d returned %d\n", triedBackend.get(), exchange.getResponseStatus());

			if (exchange.getResponseStatus() == 501) {
				if(triedBackend.incrementAndGet() < getFrontendRequest().getService().getBackends().length) {
					tryNext();
					return;
				}
			}

			if (this != getFrontendRequest().getRootRequest()
			        && (exchange.getResponseStatus() / 100 == 4 || exchange.getResponseStatus() / 100 == 5)
			        && exchange.getResponseHeader("X-SSI-AWARE") == null)
				getFrontendRequest().addBackendExceptions(
				        new LackrPresentableError("Fragment " + getQuery() + " returned code " + exchange.getResponseStatus()));
			if (exchange.getRawResponseContent() != null && exchange.getRawResponseContent().length > 0) {
				String mimeType = exchange.getResponseHeader(HttpHeaders.CONTENT_TYPE);
				if (MimeType.isML(mimeType) || MimeType.isJS(mimeType))
					parsedDocument = getFrontendRequest().getService().getInterpolr().parse(exchange.getRawResponseContent(), this);
				else
					parsedDocument = new Document(new DataChunk(exchange.getRawResponseContent()));
			} else
				parsedDocument = new Document(new DataChunk(new byte[0]));

		} catch (Throwable e) {
			e.printStackTrace();
			getFrontendRequest().addBackendExceptions(LackrPresentableError.fromThrowable(e));
		}

		getFrontendRequest().notifySubRequestDone();
	}

	public Document getParsedDocument() {
		return parsedDocument;
    }

}
