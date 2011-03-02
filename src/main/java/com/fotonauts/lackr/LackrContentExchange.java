package com.fotonauts.lackr;

import static com.fotonauts.lackr.MongoLoggingKeys.DATE;
import static com.fotonauts.lackr.MongoLoggingKeys.ELAPSED;
import static com.fotonauts.lackr.MongoLoggingKeys.HTTP_HOST;
import static com.fotonauts.lackr.MongoLoggingKeys.METHOD;
import static com.fotonauts.lackr.MongoLoggingKeys.PATH;
import static com.fotonauts.lackr.MongoLoggingKeys.QUERY_PARMS;
import static com.fotonauts.lackr.MongoLoggingKeys.SIZE;
import static com.fotonauts.lackr.MongoLoggingKeys.STATUS;

import java.io.IOException;
import java.util.Date;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.io.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;

public class LackrContentExchange extends ContentExchange {

	static Logger log = LoggerFactory.getLogger(LackrContentExchange.class);

	protected LackrRequest lackrRequest;
	protected BasicDBObject logLine;

	private long startTimestamp;

	public LackrContentExchange(LackrRequest lackrRequest) {
		super(true);
		this.lackrRequest = lackrRequest;
	}

	protected void start() throws IOException {
		String path = getURI().indexOf('?') == -1 ? getURI() : getURI().substring(0, getURI().indexOf('?'));
		String query = getURI().indexOf('?') == -1 ? null : getURI().substring(getURI().indexOf('?') + 1);
		logLine = Service.standardLogLine(lackrRequest.getRequest(), "lackr-back");
		logLine.put(HTTP_HOST.getPrettyName(), getAddress().toString());
		logLine.put(METHOD.getPrettyName(), getMethod());
		logLine.put(PATH.getPrettyName(), path);
		if (query != null)
			logLine.put(QUERY_PARMS.getPrettyName(), query);
		startTimestamp = System.currentTimeMillis();
		lackrRequest.getService().getClient().send(this);
	}

	@Override
	protected void onResponseComplete() throws IOException {
		super.onResponseComplete();
		long endTimestamp = System.currentTimeMillis();
		logLine.put(STATUS.getPrettyName(), getResponseStatus());
		if (getResponseContentBytes() != null)
			logLine.put(SIZE.getPrettyName(), getResponseContentBytes().length);
		logLine.put(DATE.getPrettyName(), new Date().getTime());
		logLine.put(ELAPSED.getPrettyName(), (endTimestamp - startTimestamp) / 1000);
		lackrRequest.getService().logCollection.save(logLine);
		log.debug(getURI() + " => " + getResponseStatus());
		lackrRequest.processIncomingResponse(this);
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
}
