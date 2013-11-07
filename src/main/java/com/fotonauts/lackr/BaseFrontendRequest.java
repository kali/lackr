package com.fotonauts.lackr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.backend.LackrBackendExchange;
import com.fotonauts.lackr.backend.LackrBackendRequest;
import com.fotonauts.lackr.backend.hashring.HashRing.NotAvailableException;

public class BaseFrontendRequest {

    static String[] headersToSkip = { "proxy-connection", "connection", "keep-alive", "transfer-encoding", "te", "trailer",
            "proxy-authorization", "proxy-authenticate", "upgrade", "content-length", "content-type", "if-modified-since",
            "if-none-match", "range", "accept-ranges" };

    public static boolean skipHeader(String header) {
        for (String skip : headersToSkip) {
            if (skip.equals(header.toLowerCase()))
                return true;
        }
        return false;
    }

    static Logger log = LoggerFactory.getLogger(BaseFrontendRequest.class);

    protected HttpServletRequest request;

    protected BaseProxy proxy;

    protected LackrBackendRequest rootRequest;

    private AsyncContext continuation;

    private List<LackrPresentableError> backendExceptions = Collections.synchronizedList(new ArrayList<LackrPresentableError>(5));

    BaseFrontendRequest(final BaseProxy baseProxy, HttpServletRequest request) {
        this.proxy = baseProxy;
        this.request = request;
        this.continuation = request.startAsync();
// FIXME        this.continuation.setTimeout(getService().getTimeout() * 1000);
        this.continuation.setTimeout(5 * 1000);
        this.continuation.addListener(new AsyncListener() {

            @Override
            public void onComplete(AsyncEvent event) throws IOException {
            }

            @Override
            public void onTimeout(AsyncEvent event) throws IOException {
                // TODO Auto-generated method stub

            }

            @Override
            public void onError(AsyncEvent event) throws IOException {
                // TODO Auto-generated method stub

            }

            @Override
            public void onStartAsync(AsyncEvent event) throws IOException {
                // TODO Auto-generated method stub

            }
        });
    }

    private void scheduleUpstreamRequest(final LackrBackendRequest request) throws NotAvailableException {
        proxy.getExecutor().execute(new Runnable() {

            @Override
            public void run() {
                try {
                    request.start();
                } catch (Throwable e) {
                    e.printStackTrace(System.err);
                    addBackendExceptions(LackrPresentableError.fromThrowable(e));
                }
            }

        });
    }

    public void copyResponseHeaders(HttpServletResponse response) {
        for (String name : rootRequest.getExchange().getResponse().getHeaderNames()) {
            if (!skipHeader(name)) {
                for (String value : rootRequest.getExchange().getResponse().getHeaderValues(name))
                    response.addHeader(name, value);
            }
        }
        if (rootRequest.getExchange().getResponse().getHeader(HttpHeader.CONTENT_TYPE.asString()) != null)
            response.addHeader(HttpHeader.CONTENT_TYPE.asString(),
                    rootRequest.getExchange().getResponse().getHeader(HttpHeader.CONTENT_TYPE.asString()));
    }

    private void preflightCheck() {
    }

    public void writeResponse(HttpServletResponse response) throws IOException {

        if (backendExceptions.isEmpty()) {
            preflightCheck();
        }

        try {
            if (!backendExceptions.isEmpty()) {
                writeErrorResponse(response);
            } else {
                writeSuccessResponse(response);
            }
        } catch (IOException writeResponseException) {
            throw writeResponseException;
        } finally {
        }
    }

    public void writeErrorResponse(HttpServletResponse response) throws IOException {
        log.debug("writing error response for " + rootRequest.getQuery());

        response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
        response.setContentType("text/plain");
        StringBuilder sb = new StringBuilder();// new PrintStream(baos);
        for (LackrPresentableError t : backendExceptions) {
            log.debug("Backend error: ", t);
            sb.append(t.getMessage());
            sb.append('\n');
        }
        String s = sb.toString();
        byte[] ba = s.getBytes("UTF-8");
        response.setContentLength(ba.length);
        response.getOutputStream().write(ba);

        String message;
        try {
            message = backendExceptions.get(0).getMessage().split("\n")[0];
            if (backendExceptions.size() > 1)
                message = message + " — and friends.";
        } catch (Throwable e) {
            message = "Failed to extract a nice message from this mess";
        }
    }

    public void writeSuccessResponse(HttpServletResponse response) throws IOException {
        LackrBackendExchange rootExchange = rootRequest.getExchange();

        response.setStatus(rootExchange.getResponse().getStatus());
        copyResponseHeaders(response);

        log.debug("writing success response for " + rootRequest.getQuery());
        if (rootRequest.getParsedDocument().length() > 0) {
            response.setContentLength(rootRequest.getParsedDocument().length());
            if (request.getMethod() != "HEAD")
                rootRequest.getParsedDocument().writeTo(response.getOutputStream());
            response.flushBuffer();
        } else {
            response.flushBuffer(); // force commiting
        }
    }

    public void kick() {
        try {
            byte[] body = null;
            if (request.getContentLength() > 0)
                body = IO.readBytes(request.getInputStream());

            String uri = request.getPathInfo();
            uri = StringUtil.isNotBlank(request.getQueryString()) ? uri + '?' + request.getQueryString() : uri;
            uri = uri.replace(" ", "%20");

            rootRequest = new LackrBackendRequest(this, request.getMethod() == "HEAD" ? "GET" : request.getMethod(), uri, null, 0,
                    null, body, buildHttpFields());
            scheduleUpstreamRequest(rootRequest);
        } catch (Throwable e) {
            log.debug("in kick() error handler: " + e);
            backendExceptions.add(LackrPresentableError.fromThrowable(e));
            continuation.dispatch();
        }
    }

    private HttpFields buildHttpFields() {
        HttpFields fields = new HttpFields();
        for (Enumeration<?> e = getRequest().getHeaderNames(); e.hasMoreElements();) {
            String header = (String) e.nextElement();
            if (!BaseFrontendRequest.skipHeader(header)) {
                fields.add(header, getRequest().getHeader(header));
            }
        }
        if (getRequest().getContentLength() > 0 && getRequest().getContentType() != null)
            fields.add(HttpField.CONTENT_TYPE.toString(), getRequest().getContentType());
        return fields;
    }

    public void addBackendExceptions(LackrPresentableError x) {
        backendExceptions.add(x);
    }

    public void addBackendExceptions(Throwable x) {
        addBackendExceptions(LackrPresentableError.fromThrowable(x));
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void notifySubRequestDone() {
        log.debug("Sub request done, dispatching.");
        continuation.dispatch();
    }

    public LackrBackendRequest getRootRequest() {
        return rootRequest;
    }

    public BaseProxy getProxy() {
        return proxy;
    }
}
