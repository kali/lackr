package com.fotonauts.lackr;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.BaseProxy.EtagMode;
import com.fotonauts.lackr.LackrBackendRequest.Listener;

public class BaseFrontendRequest {

    static Logger log = LoggerFactory.getLogger(BaseFrontendRequest.class);

    protected HttpServletRequest request;

    protected BaseProxy proxy;

    protected LackrBackendRequest rootRequest;

    private AsyncContext continuation;

    private List<LackrPresentableError> backendExceptions = Collections.synchronizedList(new ArrayList<LackrPresentableError>(5));

    protected BaseFrontendRequest(final BaseProxy baseProxy, HttpServletRequest request) {
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

    protected void scheduleUpstreamRequest(final LackrBackendRequest request) {
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
            if (!getProxy().skipHeader(name)) {
                for (String value : rootRequest.getExchange().getResponse().getHeaderValues(name))
                    response.addHeader(name, value);
            }
        }
        if (rootRequest.getExchange().getResponse().getHeader(HttpHeader.CONTENT_TYPE.asString()) != null)
            response.addHeader(HttpHeader.CONTENT_TYPE.asString(),
                    rootRequest.getExchange().getResponse().getHeader(HttpHeader.CONTENT_TYPE.asString()));
    }

    protected void preflightCheck() {
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

    protected void writeSuccessResponse(HttpServletResponse response) throws IOException {
        LackrBackendExchange rootExchange = rootRequest.getExchange();

        String etag = getETag();
        log.debug("etag for response for {} is {} ({})", rootRequest, etag, getProxy().getEtagMode());

        // IF NONE MATCH
        if (getProxy().getManageIfNoneMatch() && getRequest().getMethod().equals("GET")
                && rootExchange.getResponse().getStatus() == HttpStatus.OK_200 && etag != null) {
            String ifNoneMatch = getRequest().getHeader(HttpHeader.IF_NONE_MATCH.asString());
            if (etag.equals(ifNoneMatch)) {
                log.debug("writing 304 response for {}", rootRequest);
                // spec says no content-type, no content-length with 304
                response.setStatus(HttpStatus.NOT_MODIFIED_304);
                copyResponseHeaders(response);
                response.flushBuffer(); // force commiting
                return;
            }
        }

        log.debug("writing {} response for {}", rootExchange.getResponse().getStatus(), rootRequest);
        response.setStatus(rootExchange.getResponse().getStatus());
        copyResponseHeaders(response);
        
        // ETAG
        if (getProxy().getEtagMode() != EtagMode.DISCARD && etag != null)
            response.setHeader(HttpHeader.ETAG.asString(), etag);

        // CONTENT-TYPE AND CONTENT-LENTH
        int contentLength = getContentLength();
        if (contentLength > 0)
            response.setHeader(HttpHeader.CONTENT_TYPE.asString(), Integer.toString(contentLength));
        if (rootExchange.getResponse().getHeader(HttpHeader.CONTENT_TYPE.asString()) != null)
            response.setHeader(HttpHeader.CONTENT_TYPE.asString(),
                    rootExchange.getResponse().getHeader(HttpHeader.CONTENT_TYPE.asString()));

        // CONTENT
        if(!getRequest().getMethod().equals("HEAD"))
            writeContentTo(response.getOutputStream());
        response.flushBuffer(); // force commiting
    }

    public static String getPathAndQuery(HttpServletRequest request) {
        String uri = request.getPathInfo();
        uri = StringUtil.isNotBlank(request.getQueryString()) ? uri + '?' + request.getQueryString() : uri;
        uri = uri.replace(" ", "%20");
        return uri;
    }

    public void kick() {
        try {
            byte[] body = null;
            if (request.getContentLength() > 0)
                body = IO.readBytes(request.getInputStream());

            rootRequest = new LackrBackendRequest(this, request.getMethod(), getPathAndQuery(request), null, 0, null, body,
                    buildHttpFields(), new Listener() {

                        @Override
                        public void fail(Throwable t) {
                            addBackendExceptions(t);
                            onBackendRequestDone();
                        }

                        @Override
                        public void complete() {
                            onBackendRequestDone();
                        }
                    });
            scheduleUpstreamRequest(rootRequest);
        } catch (Throwable e) {
            log.debug("in kick() error handler: " + e);
            backendExceptions.add(LackrPresentableError.fromThrowable(e));
            continuation.dispatch();
        }
    }

    public void onBackendRequestDone() {
        log.debug("Processing done, re-dispatching http thread.");
        continuation.dispatch();
    }

    private HttpFields buildHttpFields() {
        HttpFields fields = new HttpFields();
        for (Enumeration<?> e = getRequest().getHeaderNames(); e.hasMoreElements();) {
            String header = (String) e.nextElement();
            if (!getProxy().skipHeader(header)) {
                fields.add(header, getRequest().getHeader(header));
            }
        }
        if (getRequest().getContentLength() > 0 && getRequest().getContentType() != null)
            fields.add(HttpField.CONTENT_TYPE.toString(), getRequest().getContentType());
        return fields;
    }

    public void addBackendExceptions(LackrPresentableError x) {
        log.debug("Registering error:", x);
        backendExceptions.add(x);
    }

    public void addBackendExceptions(Throwable x) {
        addBackendExceptions(LackrPresentableError.fromThrowable(x));
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public LackrBackendRequest getRootRequest() {
        return rootRequest;
    }

    public BaseProxy getProxy() {
        return proxy;
    }

    public List<LackrPresentableError> getBackendExceptions() {
        return backendExceptions;
    }

    protected String getETag() {
        switch (getProxy().getEtagMode()) {
        case CONTENT_SUM:
            return generateETagForContent();
        case DISCARD:
            return null;
        case FORWARD:
            return getRootRequest().getExchange().getResponse().getHeader(HttpHeader.ETAG.asString());
        }
        return null;
    }

    protected void writeContentTo(OutputStream out) throws IOException {
        out.write(getRootRequest().getExchange().getResponse().getBodyBytes());
    }

    protected int getContentLength() {
        if (getRootRequest().getExchange().getResponse().getBodyBytes() != null)
            return getRootRequest().getExchange().getResponse().getBodyBytes().length;
        return 0;
    }

    protected String generateETagForContent() {
        MessageDigest m = null;
        try {
            m = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            // nope.
        }
        DigestOutputStream dos = new DigestOutputStream(new OutputStream() {

            @Override
            public void write(int arg0) throws IOException {
                // noop
            }
        }, m);
        dos.on(true);
        try {
            writeContentTo(dos);
            dos.flush();
            dos.close();
        } catch (IOException e) {
            // not possible with these streams
        }
        return '"' + new BigInteger(1, m.digest()).toString(16) + '"';
    }
}
