package com.fotonauts.lackr;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jetty handler implementing lackr core http proxy features.
 * 
 * <p>Asynchronously accepts incoming requests, sends them to its mandatory {@link Backend},
 * then send it back.
 * 
 * <p>Demo "payload" feature being Interpolr, it's unlikely there is a real-world use case for
 * this {@link BaseProxy}, but it is helpful to have this seggregated for testing purposes.
 * 
 * 
 * <h2>query workflow</h2>
 * 
 * <h3>incoming request</h3>
 * <li>{@link #handle(String, Request, HttpServletRequest, HttpServletResponse)} receive jetty request 
 *      a first time and {@link #createFrontendRequest(HttpServletRequest)}. The FrontendRequest is the
 *      lackr object that hold all intermediary state of a request being processed. It is set as an
 *      attribute of the jetty request({@link #LACKR_STATE_ATTRIBUTE}), and 
 *      {@link #handleInitial(BaseFrontendRequest)} is called.
 * <li>{@link #handleInitial(BaseFrontendRequest)} creates a backend request matching the incoming request
 *     and calls ({@link #scheduleBackendRequest(LackrBackendRequest)} on it
 * <li>{@link #scheduleBackendRequest(LackrBackendRequest)} enqueue a task in the executor that will basically
 *     call {@link LackrBackendRequest#start()}
 * 
 * <h3>asynchronous workflow</h3>
 * <li>the executor threads starts executing code from {@link Backend} and {@link LackrBackendRequest} that will
 *     eventually reach the {@link CompletionListener} set in the {@link LackrBackendRequest}.
 * <li>{@link #onBackendRequestDone(BaseFrontendRequest)} is called, bouncing to {@link BaseFrontendRequest#reDispatchOnce()}
 * <li>the container is signaled that the request is ready to be handled again
 * 
 * <h3>writing response</h3>
 * <li>
 * 
 * @author kali
 *
 */
public class BaseProxy extends AbstractLifeCycle {

    static Logger log = LoggerFactory.getLogger(BaseProxy.class);

    public static enum EtagMode {
        FORWARD, DISCARD, CONTENT_SUM
    }

    private String LACKR_STATE_ATTRIBUTE = "lackr.state.attribute";

    private int timeout;

    private Backend backend;

    private ExecutorService executor;

    private EtagMode etagMode = EtagMode.FORWARD;

    private boolean manageIfNoneMatch;

    public BaseProxy() {
    }

    /**
     * BaseProxy implementation creates a {@link BaseFrontendRequest}.
     * 
     * @param request incoming request
     * @return a BaseFrontendRequest
     */
    protected BaseFrontendRequest createFrontendRequest(HttpServletRequest request) {
        return new BaseFrontendRequest(this, request);
    }

    /**
     * Initial handling of the {@link BaseFrontendRequest}.
     * 
     * Creates a matching {@link LackrBackendRequest}, and call 
     * {@link #scheduleBackendRequest(LackrBackendRequest)} to enqueue it to the executor.
     * 
     * @param frontendReq the request to start processing
     */
    protected void handleInitial(final BaseFrontendRequest frontendReq) {
        try {
            LackrBackendRequest rootRequest = createBackendRequest(frontendReq);
            scheduleBackendRequest(rootRequest);
        } catch (Throwable e) {
            log.debug("in kick() error handler: " + e);
            frontendReq.getErrors().add(LackrPresentableError.fromThrowable(e));
            frontendReq.getContinuation().dispatch();
        }
    }

    /**
     * Creates a {@link LackrBackendRequest} representing a {@link BaseFrontendRequest}.
     * 
     * <p>A completion listener is set to trampoline to {@link #onBackendRequestDone(BaseFrontendRequest)}.
     * 
     * @param frontendReq the recently created {@link BaseFrontendRequest}.
     * @return a new {@link LackrBackendRequest}.
     * @throws IOException
     */
    protected LackrBackendRequest createBackendRequest(final BaseFrontendRequest frontendReq) throws IOException {
        byte[] body = null;
        if (frontendReq.getIncomingServletRequest().getContentLength() > 0)
            body = IO.readBytes(frontendReq.getIncomingServletRequest().getInputStream());

        LackrBackendRequest rootRequest = new LackrBackendRequest(frontendReq, frontendReq.getIncomingServletRequest().getMethod(),
                getPathAndQuery(frontendReq.getIncomingServletRequest()), body, buildHttpFields(frontendReq), null,
                new CompletionListener() {

                    @Override
                    public void fail(Throwable t) {
                        frontendReq.addBackendExceptions(t);
                        onBackendRequestDone(frontendReq);
                    }

                    @Override
                    public void complete() {
                        onBackendRequestDone(frontendReq);
                    }
                });
        frontendReq.setBackendRequest(rootRequest);
        return rootRequest;
    }

    /**
     * Enqueue a backend request in the executor.
     * 
     * The enqueued task will call {@link LackrBackendRequest#start()}.
     * 
     * @param request the {@link LackrBackendRequest} to enqueue.
     */
    protected void scheduleBackendRequest(final LackrBackendRequest request) {
        getExecutor().execute(new Runnable() {

            @Override
            public void run() {
                try {
                    request.start();
                } catch (Throwable e) {
                    e.printStackTrace(System.err);
                    request.getFrontendRequest().addError(LackrPresentableError.fromThrowable(e));
                }
            }

        });
    }

    /**
     * Called in the executor thread when the asynchronous part is over.
     * 
     * Default implementation signals the container for re-scheduling of the request, in the
     * form of another call on {@link #handle(String, Request, HttpServletRequest, HttpServletResponse)}.
     * 
     * @param frontendRequest
     */
    protected void onBackendRequestDone(BaseFrontendRequest frontendRequest) {
        log.debug("Processing done, consider re-dispatching the http thread.");
        frontendRequest.reDispatchOnce();
    }

    /**
     * Called on second handling of the request.
     * 
     * <p>Choose between {@link #writeErrorResponse(BaseFrontendRequest, HttpServletResponse)}
     * and {@link #writeSuccessResponse(BaseFrontendRequest, HttpServletResponse)} based on the emptyness of
     * {@link BaseFrontendRequest#getErrors()}.
     * 
     * @param frontendRequest the lackr representation of the request
     * @param response the response object from the container
     * @throws IOException 
     */
    protected void writeResponse(BaseFrontendRequest frontendRequest, HttpServletResponse response) throws IOException {
        try {
            if (!frontendRequest.getErrors().isEmpty()) {
                log.debug("Writting error response for {}", frontendRequest);
                writeErrorResponse(frontendRequest, response);
            } else {
                log.debug("Writting success response for {}", frontendRequest);
                writeSuccessResponse(frontendRequest, response);
            }
        } catch (IOException writeResponseException) {
            throw writeResponseException;
        } finally {
        }
    }

    /**
     * Writes the body of the request response to a stream.
     * 
     * <p>This implementation just grab the buffer from the main request and writes it to the request.
     * 
     * <p>Can (and will) be called more than once.
     * 
     * @param req
     * @param out
     * @throws IOException
     */
    protected void writeContentTo(BaseFrontendRequest req, OutputStream out) throws IOException {
        out.write(req.getBackendRequest().getExchange().getResponse().getBodyBytes());
    }

    /**
     * Write a success response, dealing with ETAG/ifNoneMatch support.
     * 
     * @param state
     * @param response
     * @throws IOException
     */
    protected void writeSuccessResponse(BaseFrontendRequest state, HttpServletResponse response) throws IOException {
        LackrBackendExchange rootExchange = state.getBackendRequest().getExchange();

        String etag = getETag(state);
        log.debug("etag for response for {} is {} ({})", state.getBackendRequest(), etag, getEtagMode());

        // IF NONE MATCH
        if (getManageIfNoneMatch() && state.getIncomingServletRequest().getMethod().equals("GET")
                && rootExchange.getResponse().getStatus() == HttpStatus.OK_200 && etag != null) {
            String ifNoneMatch = state.getIncomingServletRequest().getHeader(HttpHeader.IF_NONE_MATCH.asString());
            if (etag.equals(ifNoneMatch)) {
                log.debug("writing 304 response for {}", state.getBackendRequest());
                // spec says no content-type, no content-length with 304
                response.setStatus(HttpStatus.NOT_MODIFIED_304);
                copyResponseHeaders(state, response);
                response.flushBuffer(); // force commiting
                return;
            }
        }

        log.debug("writing {} response for {}", rootExchange.getResponse().getStatus(), state.getBackendRequest());
        response.setStatus(rootExchange.getResponse().getStatus());
        copyResponseHeaders(state, response);

        // ETAG
        if (getEtagMode() != EtagMode.DISCARD && etag != null)
            response.setHeader(HttpHeader.ETAG.asString(), etag);

        // CONTENT-TYPE AND CONTENT-LENTH
        int contentLength = state.getContentLength();
        if (contentLength > 0)
            response.setHeader(HttpHeader.CONTENT_TYPE.asString(), Integer.toString(contentLength));
        if (rootExchange.getResponse().getHeader(HttpHeader.CONTENT_TYPE.asString()) != null)
            response.setHeader(HttpHeader.CONTENT_TYPE.asString(),
                    rootExchange.getResponse().getHeader(HttpHeader.CONTENT_TYPE.asString()));

        // CONTENT
        if (!state.getIncomingServletRequest().getMethod().equals("HEAD"))
            writeContentTo(state, response.getOutputStream());

        if (rootExchange.getResponse().getStatus() >= 500 && rootExchange.getResponse().getBodyBytes() != null) {
            state.getIncomingServletRequest().setAttribute("LACKR_ERROR_MESSAGE",
                    new String(rootExchange.getResponse().getBodyBytes()));
        }

        response.flushBuffer(); // force commiting
    }

    /**
     * Formats the errors accumulated in the {@link BaseFrontendRequest}.
     * 
     * Will also set the plain text error message in the "LACKR_ERROR_MESSAGE" attribute of the request.
     * 
     * @param req
     * @param response
     * @throws IOException
     */
    protected void writeErrorResponse(BaseFrontendRequest req, HttpServletResponse response) throws IOException {
        log.debug("Writing error response for " + req.getBackendRequest().getQuery());

        response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
        response.setContentType("text/plain");
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (LackrPresentableError t : req.getErrors()) {
            if (req.getErrors().size() > 1)
                sb.append(String.format("Error #%d/%d: ", ++i, req.getErrors().size()));
            sb.append(t.getMessage());
            sb.append("\n\n\n");
        }
        String s = sb.toString();
        byte[] ba = s.getBytes("UTF-8");
        response.setContentLength(ba.length);
        response.getOutputStream().write(ba);
        req.getIncomingServletRequest().setAttribute("LACKR_ERROR_MESSAGE", s);

        response.flushBuffer();
    }

    //--------------------------------------------------------------------------------------
    // Etag management
    protected String generateETagForContent(BaseFrontendRequest req) {
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
            writeContentTo(req, dos);
            dos.flush();
            dos.close();
        } catch (IOException e) {
            // not possible with these streams
        }
        return '"' + new BigInteger(1, m.digest()).toString(16) + '"';
    }

    protected String getETag(BaseFrontendRequest req) {
        switch (getEtagMode()) {
        case CONTENT_SUM:
            return generateETagForContent(req);
        case DISCARD:
            return null;
        case FORWARD:
            return req.getBackendRequest().getExchange().getResponse().getHeader(HttpHeader.ETAG.asString());
        }
        return null;
    }

    /**
     * Entry point for requests.
     * 
     * <p>Will typically be called twice for each query to support asynchronous mode.
     * 
     */
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        BaseFrontendRequest state = (BaseFrontendRequest) request.getAttribute(LACKR_STATE_ATTRIBUTE);
        if (state == null) {
            log.debug("starting processing for: " + request.getRequestURL());
            state = createFrontendRequest(request);
            request.setAttribute(LACKR_STATE_ATTRIBUTE, state);
            handleInitial(state);
        } else {
            log.debug("resuming processing for: " + request.getRequestURL());
            writeResponse(state, response);
        }

    }

    //--------------------------------------------------------------------------------------
    // jetty-based LifeCycle 
    @Override
    protected void doStart() throws Exception {
        backend.start();
        setExecutor(Executors.newFixedThreadPool(64));
    }

    @Override
    public void doStop() throws Exception {
        log.debug("Stopping, thread count: " + Thread.getAllStackTraces().size());
        backend.stop();
        getExecutor().shutdown();
        getExecutor().awaitTermination(1, TimeUnit.SECONDS);
        log.debug("Stopped thread count: " + Thread.getAllStackTraces().size());
    }

    //------------------------------------------------------------
    // header triaging
    static String[] headersToSkip = { "proxy-connection", "connection", "keep-alive", "transfer-encoding", "te", "trailer",
            "proxy-authorization", "proxy-authenticate", "upgrade", "content-length", "content-type", "if-modified-since",
            "if-none-match", "range", "accept-ranges", "etag" };

    public boolean skipHeader(String header) {
        for (String skip : headersToSkip) {
            if (skip.equals(header.toLowerCase()))
                return true;
        }
        return false;
    }

    protected HttpFields buildHttpFields(BaseFrontendRequest frontendReq) {
        HttpFields fields = new HttpFields();
        for (Enumeration<?> e = frontendReq.getIncomingServletRequest().getHeaderNames(); e.hasMoreElements();) {
            String header = (String) e.nextElement();
            if (!skipHeader(header)) {
                fields.add(header, frontendReq.getIncomingServletRequest().getHeader(header));
            }
        }
        if (frontendReq.getIncomingServletRequest().getContentLength() > 0
                && frontendReq.getIncomingServletRequest().getContentType() != null)
            fields.add(HttpHeader.CONTENT_TYPE.toString(), frontendReq.getIncomingServletRequest().getContentType());

        return fields;
    }

    public void copyResponseHeaders(BaseFrontendRequest frontendReq, HttpServletResponse response) {
        for (String name : frontendReq.getBackendRequest().getExchange().getResponse().getHeaderNames()) {
            if (!skipHeader(name)) {
                for (String value : frontendReq.getBackendRequest().getExchange().getResponse().getHeaderValues(name))
                    response.addHeader(name, value);
            }
        }
        if (frontendReq.getBackendRequest().getExchange().getResponse().getHeader(HttpHeader.CONTENT_TYPE.asString()) != null)
            response.addHeader(HttpHeader.CONTENT_TYPE.asString(), frontendReq.getBackendRequest().getExchange().getResponse()
                    .getHeader(HttpHeader.CONTENT_TYPE.asString()));
    }

    //--------------------------------------------------------------------------------------
    // configuration
    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getTimeout() {
        return timeout;
    }

    public Backend getBackend() {
        return backend;
    }

    public void setBackend(Backend backend) {
        this.backend = backend;
    }

    public void setEtagMode(EtagMode etagMode) {
        this.etagMode = etagMode;
    }

    public EtagMode getEtagMode() {
        return etagMode;
    }

    public boolean getManageIfNoneMatch() {
        return manageIfNoneMatch;
    }

    public void setManageIfNoneMatch(boolean manageIfNoneMatch) {
        this.manageIfNoneMatch = manageIfNoneMatch;
    }

    public static String getPathAndQuery(HttpServletRequest request) {

        URI uri = null;
        try {
            uri = new URI(null, null, request.getPathInfo(), null);
        } catch (URISyntaxException e) {
            throw new RuntimeException("invalid URL");
        }
        String url = (request.getQueryString() != null && request.getQueryString() != "") ? uri.toASCIIString() + '?'
                + request.getQueryString() : uri.toASCIIString();
        url = url.replace(" ", "%20");
        return url;
    }

}
