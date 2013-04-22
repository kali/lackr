package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Test;
import org.springframework.util.FileCopyUtils;

public class TestPropagation extends BaseTestLackrFullStack {

    public TestPropagation() throws Exception {
        super();
    }

    @Test
    public void hostProp() throws Exception {

        currentHandler.set(new AbstractHandler() {

            @Override
            public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request,
                    HttpServletResponse response) throws IOException, ServletException {
                writeResponse(response, request.getHeader("Host").getBytes(), MimeType.TEXT_HTML);
            }
        });

        Request e = createExchange("http://localhost:" + lackrPort + "/");
        e.header("Host", "something");
        runRequest(e, "something");
    }

    @Test
    public void userAgentProp() {

        currentHandler.set(new AbstractHandler() {

            @Override
            public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request,
                    HttpServletResponse response) throws IOException, ServletException {
                writeResponse(response, request.getHeader("User-Agent").getBytes(), MimeType.TEXT_HTML);
            }
        });

        Request e = createExchange("http://localhost:" + lackrPort + "/");
        e.header("User-Agent", "something");
        runRequest(e, "something");
    }

    @Test
    public void reqBodyProp() {
        currentHandler.set(new AbstractHandler() {

            @Override
            public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request,
                    HttpServletResponse response) throws IOException, ServletException {
                writeResponse(response, FileCopyUtils.copyToByteArray(request.getInputStream()), MimeType.TEXT_HTML);
            }
        });

        Request e = createExchange("http://localhost:" + lackrPort + "/");
        System.err.println();
        e.method(HttpMethod.POST);
        e.content(new StringContentProvider("coin"));
        e.header("Content-Length", "4");
        runRequest(e, "coin");
    }

    @Test
    public void accept() {
        currentHandler.set(new AbstractHandler() {

            @Override
            public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request,
                    HttpServletResponse response) throws IOException, ServletException {
                writeResponse(response,
                        request.getHeader("Accept") != null ? request.getHeader("Accept").getBytes() : "null".getBytes(),
                        MimeType.TEXT_PLAIN);
            }
        });

        Request e = createExchange("http://localhost:" + lackrPort + "/");
        e.header("Accept", "test/accept");
        runRequest(e, "test/accept");
    }

    @Test
    public void noAccept() {
        currentHandler.set(new AbstractHandler() {

            @Override
            public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request,
                    HttpServletResponse response) throws IOException, ServletException {
                writeResponse(response,
                        request.getHeader("Accept") != null ? request.getHeader("Accept").getBytes() : "null".getBytes(),
                        MimeType.TEXT_PLAIN);
            }
        });

        Request e = createExchange("http://localhost:" + lackrPort + "/");
        runRequest(e, "null");
    }

    @Test
    public void redirect() throws InterruptedException, TimeoutException, ExecutionException {
        currentHandler.set(new AbstractHandler() {

            @Override
            public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request,
                    HttpServletResponse response) throws IOException, ServletException {
                response.addHeader("Location", "http://blah.com");
                response.setStatus(301);
                response.flushBuffer();
            }
        });

        Request e = createExchange("http://localhost:" + lackrPort + "/");
        ContentResponse r = e.send();

        assertEquals(301, r.getStatus());
        assertEquals("http://blah.com", r.getHeaders().getStringField("Location"));
    }

    @Test
    public void timeout() throws InterruptedException, TimeoutException, ExecutionException {
        currentHandler.set(new AbstractHandler() {

            @Override
            public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request,
                    HttpServletResponse response) throws IOException, ServletException {
                try {
                    Thread.sleep(1000 * 1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Request e = createExchange("http://localhost:" + lackrPort + "/");
        ContentResponse r = e.send();
    }

    @Test
    public void parameters() {
        currentHandler.set(new AbstractHandler() {
            @Override
            public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request,
                    HttpServletResponse response) throws IOException, ServletException {
                writeResponse(response, (request.getParameter("par") != null ? request.getParameter("par") : "#null").getBytes(),
                        MimeType.TEXT_PLAIN);
            }
        });
        Request e = createExchange("http://localhost:" + lackrPort + "/?par=toto");
        runRequest(e, "toto");

        e = createExchange("http://localhost:" + lackrPort + "/?par=toto");
        e.method(HttpMethod.POST);
        runRequest(e, "toto");

        e = createExchange("http://localhost:" + lackrPort + "/?par=toto");
        e.method(HttpMethod.PUT);
        runRequest(e, "toto");
    }

    @Test
    public void cookiesIsolation() {
        currentHandler.set(new AbstractHandler() {
            @Override
            public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request,
                    HttpServletResponse response) throws IOException, ServletException {
                if (target.indexOf("SETIT") > 0)
                    response.addHeader(HttpHeader.SET_COOKIE.asString(), "c=1; Expires=Wed, 09-Jun-2021 10:18:14 GMT");
                writeResponse(response,
                        request.getHeader("Cookie") != null ? request.getHeader("Cookie").getBytes() : "null".getBytes(),
                        MimeType.TEXT_PLAIN);
            }
        });
        Request e = createExchange("http://localhost:" + lackrPort + "/");
        runRequest(e, "null");
        assertTrue("cookie store empty", client.getCookieStore().getCookies().size() == 0);

        e = createExchange("http://localhost:" + lackrPort + "/SETIT");
        ContentResponse r = runRequest(e, "null");
        System.err.println(r.getHeaders().getStringField(HttpHeader.SET_COOKIE.asString()).contains("c=1"));
        assertTrue("cookie store empty", client.getCookieStore().getCookies().size() == 0);
        assertTrue("response has set-cookie", r.getHeaders().getStringField(HttpHeader.SET_COOKIE.asString()).contains("c=1"));

        e = createExchange("http://localhost:" + lackrPort + "/");
        runRequest(e, "null");
        assertTrue("cookie store empty", client.getCookieStore().getCookies().size() == 0);

        e = createExchange("http://localhost:" + lackrPort + "/");
        e.getHeaders().add(HttpHeader.COOKIE.asString(), "c=2");
        runRequest(e, "c=2");
        assertTrue("cookie store empty", client.getCookieStore().getCookies().size() == 0);
    }

    @Test
    public void hugeCookie() {
        currentHandler.set(new AbstractHandler() {
            @Override
            public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request,
                    HttpServletResponse response) throws IOException, ServletException {
                writeResponse(response, "yop!".getBytes(), MimeType.TEXT_PLAIN);
            }
        });
        Request e = createExchange("http://localhost:" + lackrPort + "/");
        StringBuilder sb = new StringBuilder(10000);
        for (int i = 0; i < 10000; i++)
            sb.append("c");
        e.getHeaders().add("Cookie", "cook=" + sb.toString());
        ContentResponse r = null;
        try {
            r = e.send();
        } catch (InterruptedException | TimeoutException | ExecutionException e1) {
            e1.printStackTrace();
        }
        assertEquals(HttpStatus.REQUEST_ENTITY_TOO_LARGE_413, r.getStatus());
    }
}
