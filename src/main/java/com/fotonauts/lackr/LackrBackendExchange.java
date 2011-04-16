package com.fotonauts.lackr;

import static com.fotonauts.lackr.MongoLoggingKeys.DATE;
import static com.fotonauts.lackr.MongoLoggingKeys.ELAPSED;
import static com.fotonauts.lackr.MongoLoggingKeys.HTTP_HOST;
import static com.fotonauts.lackr.MongoLoggingKeys.METHOD;
import static com.fotonauts.lackr.MongoLoggingKeys.PARENT;
import static com.fotonauts.lackr.MongoLoggingKeys.PATH;
import static com.fotonauts.lackr.MongoLoggingKeys.QUERY_PARMS;
import static com.fotonauts.lackr.MongoLoggingKeys.SIZE;
import static com.fotonauts.lackr.MongoLoggingKeys.STATUS;

import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;

import org.eclipse.jetty.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.client.JettyLackrBackendExchange;
import com.fotonauts.lackr.hashring.HashRing.NotAvailableException;
import com.fotonauts.lackr.interpolr.DataChunk;
import com.fotonauts.lackr.interpolr.Document;
import com.mongodb.BasicDBObject;

public abstract class LackrBackendExchange {

	static Logger log = LoggerFactory.getLogger(JettyLackrBackendExchange.class);

	public abstract Enumeration<String> getResponseHeaderValues(String name);

	protected abstract Enumeration<String> getResponseHeaderNames();

	public abstract void addRequestHeader(String name, String value);

	protected abstract String getResponseHeader(String name);

	protected abstract byte[] getResponseContentBytes();

	protected abstract int getResponseStatus();

	protected BasicDBObject logLine;
	protected long startTimestamp;
	private byte[] rawResponseContent;
	private Document parsedDocument;
	protected BackendRequest backendRequest;

	public BackendRequest getBackendRequest() {
		return backendRequest;
	}

	public LackrBackendExchange(BackendRequest spec) {
		this.backendRequest = spec;
	}

	protected void start() throws IOException, NotAvailableException {		
		addRequestHeader("X-NGINX-SSI", "yes");
		addRequestHeader("X-SSI-ROOT", backendRequest.getFrontendRequest().getRequest()
				.getRequestURI());
		addRequestHeader("X-FTN-NORM-USER-AGENT", backendRequest.getFrontendRequest()
				.getUserAgent().toString());
		addRequestHeader("X-FTN-INLINE-IMAGES", backendRequest.getFrontendRequest()
				.getUserAgent().supportsInlineImages() ? "yes" : "no");
		if (backendRequest.getParent() != null)
			addRequestHeader("X-SSI-PARENT", backendRequest.getParent());
		if (backendRequest.getSyntax() != null)
			addRequestHeader("X-SSI-INCLUDE-SYNTAX", backendRequest.getSyntax());

		for (@SuppressWarnings("unchecked")
		Enumeration e = backendRequest.getFrontendRequest().getRequest().getHeaderNames(); e
				.hasMoreElements();) {
			String header = (String) e.nextElement();
			if (!LackrFrontendRequest.skipHeader(header)) {
				addRequestHeader(header, backendRequest.getFrontendRequest().getRequest()
						.getHeader(header));
			}
		}
		startTimestamp = System.currentTimeMillis();
		logLine = Service.standardLogLine(backendRequest.getFrontendRequest()
				.getRequest(), "lackr-back");
		logLine.put(HTTP_HOST.getPrettyName(), getRequestField("Host"));
		logLine.put(METHOD.getPrettyName(), backendRequest.getMethod());
		logLine.put(PATH.getPrettyName(), backendRequest.getPath());
		if (backendRequest.getParent() != null) {
			logLine.put(PARENT.getPrettyName(), backendRequest.getParent());
		}
		if (backendRequest.getParams() != null)
			logLine.put(QUERY_PARMS.getPrettyName(), backendRequest.getParams());
		doStart(backendRequest.getFrontendRequest().getService().getRing()
				.getHostFor(backendRequest.getQuery()).getHostname());
	}

	public void onResponseComplete() {
		rawResponseContent = getResponseContentBytes();
		long endTimestamp = System.currentTimeMillis();
		logLine.put(STATUS.getPrettyName(), getResponseStatus());
		final LackrBackendExchange exchange = this;
		if (rawResponseContent != null)
			logLine.put(SIZE.getPrettyName(), rawResponseContent.length);
		logLine.put(DATE.getPrettyName(), new Date().getTime());
		logLine.put(ELAPSED.getPrettyName(),
				0.001 * (endTimestamp - startTimestamp));
		backendRequest.getFrontendRequest().getService().log(logLine);
		backendRequest.getFrontendRequest().getService().getExecutor()
				.execute(new Runnable() {

					@Override
					public void run() {
						exchange.postProcess();
					}
				});

	}
	
	protected abstract String getRequestField(String name);
	
	protected abstract void doStart(String host) throws IOException;

	protected void postProcess() {
		if (this != backendRequest.getFrontendRequest().rootExchange
				&& (getResponseStatus() / 100 == 4 || getResponseStatus() / 100 == 5)
				&& getResponseHeader("X-SSI-AWARE") != null)
			backendRequest.getFrontendRequest().addBackendExceptions(
					new Exception("Fragment " + getBackendRequest().getQuery()
							+ " returned code " + getResponseStatus()));

		try {
			if (rawResponseContent != null && rawResponseContent.length > 0) {
				String mimeType = getResponseHeader(HttpHeaders.CONTENT_TYPE);
				if (MimeType.isML(mimeType) || MimeType.isJS(mimeType))
					parsedDocument = backendRequest.getFrontendRequest().getService()
							.getInterpolr().parse(rawResponseContent, this);
				else
					parsedDocument = new Document(new DataChunk(
							rawResponseContent));
			} else
				parsedDocument = new Document(new DataChunk(new byte[0]));
		} catch (Throwable e) {
			e.printStackTrace();
			backendRequest.getFrontendRequest().addBackendExceptions(e);
		}
		backendRequest.getFrontendRequest().notifySubRequestDone();
	}

	public Document getParsedDocument() {
		return parsedDocument;
	}

	public String getResponseHeaderValue(String name) {
		Enumeration<String> values = getResponseHeaderValues(name);
		if (values.hasMoreElements())
			return values.nextElement();
		else
			return null;
	}

}