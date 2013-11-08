package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;

import org.junit.Test;

import com.fotonauts.lackr.components.AppStubForESI;
import com.fotonauts.lackr.components.Factory;
import com.fotonauts.lackr.components.InterpolrContextStub;
import com.fotonauts.lackr.components.InterpolrScopeStub;
import com.fotonauts.lackr.interpolr.Interpolr;
import com.fotonauts.lackr.interpolr.InterpolrScope;

public class TestInterpolrESI {

    private String expand(String string, String mimeType) throws Exception {
        final AppStubForESI app = new AppStubForESI();
        Interpolr interpolr = Factory.buildInterpolr(true);
        InterpolrContextStub context = new InterpolrContextStub(interpolr) {
            @Override
            public InterpolrScope getSubBackendExchange(String url, String syntaxIdentifier, InterpolrScope scope) {
                InterpolrScope result = app.getInterpolrScope(this, syntaxIdentifier, url);
                interpolr.processResult(result);
                return result;
            }
        };
        InterpolrScopeStub scope = new InterpolrScopeStub(context, string.getBytes(), mimeType);
        interpolr.processResult(scope);
        scope.getParsedDocument().check();
        System.err.println(scope.getParsedDocument().toDebugString());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        scope.getParsedDocument().writeTo(baos);
        interpolr.stop();
        return new String(baos.toString());
    }

    public String quoteJson(String text) {
        return new String(org.codehaus.jackson.io.JsonStringEncoder.getInstance().quoteAsUTF8(text)).replaceAll("\\/", "\\\\/");
    }

    @Test
    public void testHtmlInHtml() throws Exception {
        String result = expand("before\n<!--# include virtual=\"/esi.html\" -->\nafter\n", MimeType.TEXT_HTML);
        assertEquals("before\n" + AppStubForESI.ESI_HTML + "\nafter\n", result);
    }

    @Test
    public void testHtmlInJs() throws Exception {
        String result = expand("before\n\"ssi:include:virtual:/esi.html\"\nafter\n", MimeType.APPLICATION_JAVASCRIPT);
        assertEquals("before\n\"" + quoteJson( AppStubForESI.ESI_HTML) + "\"\nafter\n", result);
    }

    @Test
    public void testJsInJs() throws Exception {
        String result = expand("before\n\"ssi:include:virtual:/esi.json\"\nafter\n", MimeType.APPLICATION_JAVASCRIPT);
        assertEquals("before\n" +  AppStubForESI.ESI_JSON + "\nafter\n", result);
    }

    @Test
    public void testHtmlInMlJs() throws Exception {
        String result = expand("before\n<!--# include virtual=\\\"/esi.html\\\" -->\nafter\n", MimeType.TEXT_HTML);
        String json = quoteJson( AppStubForESI.ESI_HTML);
        assertEquals("before\n" + json + "\nafter\n", result);
    }

    @Test
    public void testEscapedHtmlInMlJs() throws Exception {
        String result = expand("before\n\\u003C!--# include virtual=\\\"/esi.html\\\" --\\u003E\nafter\n", MimeType.TEXT_HTML);
        String json = quoteJson(AppStubForESI.ESI_HTML);
        assertEquals("before\n" + json + "\nafter\n", result);
    }

    @Test
    public void testHttp() throws Exception {
        String result = expand("before\nhttp://esi.include.virtual/esi.html#\nafter\n", MimeType.TEXT_HTML);
        assertEquals("before\n" + AppStubForESI.ESI_HTML + "\nafter\n", result);
    }

    @Test
    public void testEmptyJS() throws Exception {
        String result = expand("{ something_empty: \"ssi:include:virtual:/empty.html\" }", MimeType.TEXT_JAVASCRIPT);
        assertEquals("{ something_empty: null }", result);
    }

    @Test
    public void testPlainToML() throws Exception {
        String result = expand("before\nhttp://esi.include.virtual/some.text#\nafter\n", MimeType.TEXT_HTML);
        assertEquals("before\n" + AppStubForESI.ESI_TEXT.replace("&", "&amp;").replace("\"", "&quot;") + "\nafter\n", result);
    }

    @Test
    public void testPlainToJS() throws Exception {
        String result = expand("{ something_empty: \"ssi:include:virtual:/some.text\" }", MimeType.TEXT_JAVASCRIPT);
        assertEquals("{ something_empty: \"" + AppStubForESI.ESI_TEXT.replace("\"", "\\\"").replace("/", "\\/").replace("\n", "\\n") + "\" }",
                result);
    }

    @Test
    public void testUrlEncoding() throws Exception {
        String result = expand("before\nhttp://esi.include.virtual/\u00c9si.html#\nafter\n", MimeType.APPLICATION_JAVASCRIPT);
        assertEquals("before\n" + AppStubForESI.ESI_HTML + "\nafter\n", result);
    }

    /*
    @Test
    public void testIgnorable500() throws Exception {
        String result = expand("before\nhttp://esi.include.virtual/500.html#\nafter\n", MimeType.TEXT_HTML);
        assertEquals("before\n<!-- ignore me -->\nafter\n", result);
    }
*/
    /* FIXME
    @Test
    public void testMethodsMainRequest() throws Exception {
        for (HttpMethod method : new HttpMethod[] { HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT }) {
            Request r = client.newRequest("http://localhost:" + lackrPort + "/method");
            r.method(method);
            ContentResponse e = r.send();
            assertEquals(method.asString(), e.getContentAsString());
        }
    }

    @Test
    public void testMethodSubRequest() throws Exception {
        for (HttpMethod method : new HttpMethod[] { HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT }) {
            Request r = client.newRequest("http://localhost:" + lackrPort + "/page.html");
            r.method(method);
            page.setLength(0);
            page.append("Main request does:" + method + "\n" + "ESI does:<!--# include virtual=\\\"/method\\\" -->");
            ContentResponse e = r.send();
            System.err.println(e.getContentAsString());
            assertContains(e.getContentAsString(), "ESI does:GET");
        }
    }
*/
}
