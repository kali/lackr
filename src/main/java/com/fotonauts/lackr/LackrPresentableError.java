package com.fotonauts.lackr;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import com.fotonauts.lackr.interpolr.InterpolrScope;

@SuppressWarnings("serial")
public class LackrPresentableError extends RuntimeException {

    public LackrPresentableError(String string) {
        super(string);
    }

    public LackrPresentableError(String string, LackrBackendExchange exchange) {
        super(string + " for fragment: " + exchange.getBackendRequest().getQuery());
    }

    public LackrPresentableError(String string, InterpolrScope sub) {
        super(string + " for fragment: " + sub.toString());
    }

    public static LackrPresentableError fromThrowable(Throwable e) {
        return fromThrowableAndMessage(e, null);
    }

    public static LackrPresentableError fromThrowableAndExchange(Throwable e, LackrBackendExchange exchange) {
        return fromThrowableAndMessage(e, exchange != null ? "Processing backend request " + exchange.getBackendRequest() : null);
    }

    public static LackrPresentableError fromThrowableAndMessage(Throwable e, String message) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        if (message != null) {
            ps.println(message);
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
