package com.fotonauts.lackr.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.nio.concurrent.FutureCallback;

import com.fotonauts.lackr.BackendRequest;
import com.fotonauts.lackr.LackrBackendExchange;

public class ApacheBackendExchange extends LackrBackendExchange {

	private final HttpAsyncClient client;
	protected Map<String, String> requestHeaders = new HashMap<String, String>();
	protected HttpResponse response;

	public ApacheBackendExchange(HttpAsyncClient client, BackendRequest request) {
		super(request);
		this.client = client;
	}

	@Override
	public void addRequestHeader(String name, String value) {
		requestHeaders.put(name, value);
	}

	@Override
	protected void doStart(String host) throws IOException {
		String uri = host + getBackendRequest().getQuery();
		HttpUriRequest request;
		if (getBackendRequest().getMethod().equals("DELETE")) {
			request = new HttpDelete(uri);
		} else if (getBackendRequest().getMethod().equals("POST")) {
			request = new HttpPost(uri);
		} else if (getBackendRequest().getMethod().equals("PUT")) {
			request = new HttpPut(uri);
		} else {
			request = new HttpGet(uri);
		}
		if (request instanceof HttpEntityEnclosingRequestBase && getBackendRequest().getBody() != null)
			((HttpEntityEnclosingRequestBase) request).setEntity(new ByteArrayEntity(getBackendRequest().getBody()));
		for (Map.Entry<String, String> h : requestHeaders.entrySet()) {
			request.setHeader(h.getKey(), h.getValue());
		}
		client.execute(request, new FutureCallback<HttpResponse>() {

			@Override
			public void failed(Exception t) {
				getBackendRequest().getFrontendRequest().addBackendExceptions(t);
			}

			@Override
			public void completed(HttpResponse daResponse) {
				response = daResponse;
				onResponseComplete();
			}

			@Override
			public void cancelled() {
			}
		});
	}

	byte[] cachedContent = null;

	@Override
	protected synchronized byte[] getResponseContentBytes() {
		if (cachedContent == null) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				response.getEntity().writeTo(baos);
			} catch (IOException e) {
				getBackendRequest().getFrontendRequest().addBackendExceptions(e);
			}
			cachedContent = baos.toByteArray();
		}
		return cachedContent;
	}

	@Override
	protected String getResponseHeader(String name) {
		return response.getLastHeader(name).getValue();
	}

	@Override
	protected List<String> getResponseHeaderNames() {
		List<String> result = new ArrayList<String>(response.getAllHeaders().length);
		for (Header h : response.getAllHeaders())
			result.add(h.getName());
		return result;
	}

	@Override
	public List<String> getResponseHeaderValues(String name) {
		List<String> result = new ArrayList<String>(response.getAllHeaders().length);
		for (Header h : response.getHeaders(name))
			result.add(h.getName());
		return result;
	}

	@Override
	protected int getResponseStatus() {
		return response.getStatusLine().getStatusCode();
	}

}
