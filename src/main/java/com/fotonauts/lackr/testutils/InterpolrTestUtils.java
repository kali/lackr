package com.fotonauts.lackr.testutils;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.fotonauts.lackr.MimeType;
import com.fotonauts.lackr.interpolr.Document;
import com.fotonauts.lackr.interpolr.Interpolr;
import com.fotonauts.lackr.interpolr.InterpolrContext;
import com.fotonauts.lackr.interpolr.InterpolrScope;

public class InterpolrTestUtils {

    public static InterpolrContext parseToContext(Interpolr inter, String data, String mimeType, AppStubForESI app) {

        if (app == null)
            app = new AppStubForESI();
        app.pageContent.set(data);
        app.pageMimeType.set(mimeType);

        final AppStubForESI finalApp = app;
        final InterpolrContextStub context = new InterpolrContextStub(inter) {
            @Override
            public InterpolrScope getOrCreateSubScope(String url, String syntaxIdentifier, InterpolrScope scope) {
                InterpolrScope s = finalApp.getInterpolrScope(this, syntaxIdentifier, url);
                s.getInterpolr().processResult(s);
                return s;
            }
        };

        InterpolrScope scope = context.getOrCreateSubScope("/page.html", "ML", null);
        context.setRootScope(scope);
        inter.processResult(scope);
        inter.preflightCheck(context);
        if (context.getErrors().size() > 0) {
            throw context.getErrors().get(0);
        }
        return context;
    }

    public static InterpolrContext parseToContext(Interpolr inter, String data) {
        return parseToContext(inter, data, MimeType.TEXT_HTML, null);
    }

    public static Document parseToDocument(Interpolr inter, String data, String mimeType) {
        InterpolrContext context = parseToContext(inter, data, mimeType, null);
        if (context == null || context.getErrors().size() > 0)
            return null;
        return context.getRootScope().getParsedDocument();
    }

    public static Document parse(Interpolr inter, String data) {
        return parseToDocument(inter, data, MimeType.TEXT_HTML);
    }

    public static String expand(Document chunks) {
        try {
            int length = chunks.length();
            ByteArrayOutputStream baos = new ByteArrayOutputStream(length);
            chunks.writeTo(baos);
            byte[] bytes = baos.toByteArray();
            assertEquals("result length computation is fine", length, bytes.length);
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // no way
        } catch (IOException e) {
            // now way here
        }
        return null;
    }

    public static String expand(Interpolr interpolr, String page, String mimeType) {
        return expand(parseToDocument(interpolr, page, mimeType));
    }

    public static String expand(Interpolr interpolr, String page) {
        return expand(interpolr, page, MimeType.TEXT_HTML);
    }

}
