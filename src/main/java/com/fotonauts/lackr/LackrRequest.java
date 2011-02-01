package com.fotonauts.lackr;

import com.mongodb.BasicDBObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static com.fotonauts.lackr.MongoLoggingKeys.*;
import javax.servlet.http.Cookie;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.HttpMethods;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

public class LackrRequest {

	static String[] headersToSkip = { "proxy-connection", "connection", "keep-alive", "transfer-encoding", "te",
	        "trailer", "proxy-authorization", "proxy-authenticate", "upgrade", "content-length" };

	private boolean skipHeader(String header) {
		for (String skip : headersToSkip) {
			if (skip.equals(header.toLowerCase()))
				return true;
		}
		return false;
	}

	Map<String, LackrContentExchange> fragmentsMap;

	AtomicInteger pendingCount;

	static Logger log = LoggerFactory.getLogger(LackrRequest.class);

	protected HttpServletRequest request;

	protected Service service;

	private String rootUrl;

	protected LackrContentExchange rootExchange;

	private Continuation continuation;

	protected List<Throwable> backendExceptions = Collections.synchronizedList(new ArrayList<Throwable>(5));

	protected BasicDBObject logLine;

	protected long startTimestamp;

	LackrRequest(Service service, HttpServletRequest request) throws IOException {
		this.service = service;
		this.request = request;
		this.continuation = ContinuationSupport.getContinuation(request);
		this.continuation.setTimeout(60 * 1000);
		this.fragmentsMap = Collections.synchronizedMap(new HashMap<String, LackrContentExchange>());
		this.pendingCount = new AtomicInteger(0);
		rootUrl = StringUtils.hasText(request.getQueryString()) ? request.getPathInfo() + '?'
		        + request.getQueryString() : request.getPathInfo();
		rootUrl = rootUrl.replace(" ", "%20");

		/* Prepare the log line */
		logLine = new BasicDBObject();
		logLine.put(FACILITY.getPrettyName(), "lackr");
		logLine.put(OPERATION_ID.getPrettyName(), request.getHeader("X-Ftn-Operationid"));
		logLine.put(REMOTE_ADDR.getPrettyName(), request.getHeader("X-Forwarded-For"));

		logLine.put(USER_AGENT.getPrettyName(), request.getHeader("User-Agent"));
		logLine.put(CLIENT_ID.getPrettyName(), request.getHeader("X-Ftn-User"));
		logLine.put(SESSION_ID.getPrettyName(), request.getHeader("X-Ftn-Session"));

		logLine.put(DATE.getPrettyName(), new Date().getTime());
		logLine.put(SSL.getPrettyName(), "true".equals(request.getHeader("X-Ftn-SSL")));

		logLine.put(HTTP_HOST.getPrettyName(), request.getServerName());
		logLine.put(METHOD.getPrettyName(), request.getMethod());
		logLine.put(PATH.getPrettyName(), request.getPathInfo());
		logLine.put(QUERY_PARMS.getPrettyName(), request.getQueryString());

		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				String cname = cookie.getName();
				if (cname.equals("uid")) {
					logLine.put(USER_ID.getPrettyName(), cookie.getValue());
				} else if (cname.equals("login_session")) {
					logLine.put(LOGIN_SESSION.getPrettyName(), cookie.getValue());
				}
			}
		}

		continuation.suspend();
	}

	public void scheduleUpstreamRequest(String uri, String method, byte[] body) throws IOException {
		LackrContentExchange exchange = new LackrContentExchange(this);
		if (rootExchange == null)
			rootExchange = exchange;

		exchange.setMethod(method);
		exchange.setURL(service.getBackend() + uri);
		exchange.addRequestHeader("X-NGINX-SSI", "yes");
		this.pendingCount.incrementAndGet();
		for (@SuppressWarnings("unchecked")
		Enumeration e = request.getHeaderNames(); e.hasMoreElements();) {
			String header = (String) e.nextElement();
			if (!skipHeader(header)) {
				exchange.addRequestHeader(header, request.getHeader(header));
			}
		}
		if (body != null) {
			exchange.setRequestContent(new ByteArrayBuffer(body));
			exchange.setRequestHeader("Content-Length", Integer.toString(body.length));
		}
		service.getClient().send(exchange);
	}

	public void processIncomingResponse(LackrContentExchange lackrContentExchange) throws IOException {
		log.debug("processing response for " + lackrContentExchange.getURI());

		fragmentsMap.put(lackrContentExchange.getURI(), lackrContentExchange);
		try {
			for (SubstitutionEngine s : service.getSubstituers())
				for (String sub : s.lookForSubqueries(lackrContentExchange))
					scheduleUpstreamRequest(sub, HttpMethods.GET, null);
		} catch (Throwable e) {
			e.printStackTrace();
			addBackendExceptions(e);
		}
		if (pendingCount.decrementAndGet() <= 0) {
			log.debug("Gathered all fragments " + rootUrl);
			continuation.resume();
		}
	}

	public void copyHeaders(HttpServletResponse response) {
		for (@SuppressWarnings("unchecked")
		Enumeration names = rootExchange.getResponseFields().getFieldNames(); names.hasMoreElements();) {
			String name = (String) names.nextElement();
			if (!skipHeader(name)) {
				for (@SuppressWarnings("unchecked")
				Enumeration values = rootExchange.getResponseFields().getValues(name); values.hasMoreElements();) {
					String value = (String) values.nextElement();
					response.addHeader(name, value);
				}
			}
		}
	}

	public void writeResponse(HttpServletResponse response) throws IOException {

		long duration = (System.currentTimeMillis() - startTimestamp) / 1000;
		logLine.put(ELAPSED.getPrettyName(), duration);

		try {
			if (pendingCount.get() > 0 || !backendExceptions.isEmpty()) {
				writeErrorResponse(response);
			} else {
				writeSuccessResponse(response);
			}
		} catch (IOException writeResponseException) {
			logLine.put(STATUS.getPrettyName(), "500");
			logLine.put(DATA.getPrettyName(), writeResponseException.getMessage());
			try {
				service.logCollection.save(logLine);
			} catch (Exception ex) {
				log.error("Unable to log data in mongo: " + ex.getMessage());
			}
			throw writeResponseException;
		}

	}

	public void writeErrorResponse(HttpServletResponse response) throws IOException {
		response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
		response.setContentType("text/plain");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		for (Throwable t : backendExceptions)
			t.printStackTrace(ps);
		ps.flush();
		response.setContentLength(baos.size());
		response.getOutputStream().write(baos.toByteArray());

		logLine.put(STATUS.getPrettyName(), Integer.toString(HttpServletResponse.SC_BAD_GATEWAY));
		logLine.put(DATA.getPrettyName(), baos.toByteArray());
		try {
			service.logCollection.save(logLine);
		} catch (Exception ex) {
			log.error("Unable to log data in mongo: " + ex.getMessage());
		}

	}

	public byte[] processContent(byte[] content) {
		byte[] previousContent = null;
		while (previousContent == null || !Arrays.equals(content, previousContent)) {
			previousContent = content.clone();
			for (SubstitutionEngine s : service.getSubstituers())
				content = s.generateContent(this, content);
			if (content == null)
				throw new RuntimeException("WTF just happened ?");
		}
		return content;
	}

	public void writeSuccessResponse(HttpServletResponse response) throws IOException {
		response.setStatus(rootExchange.getResponseStatus());
		copyHeaders(response);
		log.debug("writing response for " + rootExchange.getURI());
		byte[] content = rootExchange.getResponseContentBytes();
		if (content != null) {
			content = processContent(content);
			String etag = generateEtag(content);
			response.setHeader(HttpHeaders.ETAG, etag);
			log.debug("etag: " + etag);
			log.debug("if-none-match: " + request.getHeader(HttpHeaders.IF_NONE_MATCH));
			if (rootExchange.getResponseStatus() == HttpStatus.OK_200
			        && etag.equals(request.getHeader(HttpHeaders.IF_NONE_MATCH))) {
				response.setStatus(HttpStatus.NOT_MODIFIED_304);
				response.flushBuffer(); // force commiting
			} else {
				response.setContentLength(content.length);
				response.getOutputStream().write(content);
			}
			logLine.put(SIZE.getPrettyName(), content.length);
		} else {
			response.flushBuffer(); // force commiting
		}
		logLine.put(STATUS.getPrettyName(), Integer.toString(rootExchange.getResponseStatus()));
		try {
			service.logCollection.save(logLine);
		} catch (Exception ex) {
			log.error("Unable to log data in mongo: " + ex.getMessage());
		}

	}

	private String generateEtag(byte[] content) {
		MessageDigest m = null;
		try {
			m = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// nope.
		}
		m.update(content, 0, content.length);
		return '"' + new BigInteger(1, m.digest()).toString(16) + '"';
	}

	public void kick() {
		startTimestamp = System.currentTimeMillis();
		try {
			byte[] body = null;
			if (request.getContentLength() > 0)
				body = FileCopyUtils.copyToByteArray(request.getInputStream());
			scheduleUpstreamRequest(rootUrl, request.getMethod(), body);
		} catch (Throwable e) {
			log.debug("in kick() error handler");
			backendExceptions.add(e);
			continuation.resume();
		}
	}

	public void addBackendExceptions(Throwable x) {
		backendExceptions.add(x);
	}
}
