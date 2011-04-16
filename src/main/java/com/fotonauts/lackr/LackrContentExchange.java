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

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.hashring.HashRing.NotAvailableException;
import com.fotonauts.lackr.hashring.Host;
import com.fotonauts.lackr.interpolr.DataChunk;
import com.fotonauts.lackr.interpolr.Document;
import com.mongodb.BasicDBObject;

public class LackrContentExchange {

	static class JettyContentExchange extends ContentExchange {

		LackrContentExchange exchange;

		public JettyContentExchange(LackrContentExchange exchange)
				throws NotAvailableException {
			super(true);
			setMethod(exchange.getSpec().getMethod());
			setURL(exchange.getHost().getHostname()
					+ exchange.getSpec().getQuery());
			this.exchange = exchange;

			if (exchange.getSpec().getBody() != null) {
				setRequestContent(new ByteArrayBuffer(exchange.getSpec()
						.getBody()));
				setRequestHeader("Content-Length",
						Integer.toString(exchange.getSpec().getBody().length));
			}

		}

		@Override
		protected void onResponseComplete() throws IOException {
			super.onResponseComplete();
			exchange.onResponseComplete();
		}

		@Override
		protected void onConnectionFailed(Throwable x) {
			super.onConnectionFailed(x);
			exchange.getSpec().getFrontendRequest().addBackendExceptions(x);
		}

		@Override
		protected void onException(Throwable x) {
			super.onException(x);
			exchange.getSpec().getFrontendRequest().addBackendExceptions(x);
		}

		@Override
		protected synchronized void onResponseHeader(Buffer name, Buffer value)
				throws IOException {
			super.onResponseHeader(name, value);
		}

	}

	static Logger log = LoggerFactory.getLogger(LackrContentExchange.class);

	protected BasicDBObject logLine;

	private long startTimestamp;

	private byte[] rawResponseContent;

	private Document parsedDocument;

	private ContentExchange jettyContentExchange;

	public BackendRequest getSpec() {
		return spec;
	}

	public Host getHost() {
		return host;
	}

	private BackendRequest spec;

	private Host host;

	public LackrContentExchange(BackendRequest spec)
			throws NotAvailableException {
		this.spec = spec;
		this.host = spec.getFrontendRequest().getService().getRing()
				.getHostFor(spec.getQuery());

		jettyContentExchange = new JettyContentExchange(this);

		addRequestHeader("X-NGINX-SSI", "yes");
		addRequestHeader("X-SSI-ROOT", spec.getFrontendRequest().getRequest()
				.getRequestURI());
		addRequestHeader("X-FTN-NORM-USER-AGENT", spec.getFrontendRequest()
				.getUserAgent().toString());
		addRequestHeader("X-FTN-INLINE-IMAGES", spec.getFrontendRequest()
				.getUserAgent().supportsInlineImages() ? "yes" : "no");
		if (spec.getParent() != null)
			jettyContentExchange.addRequestHeader("X-SSI-PARENT",
					spec.getParent());
		if (spec.getSyntax() != null)
			jettyContentExchange.addRequestHeader("X-SSI-INCLUDE-SYNTAX",
					spec.getSyntax());

		for (@SuppressWarnings("unchecked")
		Enumeration e = spec.getFrontendRequest().getRequest().getHeaderNames(); e
				.hasMoreElements();) {
			String header = (String) e.nextElement();
			if (!LackrFrontendRequest.skipHeader(header)) {
				addRequestHeader(header, spec.getFrontendRequest().getRequest()
						.getHeader(header));
			}
		}
	}

	public void onResponseComplete() {
		rawResponseContent = getResponseContentBytes();
		long endTimestamp = System.currentTimeMillis();
		logLine.put(STATUS.getPrettyName(), getResponseStatus());
		final LackrContentExchange exchange = this;
		if (rawResponseContent != null)
			logLine.put(SIZE.getPrettyName(), rawResponseContent.length);
		logLine.put(DATE.getPrettyName(), new Date().getTime());
		logLine.put(ELAPSED.getPrettyName(),
				0.001 * (endTimestamp - startTimestamp));
		spec.getFrontendRequest().getService().log(logLine);
		spec.getFrontendRequest().getService().getExecutor()
				.execute(new Runnable() {

					@Override
					public void run() {
						exchange.postProcess();
					}
				});

	}

	int getResponseStatus() {
		return jettyContentExchange.getResponseStatus();
	}

	private byte[] getResponseContentBytes() {
		return jettyContentExchange.getResponseContentBytes();
	}

	private String getResponseHeader(String name) {
		return jettyContentExchange.getResponseFields().getStringField(name);
	}

	protected void postProcess() {
		if (this != spec.getFrontendRequest().rootExchange
				&& (getResponseStatus() / 100 == 4 || getResponseStatus() / 100 == 5)
				&& getResponseHeader("X-SSI-AWARE") != null)
			spec.getFrontendRequest().addBackendExceptions(
					new Exception("Fragment " + getURI() + " returned code "
							+ getResponseStatus()));

		try {
			if (rawResponseContent != null && rawResponseContent.length > 0) {
				String mimeType = getResponseHeader(HttpHeaders.CONTENT_TYPE);
				if (MimeType.isML(mimeType) || MimeType.isJS(mimeType))
					parsedDocument = spec.getFrontendRequest().getService()
							.getInterpolr().parse(rawResponseContent, this);
				else
					parsedDocument = new Document(new DataChunk(
							rawResponseContent));
			} else
				parsedDocument = new Document(new DataChunk(new byte[0]));
		} catch (Throwable e) {
			e.printStackTrace();
			spec.getFrontendRequest().addBackendExceptions(e);
		}
		spec.getFrontendRequest().notifySubRequestDone();
	}

	private String getURI() {
		return jettyContentExchange.getURI();
	}

	protected void start() throws IOException {
		String path = jettyContentExchange.getURI().indexOf('?') == -1 ? jettyContentExchange
				.getURI() : jettyContentExchange.getURI().substring(0,
				jettyContentExchange.getURI().indexOf('?'));
		String query = jettyContentExchange.getURI().indexOf('?') == -1 ? null
				: jettyContentExchange.getURI().substring(
						jettyContentExchange.getURI().indexOf('?') + 1);
		startTimestamp = System.currentTimeMillis();
		logLine = Service.standardLogLine(spec.getFrontendRequest()
				.getRequest(), "lackr-back");
		logLine.put(HTTP_HOST.getPrettyName(), jettyContentExchange
				.getRequestFields().getStringField("Host"));
		logLine.put(METHOD.getPrettyName(), spec.getMethod());
		logLine.put(PATH.getPrettyName(), spec.getPath());
		if (spec.getParent() != null) {
			logLine.put(PARENT.getPrettyName(), spec.getParent());
		}
		if (query != null)
			logLine.put(QUERY_PARMS.getPrettyName(), query);
		spec.getFrontendRequest().getService().getClient()
				.send(jettyContentExchange);
	}

	public Document getParsedDocument() {
		return parsedDocument;
	}

	public void addRequestHeader(String name, String value) {
		jettyContentExchange.addRequestHeader(name, value);
	}
	
	Enumeration<String> getResponseHeaderNames() {
		return jettyContentExchange.getResponseFields().getFieldNames();
	}

	public Enumeration<String> getResponseHeaderValues(String name) {
		return jettyContentExchange.getResponseFields().getValues(name);
	}

	public String getResponseHeaderValue(String name) {
		Enumeration<String> values = getResponseHeaderValues(name);
		if(values.hasMoreElements())
			return values.nextElement();
		else
			return null;
	}
}
