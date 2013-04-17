package com.fotonauts.lackr.client;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.BackendRequest;
import com.fotonauts.lackr.Gateway;
import com.fotonauts.lackr.HttpDirectorInterface;
import com.fotonauts.lackr.HttpHost;
import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.hashring.HashRing.NotAvailableException;

public class JettyLackrBackendExchange extends LackrBackendExchange {

    static Logger log = LoggerFactory.getLogger(JettyLackrBackendExchange.class);

//	ContentExchange jettyContentExchange;
	private HttpDirectorInterface director;
	private HttpHost upstream;
    private Request request;
    protected Result result;
    private byte[] responseBody; 

	@Override
	public Gateway getUpstream() throws NotAvailableException {
	    if(upstream == null)
	        upstream = director.getHostFor(getBackendRequest());
	    return upstream;
	}
	
	public JettyLackrBackendExchange(HttpClient jettyClient, HttpDirectorInterface director, BackendRequest spec) throws NotAvailableException {
		super(spec);
		this.director = director;
        request = jettyClient.newRequest(director.getHostFor(spec).getHostname() + getBackendRequest().getQuery());
        request.method(HttpMethod.fromString(spec.getMethod()));
        if(spec.getBody() != null) {
            request.header(HttpHeader.CONTENT_TYPE.asString(), spec.getFrontendRequest().getRequest().getHeader("Content-Type"));
            request.content(new BytesContentProvider(spec.getBody()));
        }
	}

	@Override
	protected int getResponseStatus() {
	    return result.getResponse().getStatus();
	}

	@Override
	protected byte[] getResponseContentBytes() {
	    return responseBody;
	}

	@Override
	protected String getResponseHeader(String name) {
	    return result.getResponse().getHeaders().getStringField(name);
	}

	@Override
	public void addRequestHeader(String name, String value) {
	    request.getHeaders().add(name, value);
	}

	@Override
	protected List<String> getResponseHeaderNames() {
		return Collections.list(result.getResponse().getHeaders().getFieldNames());
	}

	@Override
	public List<String> getResponseHeaderValues(String name) {
		return Collections.list(result.getResponse().getHeaders().getValues(name));
	}

	@Override
	protected void doStart() throws IOException, NotAvailableException {
		final JettyLackrBackendExchange lackrExchange = this;
		request.send(new BufferingResponseListener(20*1024*1024) {
            
            @Override
            public void onComplete(Result r) {
                result = r;
                responseBody = getContent();
                lackrExchange.onResponseComplete(false);
            }

            @Override
            public void onFailure(Response arg0, Throwable x) {
                lackrExchange.getBackendRequest().getFrontendRequest().addBackendExceptions(x);
            }
        });
	}

}
