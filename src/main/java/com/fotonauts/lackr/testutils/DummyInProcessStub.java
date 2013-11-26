package com.fotonauts.lackr.testutils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// FIXME consider fusioning with AppStubForESI... should be easy enough
// FIXME discard references to femtor (in path :/)
public class DummyInProcessStub implements Filter {

    static Logger log = LoggerFactory.getLogger(DummyInProcessStub.class);

	@Override
    public void init(FilterConfig filterConfig) throws ServletException {
	    log.debug("init()");
    }

	@Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest hr = (HttpServletRequest) request;
        log.debug("filtering: {}", hr);
		if(hr.getPathInfo().startsWith("/femtor/crash/servlet")) {
			throw new ServletException("catch me or you're dead.");
		} else if(hr.getPathInfo().startsWith("/femtor/crash/re")) {
            throw new RuntimeException("catch me or you're dead.");
        } else if(hr.getPathInfo().startsWith("/femtor/crash/error")) {
            throw new Error("catch me or you're dead.");
        } else if(hr.getPathInfo().startsWith("/femtor/asyncProxy")) {
            ((HttpServletResponse) response).setStatus(398);
            ((HttpServletResponse) response).setHeader("Location", "http://localhost:" + ((HttpServletRequest) request).getParameter("lackrPort") + "/femtor");
            response.flushBuffer();
		} else if(hr.getPathInfo().startsWith("/femtor/dumpwrapper")) {
			response.setContentType("text/html");
			response.getWriter().println("<!--# include virtual=\"/femtor/dump?tut=pouet\" -->");
			response.flushBuffer();
        } else if(hr.getPathInfo().startsWith("/femtor/esiToInvalidUrl")) {
            response.setContentType("text/html");
            response.getWriter().println("<!--# include virtual=\"/femtor/dump\\\" -->");
            response.flushBuffer();
		} else if(hr.getPathInfo().startsWith("/femtor/dump")) {
			response.getWriter().println("Hi from dummy femtor");
			response.getWriter().println("method: " + hr.getMethod());
			response.getWriter().println("pathInfo: " + hr.getPathInfo());
			response.getWriter().println("getQueryString: " + hr.getQueryString());
			response.getWriter().println("getRequestURI: " + hr.getRequestURI());
            response.getWriter().println("X-Ftn-OperationId: " + hr.getHeader("X-Ftn-OperationId"));
            response.getWriter().println("x-ftn-operationid: " + hr.getHeader("x-ftn-operationid"));
			String parameters[] = (String[]) Collections.list(hr.getParameterNames()).toArray(new String[] {});
			Arrays.sort(parameters);
			response.getWriter().println("parameterNames: " + Arrays.toString(parameters));
			response.flushBuffer();
        } else if(hr.getPathInfo().startsWith("/rewrite")) {
            ((HttpServletResponse) response).setStatus(399);
            ((HttpServletResponse) response).setHeader("Location", "/femtor");
            response.flushBuffer();
		} else if(hr.getPathInfo().startsWith("/echobody")) {
			char[] buffer = new char[4096];
			int len = request.getReader().read(buffer);
			response.getWriter().write(buffer, 0, len);
			response.flushBuffer();
		} else if(hr.getPathInfo().startsWith("/femtor")) {
			response.getWriter().println("Hi from dummy femtor");
			response.flushBuffer();
		} else {
			((HttpServletResponse) response).sendError(501);
		}
    }

	@Override
    public void destroy() {
    }

}
