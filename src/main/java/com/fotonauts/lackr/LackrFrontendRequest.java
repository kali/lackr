package com.fotonauts.lackr;

import static com.fotonauts.commons.RapportrLoggingKeys.DATA;
import static com.fotonauts.commons.RapportrLoggingKeys.DATE;
import static com.fotonauts.commons.RapportrLoggingKeys.ELAPSED;
import static com.fotonauts.commons.RapportrLoggingKeys.SIZE;
import static com.fotonauts.commons.RapportrLoggingKeys.STATUS;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import com.fotonauts.commons.FrontendEndpointMatcher;
import com.fotonauts.commons.RapportrService;
import com.fotonauts.commons.UserAgent;
import com.fotonauts.lackr.femtor.InProcessFemtor;
import com.fotonauts.lackr.hashring.HashRing.NotAvailableException;
import com.fotonauts.lackr.interpolr.Document;
import com.fotonauts.lackr.mustache.MustacheContext;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

public class LackrFrontendRequest {
    static String[] headersToSkip = { "proxy-connection", "connection", "keep-alive", "transfer-encoding", "te", "trailer",
            "proxy-authorization", "proxy-authenticate", "upgrade", "content-length", "content-type", "if-modified-since",
            "if-none-match", "range", "accept-ranges" };

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

    protected Map<String, String> ancillialiaryHeaders = Collections.synchronizedMap(new HashMap<String, String>(5));

    protected Service service;

    private String rootUrl;

    private String opid;

    protected BackendRequest rootRequest;

    private AsyncContext continuation;

    private List<LackrPresentableError> backendExceptions = Collections.synchronizedList(new ArrayList<LackrPresentableError>(5));

    protected BasicDBObject logLine;

    protected long startTimestamp;

    private UserAgent userAgent;

    private MustacheContext mustacheContext;

    private ConcurrentHashMap<String, BackendRequest> backendRequestCache = new ConcurrentHashMap<String, BackendRequest>();

    private AtomicInteger backendRequestCounts[];

    private ConcurrentHashMap<String, AtomicInteger> backendRequestEndpointsCounters = new ConcurrentHashMap<String, AtomicInteger>();

