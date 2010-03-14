package com.fotonauts.lackr;

import java.io.IOException;

import org.eclipse.jetty.client.ContentExchange;

public class LackrContentExchange extends ContentExchange {
    
    protected LackrRequest lackrRequest;
    
    public LackrContentExchange(LackrRequest lackrRequest) {
        super(true);
        this.lackrRequest = lackrRequest;
    }
    
    @Override
    protected void onResponseComplete() throws IOException {
        
        lackrRequest.processIncomingResponse(this);
    }
}
