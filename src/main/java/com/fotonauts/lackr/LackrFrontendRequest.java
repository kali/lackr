package com.fotonauts.lackr;

import static com.fotonauts.commons.RapportrLoggingKeys.DATA;
import static com.fotonauts.commons.RapportrLoggingKeys.DATE;
import static com.fotonauts.commons.RapportrLoggingKeys.ELAPSED;
import static com.fotonauts.commons.RapportrLoggingKeys.SIZE;
import static com.fotonauts.commons.RapportrLoggingKeys.STATUS;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationListener;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import com.fotonauts.commons.RapportrService;
import com.fotonauts.commons.UserAgent;
import com.fotonauts.lackr.hashring.HashRing.NotAvailableException;
import com.fotonauts.lackr.interpolr.Document;
import com.fotonauts.lackr.mustache.MustacheContext;
import com.mongodb.BasicDBObject;

public class LackrFrontendRequest {
    static String[] headersToSkip = { "proxy-connection", "connection", "keep-alive", "transfer-encoding", "te", "trailer",
            "proxy-authorization", "proxy-authenticate", "upgrade", "content-length", "if-modified-since", "if-none-match" };

    static boolean skipHeader(String header) {
        for (String skip : headersToSkip) {
            if (skip.equals(header.toLowerCase()))
                return true;
        }
        return false;
    }

    AtomicInteger pendingCount;

    static Logger log = LoggerFactory.getLogger(LackrFrontendRequest.class);

    protected HttpServletRequest request;

    protected Service service;

    private String rootUrl;

    private String opid;

    protected BackendRequest rootRequest;

    private Continuation continuation;

    private List<LackrPresentableError> backendExceptions = Collections.synchronizedList(new ArrayList<LackrPresentableError>(5));

    protected BasicDBObject logLine;

    protected long startTimestamp;

    private UserAgent userAgent;

    private MustacheContext mustacheContext;

    private ConcurrentHashMap<String, BackendRequest> backendRequestCache = new ConcurrentHashMap<String, BackendRequest>();

    LackrFrontendRequest(final Service service, HttpServletRequest request) throws IOException {
        this.service = service;
        long id = service.getGateway().getRunningRequestsHolder().incrementAndGet();
        opid = request.getHeader("X-Ftn-OperationId");
        if (opid == null)
            opid = "<noopid:" + id + ">";
        this.request = request;
        this.mustacheContext = new MustacheContext();
        this.continuation = ContinuationSupport.getContinuation(request);
        this.continuation.setTimeout(getService().getTimeout() * 1000);
        this.continuation.addContinuationListener(new ContinuationListener() {

            @Override
            public void onTimeout(Continuation continuation) {
                /* onComplete will also be called after a timeout */
            }

            @Override
            public void onComplete(Continuation continuation) {
                service.getGateway().getRunningRequestsHolder().decrementAndGet();
            }
        });
        this.pendingCount = new AtomicInteger(0);
        URI uri = null;
        try {
            uri = new URI(null, null, request.getPathInfo(), null);
        } catch (URISyntaxException e) {
            throw new RuntimeException("invalid URL");
        }
        rootUrl = StringUtils.hasText(request.getQueryString()) ? uri.toASCIIString() + '?' + request.getQueryString() : uri
                .toASCIIString();
        rootUrl = rootUrl.replace(" ", "%20");

        logLine = RapportrService.accessLogLineTemplate(request, "lackr-front");

        this.userAgent = new UserAgent(request.getHeader(HttpHeaders.USER_AGENT));

        continuation.suspend();
    }

    public BackendRequest getSubBackendExchange(String url, String format, BackendRequest dad) throws NotAvailableException {
        String key = format + "::" + url;
        BackendRequest ex = backendRequestCache.get(key);
        if (ex != null)
            return ex;
        ex = new BackendRequest(this, "GET", url, dad.getQuery(), dad.hashCode(), format, null);
        backendRequestCache.put(key, ex);
        scheduleUpstreamRequest(ex);
        return ex;
    }

    private void scheduleUpstreamRequest(final BackendRequest request) throws NotAvailableException {
        this.pendingCount.incrementAndGet();
        getService().getExecutor().execute(new Runnable() {

            @Override
            public void run() {
                try {
                    request.start();
                } catch (IOException e) {
                    addBackendExceptions(LackrPresentableError.fromThrowable(e));
                } catch (NotAvailableException e) {
                    addBackendExceptions(LackrPresentableError.fromThrowable(e));
                } catch (NullPointerException e) {
                    System.out.println("The exchange " + request.getQuery() + " has thrown a NullPointerException.");
                    e.printStackTrace(System.err);
                    // This occurs sometimes in X-SSI-ROOT inject in the backend
                    addBackendExceptions(LackrPresentableError.fromThrowable(e));
                }
            }
        });
    }

    public UserAgent getUserAgent() {
        return userAgent;
    }

    public void copyResponseHeaders(HttpServletResponse response) {
        for (String name : rootRequest.getExchange().getResponseHeaderNames()) {
            if (!skipHeader(name)) {
                for (String value : rootRequest.getExchange().getResponseHeaderValues(name))
                    response.addHeader(name, value);
            }
        }
    }

    private void preflightCheck() {
        getMustacheContext().checkAndCompileAll(backendExceptions);
        if (rootRequest.getParsedDocument() != null) {
            rootRequest.getParsedDocument().check();
        }
    }

