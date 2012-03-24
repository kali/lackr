package com.fotonauts.lackr.femtor;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;

import com.fotonauts.lackr.BackendRequest;

public class FemtorRequest extends HttpServletRequestWrapper {

	BackendRequest request;
	private Map<String, List<String>> headers = new HashMap<String, List<String>>();
	private HttpURI jettyHttpURI;
    private MultiMap<String> params;

	public FemtorRequest(HttpServletRequest httpServletRequest, BackendRequest request) {
		super(httpServletRequest);
		this.request = request;
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

	public void addHeader(String name, String value) {
		if (headers.containsKey(name))
			headers.get(name).add(value);
		else
			headers.put(name, Arrays.asList(value));
	}

	protected MultiMap<String> getParams() {
	    if(params == null) {
	        params = new MultiMap<String>();
	        UrlEncoded.decodeTo(getQueryString(), params, "UTF-8");
	    }
	    return params;
	}
	
	@Override
	public Enumeration getParameterNames() {
        if(request.getFrontendRequest().getRootRequest() == request)
            return super.getParameterNames();
        else
            return Collections.enumeration(getParams().keySet());
	}
	
	@Override
	public String getParameter(String name) {
	    if(request.getFrontendRequest().getRootRequest() == request)
	        return super.getParameter(name);
	    else
	        return getParams().getString(name);
	}
	
	@Override
	public String[] getParameterValues(String name) {
        if(request.getFrontendRequest().getRootRequest() == request)
            return super.getParameterValues(name);
        else
	    return getParams().getValues(name).toArray(new String[0]);
	}
	
	@Override
	public Map getParameterMap() {
        if(request.getFrontendRequest().getRootRequest() == request)
            return super.getParameterMap();
        else
        return getParams().toStringArrayMap();
	    
	}
	
	@Override
	public Enumeration getHeaderNames() {
	    return Collections.enumeration(headers.keySet());
	}

	@Override
	public String getHeader(String name) {
		if (headers.containsKey(name))
			return headers.get(name).get(0);
		else
			return null;
	}

	@Override
	public Enumeration getHeaders(String name) {
		return Collections.enumeration(headers.containsKey(name) ? headers.get(name) : Collections.EMPTY_LIST);
	}
}
