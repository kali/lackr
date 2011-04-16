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

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.io.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.interpolr.DataChunk;
import com.fotonauts.lackr.interpolr.Document;
import com.mongodb.BasicDBObject;

public class LackrContentExchange extends ContentExchange {

	static Logger log = LoggerFactory.getLogger(LackrContentExchange.class);

	protected LackrFrontendRequest lackrRequest;
	protected BasicDBObject logLine;

	private long startTimestamp;

	private byte[] rawResponseContent;

	private Document parsedDocument;

	public LackrContentExchange(LackrFrontendRequest lackrRequest) {
		super(true);
		this.lackrRequest = lackrRequest;
	}

	protected void start(String parent) throws IOException {
		String path = getURI().indexOf('?') == -1 ? getURI() : getURI().substring(0, getURI().indexOf('?'));
		String query = getURI().indexOf('?') == -1 ? null : getURI().substring(getURI().indexOf('?') + 1);
		startTimestamp = System.currentTimeMillis();
		logLine = Service.standardLogLine(lackrRequest.getRequest(), "lackr-back");
		logLine.put(HTTP_HOST.getPrettyName(), getRequestFields().getStringField("Host"));
		logLine.put(METHOD.getPrettyName(), getMethod());
		logLine.put(PATH.getPrettyName(), path);
		if(parent != null) {
			logLine.put(PARENT.getPrettyName(), parent);
		}
		if (query != null)
			logLine.put(QUERY_PARMS.getPrettyName(), query);
		lackrRequest.getService().getClient().send(this);
	}

	@Override
	protected void onResponseComplete() throws IOException {
		super.onResponseComplete();
		rawResponseContent = getResponseContentBytes();
		long endTimestamp = System.currentTimeMillis();
		logLine.put(STATUS.getPrettyName(), getResponseStatus());
		final LackrContentExchange exchange = this;
		if (rawResponseContent != null)
			logLine.put(SIZE.getPrettyName(), rawResponseContent.length);
		logLine.put(DATE.getPrettyName(), new Date().getTime());
		logLine.put(ELAPSED.getPrettyName(), 0.001 * (endTimestamp - startTimestamp));
		lackrRequest.getService().log(logLine);
		lackrRequest.getService().getExecutor().execute(new Runnable() {

			@Override
			public void run() {
				exchange.postProcess();
			}
		});
	}

	protected void postProcess() {
		if (this != lackrRequest.rootExchange && (getResponseStatus() / 100 == 4 || getResponseStatus() / 100 == 5)
		        && !getResponseFields().containsKey("X-SSI-AWARE"))
			lackrRequest.addBackendExceptions(new Exception("Fragment " + getURI() + " returned code "
			        + getResponseStatus()));

		try {
			if (rawResponseContent != null && rawResponseContent.length > 0) {
				String mimeType = getResponseFields().getStringField(HttpHeaders.CONTENT_TYPE);
				if (MimeType.isML(mimeType) || MimeType.isJS(mimeType))
					parsedDocument = lackrRequest.getService().getInterpolr().parse(rawResponseContent, this);
				else
					parsedDocument = new Document(new DataChunk(rawResponseContent));
			} else
				parsedDocument = new Document(new DataChunk(new byte[0]));
		} catch (Throwable e) {
			e.printStackTrace();
			lackrRequest.addBackendExceptions(e);
		}
		lackrRequest.notifySubRequestDone();
	}

	@Override
	protected void onConnectionFailed(Throwable x) {
		super.onConnectionFailed(x);
		lackrRequest.addBackendExceptions(x);
	}

	@Override
	protected void onException(Throwable x) {
		super.onException(x);
		lackrRequest.addBackendExceptions(x);
	}

	@Override
	protected synchronized void onResponseHeader(Buffer name, Buffer value) throws IOException {
		super.onResponseHeader(name, value);
	}

	public Document getParsedDocument() {
		return parsedDocument;
	}

	public LackrFrontendRequest getLackrRequest() {
		return lackrRequest;
	}
}
