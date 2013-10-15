package com.fotonauts.lackr;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import com.fotonauts.lackr.backend.LackrBackendExchange;

@SuppressWarnings("serial")
public class LackrPresentableError extends RuntimeException {

	public LackrPresentableError(String string) {
		super(string);
    }

    public LackrPresentableError(String string, LackrBackendExchange exchange) {
        super(string + " for fragment: " + exchange.getBackendRequest().getQuery());
    }

    public static LackrPresentableError fromThrowable(Throwable e) {
    	return fromThrowableAndExchange(e, null);
    }

    public static LackrPresentableError fromThrowableAndExchange(Throwable e, LackrBackendExchange exchange) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        if(exchange != null) {
        	ps.println("While requesting backend for " + exchange.getBackendRequest().getPath());
        }
        e.printStackTrace(ps);
        ps.println();
        try {
            return new LackrPresentableError(baos.toString("UTF-8"));
        } catch (UnsupportedEncodingException e1) {
            // no way
            return null;
        }
    }
}
