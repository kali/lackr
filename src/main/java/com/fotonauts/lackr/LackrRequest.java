package com.fotonauts.lackr;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LackrRequest {

    static String[] headersToSkip = { "proxy-connection", "connection", "keep-alive", "transfer-encoding", "te", "trailer",
            "proxy-authorization", "proxy-authenticate", "upgrade", "content-length" };

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

    private Pattern pattern = Pattern.compile("<!--# include virtual=\"(.*?)\" -->");

    private Continuation continuation;

    LackrRequest(Service service, HttpServletRequest request) throws IOException {
        this.service = service;
        this.request = request;
        this.continuation = ContinuationSupport.getContinuation(request);
        this.fragmentsMap = Collections.synchronizedMap(new HashMap<String, LackrContentExchange>());
        this.pendingCount = new AtomicInteger(0);
        rootUrl = request.getPathInfo();
        log.debug("Starting to process " + rootUrl);
        continuation.suspend();
    }

    public void scheduleUpstreamRequest(String uri) throws IOException {
        ContentExchange exchange = new LackrContentExchange(this);
        log.debug("Requesting backend for " + uri);
        exchange.setURL("http://i3.testing.ftnz.net:2002" + uri);
        exchange.addRequestHeader("X-NGINX-SSI", "yes");
        this.pendingCount.incrementAndGet();
        for (@SuppressWarnings("unchecked")
        Enumeration e = request.getHeaderNames(); e.hasMoreElements();) {
            String header = (String) e.nextElement();
            if (!skipHeader(header)) {
                exchange.addRequestHeader(header, request.getHeader(header));
            }
        }

        service.getClient().send(exchange);
    }

    protected boolean parseable(String mimeType) {
        return mimeType.startsWith("text/html") || mimeType.startsWith("application/xml")
                || mimeType.startsWith("application/json") || mimeType.startsWith("application/atom+xml")
                || mimeType.startsWith("text/javascript") || mimeType.startsWith("application/x-mmtml")
                || mimeType.startsWith("application/x-mmtml+xml");
    }

    public void processIncomingResponse(LackrContentExchange lackrContentExchange) throws IOException {
        log.debug("processing response for " + lackrContentExchange.getURI());
        fragmentsMap.put(lackrContentExchange.getURI(), lackrContentExchange);
        if (parseable(lackrContentExchange.getResponseFields().getStringField("Content-Type"))) {
            try {
                String content = new String(lackrContentExchange.getResponseContentBytes(), "UTF-8");
                Matcher matcher = pattern.matcher(content);
                while (matcher.find()) {
                    scheduleUpstreamRequest(matcher.group(1));
                }
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (pendingCount.decrementAndGet() == 0) {
            log.debug("Gathered all fragments " + rootUrl);
            continuation.resume();
        }
    }

    public void writeResponse(HttpServletResponse response) throws IOException {
        LackrContentExchange root = fragmentsMap.get(rootUrl);
        for (@SuppressWarnings("unchecked")
        Enumeration names = root.getResponseFields().getFieldNames(); names.hasMoreElements();) {
            String name = (String) names.nextElement();
            if (!skipHeader(name)) {
                for (@SuppressWarnings("unchecked")
                Enumeration values = root.getResponseFields().getValues(name); values.hasMoreElements();) {
                    response.addHeader(name, (String) values.nextElement());

                }
            }
        }
        if (parseable(root.getResponseFields().getStringField("Content-Type"))) {
            try {
                StringBuilder content = new StringBuilder(new String(root.getResponseContentBytes(), "UTF-8"));
                boolean replacedSome = false;
                do {
                    replacedSome = false;
                    Matcher matcher = pattern.matcher(content);
                    while (matcher.find()) {
                        String fragment = new String(fragmentsMap.get(matcher.group(1)).getResponseContentBytes(), "UTF-8");
                        content.replace(matcher.start(0), matcher.end(0), fragment);
                        replacedSome = true;
                    }
                } while (replacedSome);
                byte[] bytes = content.toString().replaceAll("http://_A_S_S_E_T_S___P_A_T_H_",
                        "http://assets.cdn.testing.ftnz.net/picor/433f647735b32b89fd5ecb7dd1bc8951c41617c1").getBytes("UTF-8");
                response.getOutputStream().write(bytes);
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            response.getOutputStream().write(root.getResponseContentBytes());
        }
        log.debug("sending response for " + rootUrl);
    }

    public void kick() throws IOException {
        scheduleUpstreamRequest(rootUrl);
    }
}
