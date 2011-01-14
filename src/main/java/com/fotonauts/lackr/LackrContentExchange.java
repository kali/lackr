package com.fotonauts.lackr;

import java.io.IOException;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.io.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LackrContentExchange extends ContentExchange {
	
	static Logger log = LoggerFactory.getLogger(LackrContentExchange.class);

	protected LackrRequest lackrRequest;

	public LackrContentExchange(LackrRequest lackrRequest) {
		super(true);
		this.lackrRequest = lackrRequest;
	}

	@Override
	protected void onResponseComplete() throws IOException {
		super.onResponseComplete();
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
	protected synchronized void onResponseHeader(Buffer name, Buffer value)
			throws IOException {
		super.onResponseHeader(name, value);
	}
}
