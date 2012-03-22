package com.fotonauts.lackr.femtor;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.fotonauts.lackr.BackendRequest;

public class FemtorRequest extends HttpServletRequestWrapper {

	BackendRequest request;
	private Map<String, List<String>> headers = new HashMap<String, List<String>>();

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
		return request.getQuery();
	}

	public void addHeader(String name, String value) {
		if (headers.containsKey(name))
			headers.get(name).add(value);
		else
			headers.put(name, Arrays.asList(value));
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
