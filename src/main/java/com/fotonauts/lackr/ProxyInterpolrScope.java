package com.fotonauts.lackr;

import org.eclipse.jetty.http.HttpHeader;

import com.fotonauts.lackr.backend.LackrBackendRequest;
import com.fotonauts.lackr.interpolr.Document;
import com.fotonauts.lackr.interpolr.Interpolr;
import com.fotonauts.lackr.interpolr.InterpolrContext;
import com.fotonauts.lackr.interpolr.InterpolrScope;

public class ProxyInterpolrScope implements InterpolrScope {

    private InterpolrFrontendRequest context;
    private Document parsedDocument;
    private LackrBackendRequest request;

    public ProxyInterpolrScope(InterpolrFrontendRequest context) {
        super();
        this.context = context;
    }

    @Override
    public InterpolrContext getInterpolrContext() {
        return context;
    }

    @Override
    public Interpolr getInterpolr() {
        return context.getInterpolr();
    }

    @Override
    public Document getParsedDocument() {
        return parsedDocument;
    }

    @Override
    public void setParsedDocument(Document result) {
        parsedDocument = result;
    }

    @Override
    public String getResultMimeType() {
        return request.getExchange().getResponse().getResponseHeaderValue(HttpHeader.CONTENT_TYPE.asString());
    }

    @Override
    public byte[] getBodyBytes() {
        return request.getExchange().getResponse().getBodyBytes();
    }

    public LackrBackendRequest getRequest() {
        return request;
    }

    public void setRequest(LackrBackendRequest request) {
        this.request = request;
    }
    
    @Override
    public String toString() {
        return request.toString();
    }

}
