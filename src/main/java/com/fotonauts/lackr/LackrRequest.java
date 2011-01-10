package com.fotonauts.lackr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.eclipse.jetty.http.HttpMethods;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.util.URIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

public class LackrRequest {

	static String[] headersToSkip = { "proxy-connection", "connection",
			"keep-alive", "transfer-encoding", "te", "trailer",
			"proxy-authorization", "proxy-authenticate", "upgrade",
			"content-length" };

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

	protected List<Throwable> backendExceptions = Collections
			.synchronizedList(new ArrayList<Throwable>(5));

	LackrRequest(Service service, HttpServletRequest request)
			throws IOException {
		this.service = service;
		this.request = request;
		this.continuation = ContinuationSupport.getContinuation(request);
		this.fragmentsMap = Collections
				.synchronizedMap(new HashMap<String, LackrContentExchange>());
		this.pendingCount = new AtomicInteger(0);
		rootUrl = StringUtils.hasText(request.getQueryString()) ? request
				.getPathInfo() + '?'
				+ request.getQueryString() : request.getPathInfo();
		log.debug("Starting to process " + rootUrl);
		continuation.suspend();
	}

	public void scheduleUpstreamRequest(String uri, String method, byte[] body)
			throws IOException {
		ContentExchange exchange = new LackrContentExchange(this);
		log.debug("Requesting backend for " + uri);
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
			exchange.setRequestHeader("Content-Length", Integer
					.toString(body.length));
		}
		service.getClient().send(exchange);
	}

	public void processIncomingResponse(
			LackrContentExchange lackrContentExchange) throws IOException {
		log.debug("processing response for " + lackrContentExchange.getURI());

		synchronized (this) {
			if (rootExchange == null)
				rootExchange = lackrContentExchange;
		}

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

	public void writeResponse(HttpServletResponse response) throws IOException {
		if (backendExceptions.isEmpty()) {
			writeSuccessResponse(response);
		} else {
			writeErrorResponse(response);
		}
	}

	public void writeErrorResponse(HttpServletResponse response)
			throws IOException {
		response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
		response.setContentType("text/plain");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		for (Throwable t : backendExceptions)
			t.printStackTrace(ps);
		ps.flush();
		response.setContentLength(baos.size());
		response.getOutputStream().write(baos.toByteArray());
	}

	public void writeSuccessResponse(HttpServletResponse response)
			throws IOException {
		response.setStatus(rootExchange.getResponseStatus());
		log.debug("writing response for " + rootExchange.getURI());
		for (@SuppressWarnings("unchecked")
		Enumeration names = rootExchange.getResponseFields().getFieldNames(); names
				.hasMoreElements();) {
			String name = (String) names.nextElement();
			if (!skipHeader(name)) {
				for (@SuppressWarnings("unchecked")
				Enumeration values = rootExchange.getResponseFields()
						.getValues(name); values.hasMoreElements();) {
					response.addHeader(name, (String) values.nextElement());

				}
			}
		}
		byte[] content = rootExchange.getResponseContentBytes();
		if (content != null) {
			byte[] previousContent = null;
			while (previousContent == null
					|| !Arrays.equals(content, previousContent)) {
				previousContent = content.clone();
				for (SubstitutionEngine s : service.getSubstituers())
					content = s.generateContent(this, content);
				if (content == null)
					throw new RuntimeException("WTF just happened ?");
			}
			response.setContentLength(content.length);
			response.getOutputStream().write(content);
		} else {
			response.flushBuffer(); // force commiting. possible bug in jetty
		}
	}

	public void kick() {
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
