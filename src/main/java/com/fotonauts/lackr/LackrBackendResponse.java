package com.fotonauts.lackr;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LackrBackendResponse {

    static Logger log = LoggerFactory.getLogger(LackrBackendResponse.class);

    public abstract List<String> getHeaderValues(String name);

    public abstract List<String> getHeaderNames();

    public abstract String getHeader(String name);

    public abstract byte[] getBodyBytes();

    public abstract int getStatus();

    protected LackrBackendExchange exchange;

    public LackrBackendResponse(LackrBackendExchange exchange) {
        this.exchange = exchange;
    }

    public String getResponseHeaderValue(String name) {
        List<String> values = null;
        try {
            values = getHeaderValues(name);
        } catch (NullPointerException exception) {
            return null;
        }
        if (values == null || values.isEmpty())
            return null;
        else
            return values.get(0);
    }

    @Override
    public String toString() {
        return String.format("Response:%s", exchange);
    }
}
