package com.fotonauts.lackr;

import static com.fotonauts.lackr.testutils.TextUtils.S;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.fotonauts.lackr.BaseProxy.EtagMode;
import com.fotonauts.lackr.testutils.Factory;
import com.fotonauts.lackr.testutils.RemoteControlledStub;
import com.fotonauts.lackr.testutils.TestClient;

@RunWith(value = Parameterized.class)
public class Test304 {

    @Parameters
    public static Collection<Object[]> data() {
        ArrayList<Object[]> params = new ArrayList<>(6);
        for (EtagMode mode : EtagMode.values()) {
            for (String proxy : new String[] { "base", "interpolr" })
                for (boolean backEtags : new Boolean[] { false, true })
                    params.add(new Object[] { mode, proxy, backEtags });
        }
        return params;
    }

    private AtomicReference<String> pageData = new AtomicReference<>("coin");
    private boolean backendSetsEtag;

    private String proxyType;
    private EtagMode mode;
    private RemoteControlledStub remoteControlledStub;
    private Server server;
    private TestClient client;

    public Test304(EtagMode mode, String proxyType, boolean backendSetsEtag) {
        this.mode = mode;
        this.proxyType = proxyType;
        this.backendSetsEtag = backendSetsEtag;
        // System.err.println("mode:" + mode + " proxy:" + proxyType + " backendSetsEtag:" + backendSetsEtag);
    }

    @Before
    public void setup() throws Exception {
        remoteControlledStub = new RemoteControlledStub();
        remoteControlledStub.getCurrentHandler().set(new AbstractHandler() {

            @Override
            public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request,
                    HttpServletResponse response) throws IOException, ServletException {
                String content;
                if (target.equals("/variable.html"))
                    content = Long.toString(System.currentTimeMillis());
                else
                    content = pageData.get();

                if (backendSetsEtag)
                    response.setHeader(HttpHeader.ETAG.asString(), "backend-etag-" + content);
                response.setContentType(MimeType.TEXT_HTML);
                response.setContentLength(content.getBytes().length);
                response.getOutputStream().write(content.getBytes());
                response.flushBuffer();
            }
        });
        remoteControlledStub.start();

        BaseProxy proxy;
        if ("interpolr".equals(proxyType))
            proxy = Factory.buildInterpolrProxy(Factory.buildInterpolr("esi"),
                    Factory.buildFullClientBackend(remoteControlledStub.getPort()));
        else
            proxy = Factory.buildSimpleBaseProxy(Factory.buildFullClientBackend(remoteControlledStub.getPort()));

        proxy.setEtagMode(mode);
        server = Factory.buildProxyServer(proxy);
        server.start();

        client = new TestClient(server);
        client.start();
    }

    @After
    public void tearDown() throws Exception {
        client.stop();
        server.stop();
        remoteControlledStub.stop();
        assertTrue(Thread.getAllStackTraces().size() < 10);
    }

    @Test
    public void testEtagTrivialModes() throws Exception {
        pageData.set("blah");
        ContentResponse response = client.createExchange("/").send();
        String etag = response.getHeaders().getStringField(HttpHeader.ETAG);
        switch (mode) {
        case DISCARD:
            assertEquals(etag, null);
            break;
        case FORWARD:
            assertEquals(etag, backendSetsEtag ? "backend-etag-blah" : null);
            break;
        case CONTENT_SUM:
            assertNotNull(etag);
            assertFalse(etag.startsWith("backend-etag"));
            break;
        }
    }

    @Test
    public void testEtagGeneration() throws Exception {
        if (mode != EtagMode.CONTENT_SUM)
            return;
        pageData.set("blah");
        ContentResponse e1 = client.runRequest(client.createExchange("/page.html"), "blah");
        String etag1 = e1.getHeaders().getStringField("etag");
        assertNotNull("e1 has etag", etag1);

        pageData.set("blih");
        ContentResponse e2 = client.runRequest(client.createExchange("/page.html"), "blih");
        String etag2 = e2.getHeaders().getStringField("etag");
        assertNotNull("e2 has etag", etag2);

        assertTrue("etags are different", !etag1.equals(etag2));
    }

    @Test
    public void testEtagESIGeneration() throws Exception {
        if (mode != EtagMode.CONTENT_SUM || !proxyType.equals("interpolr"))
            return;
        pageData.set(S(/*<!--# include virtual="/variable.html" -->*/));
        ContentResponse e1 = client.createExchange("/page.html").send();
        String etag1 = e1.getHeaders().getStringField("etag");
        assertNotNull("e1 has etag", etag1);

        ContentResponse e2 = client.createExchange("/page.html").send();
        String etag2 = e2.getHeaders().getStringField("etag");
        assertNotNull("e2 has etag", etag2);

        assertFalse("etags must be different", etag1.equals(etag2));
    }

    @Test
    public void testEtagAndIfNoneMatch() throws Exception {
        if (mode == EtagMode.DISCARD || (mode == EtagMode.FORWARD && !backendSetsEtag))
            return;
        pageData.set("blah");
        ContentResponse e1 = client.runRequest(client.createExchange("/page.html"), "blah");
        String etag1 = e1.getHeaders().getStringField("etag");
        assertNotNull("e1 has etag", etag1);

        ContentResponse e2 = client.runRequest(client.createExchange("/page.html"), "blah");
        assertEquals(e2.getStatus(), HttpStatus.OK_200);

        Request req3 = client.createExchange("/page.html");
        req3.header(HttpHeader.IF_NONE_MATCH, etag1);
        ContentResponse e3 = req3.send();
        assertEquals(HttpStatus.NOT_MODIFIED_304, e3.getStatus());
        assertFalse(e3.getHeaders().getFieldNamesCollection().contains(HttpHeader.CONTENT_LENGTH));
        assertFalse(e3.getHeaders().getFieldNamesCollection().contains(HttpHeader.CONTENT_TYPE));
        assertTrue(e3.getContent() == null || e3.getContent().length == 0);

        pageData.set("blih");
        Request req4 = client.createExchange("/page.html");
        req4.header(HttpHeader.IF_NONE_MATCH, etag1);
        ContentResponse e4 = client.runRequest(req4, "blih");
        assertEquals(e4.getStatus(), HttpStatus.OK_200);
    }
}
