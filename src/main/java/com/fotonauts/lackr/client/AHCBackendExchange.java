package com.fotonauts.lackr.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.util.FileCopyUtils;

import com.fotonauts.lackr.BackendRequest;
import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.LackrPresentableError;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Response;

public class AHCBackendExchange extends LackrBackendExchange {

	protected AsyncHttpClient client;
	
	protected Response response;
	
	protected Map<String, String> requestHeaders = new HashMap<String, String>();
	
	public AHCBackendExchange(AsyncHttpClient client, BackendRequest backendRequest) {
		super(backendRequest);
		this.client = client;
	}

	@Override
	public List<String> getResponseHeaderValues(String name) {
		return response.getHeaders(name);
	}

	@Override
	protected List<String> getResponseHeaderNames() {
		return new ArrayList<String>(response.getHeaders().keySet());
	}

	@Override
	public void addRequestHeader(String name, String value) {
		requestHeaders.put(name, value);
	}

	@Override
	protected String getResponseHeader(String name) {
		return response.getHeader(name);
	}

	@Override
	protected byte[] getResponseContentBytes() {
		try {
			return FileCopyUtils.copyToByteArray(response.getResponseBodyAsStream());
		} catch (IOException e) {
			// not likely, as request is supposed "completed"
			return null;
		}
	}

	@Override
	protected int getResponseStatus() {
		return response.getStatusCode();
	}

	@Override
	protected void doStart(String host) throws IOException {
		BoundRequestBuilder builder = client.prepareGet("/").setMethod(getBackendRequest().getMethod());
		builder.setUrl(host+getBackendRequest().getQuery());
		for(Entry<String, String> h: requestHeaders.entrySet())
			builder.addHeader(h.getKey(), h.getValue());
		if(getBackendRequest().getBody() != null)
			builder.setBody(new ByteArrayInputStream(getBackendRequest().getBody()));
		builder.setVirtualHost(getBackendRequest().getFrontendRequest().getRequest().getHeader("Host"));

		builder.execute(new AsyncCompletionHandler<Response>(){
			
	        @Override
	        public void onThrowable(Throwable t){
	        	getBackendRequest().getFrontendRequest().addBackendExceptions(LackrPresentableError.fromThrowable(t));
	        }

			@Override
			public Response onCompleted(Response daResponse) throws Exception {
				response = daResponse;
				onResponseComplete();
				return response;
			}
	    });
	}
}