    public void writeResponse(HttpServletResponse response) throws IOException {
        if (request.getHeader("X-Ftn-OperationId") != null)
            response.addHeader("X-Ftn-OperationId", request.getHeader("X-Ftn-OperationId"));
        preflightCheck();
        try {
            if (pendingCount.get() > 0 || !backendExceptions.isEmpty()) {
                writeErrorResponse(response);
            } else {
                writeSuccessResponse(response);
            }
        } catch (IOException writeResponseException) {
            logLine.put(STATUS.getPrettyName(), "500");
            logLine.put(DATA.getPrettyName(), writeResponseException.getMessage());
            throw writeResponseException;
        } finally {
            long endTimestamp = System.currentTimeMillis();
            logLine.put(ELAPSED.getPrettyName(), 1.0 * (endTimestamp - startTimestamp) / 1000);
            logLine.put(DATE.getPrettyName(), new Date().getTime());
            service.getRapportr().log(logLine);
            service.getGateway().getElapsedMillisHolder().addAndGet(endTimestamp - startTimestamp);
        }
    }

    public void writeErrorResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
        response.setContentType("text/plain");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        for (LackrPresentableError t : backendExceptions) {
            ps.println(t.getMessage());
        }
        ps.flush();
        response.setContentLength(baos.size());
        response.getOutputStream().write(baos.toByteArray());

        String message;
        try {
            message = backendExceptions.get(0).getMessage().split("\n")[0];
            if (backendExceptions.size() > 1)
                message = message + " — and friends.";
        } catch (Throwable e) {
            message = "Failed to extract a nice message from this mess";
        }

        logLine.put(STATUS.getPrettyName(), Integer.toString(HttpServletResponse.SC_BAD_GATEWAY));
        logLine.put(DATA.getPrettyName(), baos.toByteArray());
        getService().getRapportr().rapportrException(request, message, new String(baos.toByteArray(), "UTF-8"));
    }

    public void writeSuccessResponse(HttpServletResponse response) throws IOException {
        LackrBackendExchange rootExchange = rootRequest.getExchange();
        response.setStatus(rootExchange.getResponseStatus());
        copyResponseHeaders(response);
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies())
                if ("uid".equals(c.getName())) {
                    String domain = request.getHeader("Host") != null ? request.getHeader("Host").replaceFirst("^[^\\.]*\\.", ".")
                            : ".fotopedia.com";
                    response.addHeader(
                            "Set-Cookie",
                            String.format("uid=%s;Version=1;Path=/;Domain=%s;Expires=Thu, 31-Dec-2037 06:00:00 GMT;Max-Age=%d",
                                    c.getValue(), domain, 2145852000 - System.currentTimeMillis() / 1000));
                }
        }
        log.debug("writing success response for " + rootRequest.getQuery());
        if (rootRequest.getParsedDocument().length() > 0) {
            String etag = generateEtag(rootRequest.getParsedDocument());
            response.setHeader(HttpHeaders.ETAG, etag);
            if (log.isDebugEnabled()) {
                log.debug("etag: " + etag);
                log.debug("if-none-match: " + request.getHeader(HttpHeaders.IF_NONE_MATCH));
            }
            if (rootExchange.getResponseStatus() == HttpStatus.OK_200 && etag.equals(request.getHeader(HttpHeaders.IF_NONE_MATCH))) {
                response.setStatus(HttpStatus.NOT_MODIFIED_304);
                response.setHeader("Status", "304 Not Modified");
                response.flushBuffer(); // force commiting
                logLine.put(STATUS.getPrettyName(), Integer.toString(HttpStatus.NOT_MODIFIED_304));
            } else {
                logLine.put(STATUS.getPrettyName(), Integer.toString(rootExchange.getResponseStatus()));
                response.setContentLength(rootRequest.getParsedDocument().length());
                if (request.getMethod() != "HEAD")
                    rootRequest.getParsedDocument().writeTo(response.getOutputStream());
                response.flushBuffer();
            }
            logLine.put(SIZE.getPrettyName(), rootRequest.getParsedDocument().length());
        } else {
            logLine.put(STATUS.getPrettyName(), Integer.toString(rootExchange.getResponseStatus()));
            response.flushBuffer(); // force commiting
        }
    }

    private String generateEtag(Document content) {
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
            content.writeTo(dos);
            dos.flush();
        } catch (IOException e) {
            // not possible with these streams
        }
        return '"' + new BigInteger(1, m.digest()).toString(16) + '"';
    }

    public void kick() {
        startTimestamp = System.currentTimeMillis();
        try {
            byte[] body = null;
            if (request.getContentLength() > 0)
                body = FileCopyUtils.copyToByteArray(request.getInputStream());
            rootRequest = new BackendRequest(this, request.getMethod() == "HEAD" ? "GET" : request.getMethod(), rootUrl, null, 0,
                    null, body);
            scheduleUpstreamRequest(rootRequest);
        } catch (Throwable e) {
            log.debug("in kick() error handler: " + e);
            backendExceptions.add(LackrPresentableError.fromThrowable(e));
            continuation.resume();
        }
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

    public Service getService() {
        return service;
    }

    public void notifySubRequestDone() {
        if (pendingCount.decrementAndGet() <= 0) {
            if (log.isDebugEnabled())
                log.debug("Gathered all fragments for " + rootUrl + " with " + backendExceptions.size() + " exceptions.");
            continuation.resume();
        }
    }

    public MustacheContext getMustacheContext() {
        return mustacheContext;
    }

    public BackendRequest getRootRequest() {
        return rootRequest;
    }
}