    LackrFrontendRequest(final Service service, HttpServletRequest request) throws IOException {
        this.service = service;
        service.getGateway().getRunningRequestsHolder().inc();
        opid = request.getHeader("X-Ftn-OperationId");
        if (opid == null)
            opid = "<noopid:" + UUID.randomUUID().toString() + ">";
        this.request = request;
        this.mustacheContext = new MustacheContext(this);
        this.backendRequestCounts = new AtomicInteger[service.getBackends().length];
        for (int i = 0; i < service.getBackends().length; i++)
            this.backendRequestCounts[i] = new AtomicInteger();
        this.continuation = request.startAsync();
        this.continuation.setTimeout(getService().getTimeout() * 1000);
        this.continuation.addListener(new AsyncListener() {

            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                service.getGateway().getRunningRequestsHolder().dec();
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

        this.userAgent = new UserAgent(request.getHeader(HttpHeader.USER_AGENT.asString()));

        //        continuation.suspend();
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
                } catch (Throwable e) {
                    addBackendExceptions(LackrPresentableError.fromThrowable(e));
                    notifySubRequestDone();
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
        if (rootRequest.getExchange().getResponseHeader(HttpHeader.CONTENT_TYPE.asString()) != null)
            response.addHeader(HttpHeader.CONTENT_TYPE.asString(),
                    rootRequest.getExchange().getResponseHeader(HttpHeader.CONTENT_TYPE.asString()));
    }

    private void preflightCheck() {
        try {
            getMustacheContext().checkAndCompileAll();
            if (rootRequest.getParsedDocument() != null) {
                rootRequest.getParsedDocument().check();
            }
        } catch (Throwable e) {
            backendExceptions.add(LackrPresentableError.fromThrowable(e));
        }
    }

    public void writeResponse(HttpServletResponse response) throws IOException {
        if (request.getHeader("X-Ftn-OperationId") != null)
            response.addHeader("X-Ftn-OperationId", request.getHeader("X-Ftn-OperationId"));

        BasicDBObject backendRequestCounters = new BasicDBObject();
        for (int i = 0; i < service.getBackends().length; i++) {
            int value = this.backendRequestCounts[i].get();
            if (value > 0)
                backendRequestCounters.put(service.getBackends()[i].getClass().getSimpleName() + "-" + i, value);
        }
        logLine.put("counters", backendRequestCounters);

        BasicDBList backendEndpointCounters = new BasicDBList();
        for (Map.Entry<String, AtomicInteger> endpoint : backendRequestEndpointsCounters.entrySet()) {
            BasicDBObject obj = new BasicDBObject();
            obj.append("ep", endpoint.getKey());
            obj.append("c", endpoint.getValue());
            backendEndpointCounters.add(obj);
        }
        if (backendEndpointCounters.size() > 0)
            logLine.put("peps", backendEndpointCounters);

        if (backendExceptions.isEmpty()) {
            preflightCheck();
        }

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
            service.getGateway().getElapsedMillisHolder().inc(endTimestamp - startTimestamp);
            String status = logLine.getString(STATUS.getPrettyName());
            service.countStatus(status);
            String endpoint = FrontendEndpointMatcher.matchUrl(logLine.getString("path"), request.getQueryString());
            if (endpoint != null) {
                endpoint = endpoint.replace('/', '-').replace('.', '-').replaceAll("\\*\\*", "XXX").replace('*', 'X');
                service.countEndpointWithTimer(endpoint, endTimestamp - startTimestamp);
                for (Map.Entry<String, AtomicInteger> beEndpoint : backendRequestEndpointsCounters.entrySet()) {
                    service.countPicorEpPerEP(endpoint, beEndpoint.getKey().replace('/', '-').replace('.', '-'), beEndpoint
                            .getValue().intValue());
                }
                for (int i = 0; i < service.getBackends().length; i++) {
                    int value = this.backendRequestCounts[i].get();
                    if (value > 0) {
                        String nicerName = service.getBackends()[i].getClass() == InProcessFemtor.class ? "femtor" : "http-" + i;
                        service.countBePerEP(endpoint, nicerName, value);
                    }
                }

            }
        }
    }

    public void writeErrorResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
        response.setContentType("text/plain");
        StringBuilder sb = new StringBuilder();// new PrintStream(baos);
        for (LackrPresentableError t : backendExceptions) {
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

        logLine.put(STATUS.getPrettyName(), Integer.toString(HttpServletResponse.SC_BAD_GATEWAY));
        logLine.put(DATA.getPrettyName(), s);
        getService().getRapportr().rapportrException(request, message, s);
    }

    public void writeSuccessResponse(HttpServletResponse response) throws IOException {
        LackrBackendExchange rootExchange = rootRequest.getExchange();
        if (rootExchange.getResponseStatus() == 398 && rootExchange.getResponseHeaderValue("Location") != null) {
            // 398 is a special adhoc http code: used as a way for femtor to ask lackr to proxy and pipe a entirely different url
            logLine.put(STATUS.getPrettyName(), Integer.toString(rootExchange.getResponseStatus()));
            asyncProxy(response, rootExchange.getResponseHeaderValue("Location"));
            return;
        }

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
            response.setHeader(HttpHeader.ETAG.asString(), etag);
            if (log.isDebugEnabled()) {
                log.debug("etag: " + etag);
                log.debug("if-none-match: " + request.getHeader(HttpHeader.IF_NONE_MATCH.asString()));
            }
            if (rootExchange.getResponseStatus() == HttpStatus.OK_200
                    && etag.equals(request.getHeader(HttpHeader.IF_NONE_MATCH.asString()))) {
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

    private void asyncProxy(final HttpServletResponse lackrResponse, final String url) {
        getRequest().startAsync();
        Request req = getService().getClient().newRequest(url);
        req.method(HttpMethod.GET);
        req.send(new Response.Listener.Empty() {
            @Override
            public void onFailure(Response response, Throwable failure) {
                System.err.println(failure);
            }

            @Override
            public void onBegin(Response response) {
                lackrResponse.setStatus(response.getStatus());
            }

            @Override
            public void onHeaders(Response response) {
                String contentType = response.getHeaders().getStringField(HttpHeader.CONTENT_TYPE);
                if (contentType == null && url.indexOf(".jpg") >= 0)
                    contentType = "image/jpeg";
                lackrResponse.setContentType(contentType);
                lackrResponse.setHeader(HttpHeader.CONTENT_LENGTH.asString(),
                        response.getHeaders().getStringField(HttpHeader.CONTENT_LENGTH));
                lackrResponse.setHeader(HttpHeader.CONNECTION.asString(), "close");
                if (response.getHeaders().containsKey(HttpHeader.CACHE_CONTROL.asString()))
                    lackrResponse.setHeader(HttpHeader.CACHE_CONTROL.asString(),
                            response.getHeaders().getStringField(HttpHeader.CACHE_CONTROL));
            }

            @Override
            public void onContent(Response response, ByteBuffer content) {
                try {
                    while (content.hasRemaining())
                        lackrResponse.getOutputStream().write(content.get());
                } catch (IOException e) {
                    throw new RuntimeException("error in proxy: " + e);
                }
            }

            @Override
            public void onComplete(Result result) {
                try {
                    lackrResponse.flushBuffer();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                getRequest().getAsyncContext().complete();
                service.getGateway().getRunningRequestsHolder().dec();
            }
        });
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
            continuation.dispatch();
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
        log.debug("notifySubRequestDone pending: " + pendingCount.get());
        if (pendingCount.decrementAndGet() <= 0) {
            if (log.isDebugEnabled())
                log.debug("Gathered all fragments for " + rootUrl + " with " + backendExceptions.size() + " exceptions.");
            /*
            while (continuation.) {
                // System.err.println("finished early");
                // rare race condition where we get there (processing all done)
                // before the initial incoming query handling has been done
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    // I don't care
                }
            }
            */
            continuation.dispatch();
        }
    }

    public MustacheContext getMustacheContext() {
        return mustacheContext;
    }

    public BackendRequest getRootRequest() {
        return rootRequest;
    }

    public void addAncilliaryHeader(String k, String v) {
        ancillialiaryHeaders.put(k, v);
    }

    public Map<String, String> getAncilliaryHeaders() {
        return ancillialiaryHeaders;
    }

    public AtomicInteger[] getBackendRequestCounts() {
        return backendRequestCounts;
    }

    public ConcurrentHashMap<String, AtomicInteger> getBackendRequestEndpointsCounters() {
        return backendRequestEndpointsCounters;
    }

    public Locale getPreferredLocale() {
        String hostname = request.getHeader("Host");
        String langForHostname = hostname.split("[\\.:]")[0];
        if (langForHostname.equals("www") || langForHostname.equals("localhost"))
            langForHostname = "en";
        if (request.getLocale().getLanguage() == langForHostname)
            return request.getLocale();
        else
            return Locale.forLanguageTag(langForHostname);
    }
}
