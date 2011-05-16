package com.fotonauts.lackr;

import static com.fotonauts.lackr.MongoLoggingKeys.DATA;
import static com.fotonauts.lackr.MongoLoggingKeys.DATE;
import static com.fotonauts.lackr.MongoLoggingKeys.ELAPSED;
import static com.fotonauts.lackr.MongoLoggingKeys.HTTP_HOST;
import static com.fotonauts.lackr.MongoLoggingKeys.METHOD;
import static com.fotonauts.lackr.MongoLoggingKeys.PATH;
import static com.fotonauts.lackr.MongoLoggingKeys.QUERY_PARMS;
import static com.fotonauts.lackr.MongoLoggingKeys.SIZE;
import static com.fotonauts.lackr.MongoLoggingKeys.STATUS;

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
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import com.fotonauts.lackr.hashring.HashRing.NotAvailableException;
import com.fotonauts.lackr.interpolr.Document;
import com.fotonauts.lackr.mustache.MustacheContext;
import com.mongodb.BasicDBObject;

public class LackrFrontendRequest {
	static String[] headersToSkip = { "proxy-connection", "connection", "keep-alive", "transfer-encoding", "te",
	        "trailer", "proxy-authorization", "proxy-authenticate", "upgrade", "content-length" };

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

	protected LackrBackendExchange rootExchange;

	private Continuation continuation;

	protected List<Throwable> backendExceptions = Collections.synchronizedList(new ArrayList<Throwable>(5));

	protected BasicDBObject logLine;

	protected long startTimestamp;

	private UserAgent userAgent;

	private MustacheContext mustacheContext;

	LackrFrontendRequest(Service service, HttpServletRequest request) throws IOException {
		this.service = service;
		this.request = request;
		this.mustacheContext = new MustacheContext();
		this.continuation = ContinuationSupport.getContinuation(request);
		this.continuation.setTimeout(getService().getTimeout() * 1000);
		this.pendingCount = new AtomicInteger(0);
		URI uri = null;
		try {
			uri = new URI(null, null, request.getPathInfo(), null);
		} catch (URISyntaxException e) {
			throw new RuntimeException("invalid URL");
		}
		rootUrl = StringUtils.hasText(request.getQueryString()) ? uri.toASCIIString() + '?' + request.getQueryString()
		        : uri.toASCIIString();
		rootUrl = rootUrl.replace(" ", "%20");

		logLine = Service.accessLogLineTemplate(request, "lackr-front");

		logLine.put(HTTP_HOST.getPrettyName(), request.getHeader("Host"));
		logLine.put(METHOD.getPrettyName(), request.getMethod());
		logLine.put(PATH.getPrettyName(), request.getPathInfo());
		logLine.put(QUERY_PARMS.getPrettyName(), request.getQueryString());

		this.userAgent = new UserAgent(request.getHeader(HttpHeaders.USER_AGENT));

		continuation.suspend();
	}

	public LackrBackendExchange scheduleUpstreamRequest(BackendRequest spec) throws NotAvailableException {
		final LackrBackendExchange exchange = getService().getClient().createExchange(spec);
		if (rootExchange == null)
			rootExchange = exchange;
		this.pendingCount.incrementAndGet();
		getService().getExecutor().execute(new Runnable() {

			@Override
			public void run() {
				try {
					exchange.start();
				} catch (IOException e) {
					addBackendExceptions(e);
				} catch (NotAvailableException e) {
					addBackendExceptions(e);
				}
			}
		});
		return exchange;
	}

	UserAgent getUserAgent() {
		return userAgent;
	}

	public void copyHeaders(HttpServletResponse response) {
		for (String name : rootExchange.getResponseHeaderNames()) {
			if (!skipHeader(name)) {
				for (String value : rootExchange.getResponseHeaderValues(name))
					response.addHeader(name, value);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void preflightCheck() {
		getMustacheContext().checkAndCompileAll((List) backendExceptions);
		if (rootExchange.getParsedDocument() != null) {
			rootExchange.getParsedDocument().check((List) backendExceptions);
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
			service.log(logLine);
		}
	}

	public void writeErrorResponse(HttpServletResponse response) throws IOException {
		response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
		response.setContentType("text/plain");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		for (Throwable t : backendExceptions) {
			if (t instanceof LackrPresentableError) {
	            LackrPresentableError error = (LackrPresentableError) t;
	            ps.append(error.getMessage());
            } else 
            	t.printStackTrace(ps);
		}
		ps.flush();
		response.setContentLength(baos.size());
		response.getOutputStream().write(baos.toByteArray());

		logLine.put(STATUS.getPrettyName(), Integer.toString(HttpServletResponse.SC_BAD_GATEWAY));
		logLine.put(DATA.getPrettyName(), baos.toByteArray());
		getService().rapportrException(request, new String(baos.toByteArray(), "UTF-8"));
	}

	public void writeSuccessResponse(HttpServletResponse response) throws IOException {
		response.setStatus(rootExchange.getResponseStatus());
		copyHeaders(response);
		log.debug("writing success response for " + rootExchange.getBackendRequest().getQuery());
		if (rootExchange.getParsedDocument().length() > 0) {
			String etag = generateEtag(rootExchange.getParsedDocument());
			response.setHeader(HttpHeaders.ETAG, etag);
			if (log.isDebugEnabled()) {
				log.debug("etag: " + etag);
				log.debug("if-none-match: " + request.getHeader(HttpHeaders.IF_NONE_MATCH));
			}
			if (rootExchange.getResponseStatus() == HttpStatus.OK_200
			        && etag.equals(request.getHeader(HttpHeaders.IF_NONE_MATCH))) {
				response.setStatus(HttpStatus.NOT_MODIFIED_304);
				response.setHeader("Status", "304 Not Modified");
				response.flushBuffer(); // force commiting
				logLine.put(STATUS.getPrettyName(), Integer.toString(HttpStatus.NOT_MODIFIED_304));
			} else {
				logLine.put(STATUS.getPrettyName(), Integer.toString(rootExchange.getResponseStatus()));
				response.setContentLength(rootExchange.getParsedDocument().length());
				if (request.getMethod() != "HEAD")
					rootExchange.getParsedDocument().writeTo(response.getOutputStream());
				response.flushBuffer();
			}
			logLine.put(SIZE.getPrettyName(), rootExchange.getParsedDocument().length());
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
			BackendRequest spec = new BackendRequest(this, request.getMethod() == "HEAD" ? "GET" : request.getMethod(),
			        rootUrl, null, 0, null, body);
			scheduleUpstreamRequest(spec);
		} catch (Throwable e) {
			log.debug("in kick() error handler: " + e);
			backendExceptions.add(e);
			continuation.resume();
		}
	}

	public void addBackendExceptions(Throwable x) {
		backendExceptions.add(x);
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
				log.debug("Gathered all fragments for " + rootUrl + " with " + backendExceptions.size()
				        + " exceptions.");
			continuation.resume();
		}
	}

	public MustacheContext getMustacheContext() {
		return mustacheContext;
    }
}
