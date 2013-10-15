package com.fotonauts.lackr.backend.inprocess;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;

import com.fotonauts.lackr.backend.LackrBackendRequest;

public class FemtorRequest extends HttpServletRequestWrapper {

    LackrBackendRequest request;
    private MultiMap<String> params;
    private BufferedReader reader;
    private ServletInputStream inputStream;

    public FemtorRequest(HttpServletRequest httpServletRequest, LackrBackendRequest request) {
        super(httpServletRequest);
        this.request = request;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (inputStream != null)
            throw new java.lang.IllegalStateException("Already streaming as an input stream");
        if (reader == null)
            reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(request.getBody()), "UTF-8"));
        return reader;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (reader != null)
            throw new java.lang.IllegalStateException("Already streaming as a reader");
        if (inputStream == null)
            inputStream = new ServletInputStream() {

                ByteArrayInputStream is = new ByteArrayInputStream(request.getBody());

                @Override
                public int read() throws IOException {
                    return is.read();
                }
            };
        return inputStream;
    }

    @Override
    public String getMethod() {
        return request.getMethod();
    }

    @Override
    public String getPathInfo() {
        return request.getPath();
    }

    @Override
    public String getQueryString() {
        return request.getParams();
    }

    @Override
    public String getRequestURI() {
        return request.getPath();
    }

    protected MultiMap<String> getParams() {
        if (params == null) {
            params = new MultiMap<String>();
            UrlEncoded.decodeTo(getQueryString(), params, "UTF-8", 100);
        }
        return params;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Enumeration getParameterNames() {
        if (request.getFrontendRequest().getRootRequest() == request)
            return super.getParameterNames();
        else
            return Collections.enumeration(getParams().keySet());
    }

    @Override
    public String getParameter(String name) {
        if (request.getFrontendRequest().getRootRequest() == request)
            return super.getParameter(name);
        else
            return getParams().getString(name);
    }

    @Override
    public String[] getParameterValues(String name) {
        if (request.getFrontendRequest().getRootRequest() == request)
            return super.getParameterValues(name);
        else if (getParams() != null && getParams().getValues(name) != null)
            return (String[]) getParams().getValues(name).toArray(new String[0]);
        else
            return null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Map getParameterMap() {
        if (request.getFrontendRequest().getRootRequest() == request)
            return super.getParameterMap();
        else
            return getParams().toStringArrayMap();

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Enumeration getHeaderNames() {
        return request.getFields().getFieldNames();
    }

    @Override
    public String getHeader(String name) {
        return request.getFields().getStringField(name);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Enumeration getHeaders(String name) {
        return request.getFields().getValues(name);
    }
}
