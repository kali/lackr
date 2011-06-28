package com.fotonauts.lackr;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

@SuppressWarnings("serial")
public class LackrPresentableError extends RuntimeException {

	public LackrPresentableError(String string) {
		super(string);
    }

    public LackrPresentableError(String string, LackrBackendExchange exchange) {
        super(string + " for fragment: " + exchange.getBackendRequest().getQuery());
    }

    public static LackrPresentableError fromThrowable(Throwable e) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        e.printStackTrace(ps);
        try {
            return new LackrPresentableError(baos.toString("UTF-8"));
        } catch (UnsupportedEncodingException e1) {
            // no way
            return null;
        }
    }
}
