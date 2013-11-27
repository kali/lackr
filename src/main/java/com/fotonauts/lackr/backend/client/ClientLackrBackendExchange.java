package com.fotonauts.lackr.backend.client;

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

import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.LackrBackendRequest;
import com.fotonauts.lackr.LackrBackendResponse;
import com.fotonauts.lackr.backend.hashring.HashRingBackend.NotAvailableException;

public class ClientLackrBackendExchange extends LackrBackendExchange {

    static Logger log = LoggerFactory.getLogger(ClientLackrBackendExchange.class);

    private Request request;
    protected Result result;
    private LackrBackendResponse response;
    private byte[] responseBody;

    public ClientLackrBackendExchange(ClientBackend backend, HttpClient jettyClient, String prefix,
            LackrBackendRequest spec) throws NotAvailableException {
        super(backend, spec);
        String url = prefix + getBackendRequest().getQuery();
        request = jettyClient.newRequest(url);
        request.method(HttpMethod.fromString(spec.getMethod()));
        request.getHeaders().add(spec.getFields());
        if (spec.getBody() != null) {
            request.header(HttpHeader.CONTENT_TYPE.asString(), spec.getFrontendRequest().getIncomingServletRequest().getHeader("Content-Type"));
            request.content(new BytesContentProvider(spec.getBody()));
        }
        log.debug("Created {}", this);
    }

    @Override
    public LackrBackendResponse getResponse() {
        return response;
    }

    public class ResponseAdapter extends LackrBackendResponse {

        public ResponseAdapter(LackrBackendExchange exchange) {
            super(exchange);
        }

        @Override
        public int getStatus() {
            return result.getResponse().getStatus();
        }

        @Override
        public byte[] getBodyBytes() {
            return responseBody;
        }

        @Override
        public String getHeader(String name) {
            return result.getResponse().getHeaders().getStringField(name);
        }

        @Override
        public List<String> getHeaderNames() {
            return Collections.list(result.getResponse().getHeaders().getFieldNames());
        }

        @Override
        public List<String> getHeaderValues(String name) {
            return Collections.list(result.getResponse().getHeaders().getValues(name));
        }

    }

    @Override
    protected void doStart() {
        final ClientLackrBackendExchange lackrExchange = this;
        request.send(new BufferingResponseListener(100 * 1024 * 1024) {

            @Override
            public void onComplete(Result r) {
                if (r.isSucceeded()) {
                    lackrExchange.result = r;
                    lackrExchange.responseBody = getContent();
                    response = new ResponseAdapter(lackrExchange);
                    lackrExchange.onComplete();
                } else {
                    lackrExchange.getBackendRequest().getFrontendRequest().addBackendExceptions(r.getFailure());
                }
            }

            @Override
            public void onFailure(Response arg0, Throwable x) {
                lackrExchange.getBackendRequest().getFrontendRequest().addBackendExceptions(x);
            }
        });
    }

}
