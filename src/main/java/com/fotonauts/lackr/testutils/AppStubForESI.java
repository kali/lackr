package com.fotonauts.lackr.testutils;

import java.util.concurrent.atomic.AtomicReference;

import com.fotonauts.lackr.MimeType;
import com.fotonauts.lackr.interpolr.Interpolr;
import com.fotonauts.lackr.interpolr.InterpolrContext;
import com.fotonauts.lackr.interpolr.InterpolrScope;

public class AppStubForESI {

    public static final String ESI_HTML = "<p> un bout de html avec des \" et des \\ dedans\nsur plusieurs lignes\npour faire joli</p>";
    public static final String ESI_JSON = "{ \"some\": \"json crap\" }";
    public static final String ESI_URL = "http://hou.salut.com/blah&merci.pour.tout";
    public static final String ESI_TEXT = "something like a \"base title\" or \nlike a http://url?with=options&blah=blih";
    public static final String ESI_MUST = "some text from the template name:{{name}} value:{{value}} some:{{esi.some}}";

    public AtomicReference<String> pageContent = new AtomicReference<>();
    public AtomicReference<String> pageMimeType = new AtomicReference<>(MimeType.TEXT_HTML);

    public InterpolrScopeStub getInterpolrScope(InterpolrContext context, String syntax, String url) {
        if (url.equals("/page.html")) {
            return new InterpolrScopeStub(context, pageContent.get().getBytes(), pageMimeType.get());
        } else if (url.equals("/empty.html")) {
            return new InterpolrScopeStub(context, "".getBytes(), MimeType.TEXT_HTML);
        } else if (url.endsWith("must")) {
            return new InterpolrScopeStub(context, ESI_MUST.getBytes(), MimeType.TEXT_PLAIN);
        } else if (url.endsWith("method")) {
            return new InterpolrScopeStub(context, ESI_MUST.getBytes(), MimeType.TEXT_PLAIN);
        } else if (url.endsWith("url")) {
            return new InterpolrScopeStub(context, ESI_URL.getBytes(), MimeType.TEXT_PLAIN);
        } else if (url.endsWith("text")) {
            return new InterpolrScopeStub(context, ESI_TEXT.getBytes(), MimeType.TEXT_PLAIN);
        } else if (url.endsWith("json")) {
            return new InterpolrScopeStub(context, ESI_JSON.getBytes(), MimeType.APPLICATION_JSON);
        } else if (url.endsWith("html")) {
            return new InterpolrScopeStub(context, ESI_HTML.getBytes(), MimeType.TEXT_HTML);
        }
        throw new RuntimeException("not handled :/");
    }

    public InterpolrContext createInterpolrContext(Interpolr interpolr) {
        return new InterpolrContextStub(interpolr) {
            
            public InterpolrScope getSubBackendExchange(String url, String syntaxIdentifier, InterpolrScope scope) {
                return getInterpolrScope(this, syntaxIdentifier, url);
            }

        };
    }
}
