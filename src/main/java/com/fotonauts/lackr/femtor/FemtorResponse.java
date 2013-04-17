package com.fotonauts.lackr.femtor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;

public class FemtorResponse implements HttpServletResponse {

	public static final int NONE = 0, STREAM = 1, WRITER = 2;

	private int sc = 200;
	private ByteArrayOutputStream baos = new ByteArrayOutputStream();
	private PrintWriter _writer = null;
	private String _characterEncoding = "UTF-8";
	private Map<String, List<String>> headers = new HashMap<String, List<String>>();
	private int _outputState;

	public FemtorResponse(FemtorExchange exchange) {
		_outputState = NONE;
	}

	@Override
	public String getCharacterEncoding() {
		return _characterEncoding;
	}

	@Override
	public String getContentType() {
		return getHeaders().containsKey(HttpHeader.CONTENT_TYPE.asString()) ? getHeaders().get(HttpHeader.CONTENT_TYPE.asString()).get(0) : null;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {

		if (_outputState != NONE && _outputState != STREAM)
			throw new IllegalStateException("WRITER");

		_outputState = STREAM;

		return new ServletOutputStream() {

			@Override
			public void write(int b) throws IOException {
				baos.write(b);
			}
		};
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (_outputState != NONE && _outputState != WRITER)
			throw new IllegalStateException("STREAM");

		/* if there is no writer yet */
		if (_writer == null) {
			_writer = new PrintWriter(new OutputStreamWriter(baos, _characterEncoding));
		}
		_outputState = WRITER;
		return _writer;
	}

	@Override
	public void setCharacterEncoding(String charset) {
		this._characterEncoding = charset;
	}

	@Override
	public void setContentLength(int len) {
		// noop, we are buffered anyway
	}

	@Override
	public void setContentType(String type) {
		headers.put(HttpHeader.CONTENT_TYPE.asString(), Arrays.asList(new String[] { type }));
	}

	@Override
	public void setBufferSize(int size) {
		// noop irrelevant
	}

	@Override
	public int getBufferSize() {
		return Integer.MAX_VALUE;
	}

	@Override
	public void flushBuffer() throws IOException {
		if (_outputState == WRITER)
			getWriter().flush();
		if (_outputState == STREAM)
			baos.flush();
	}

	@Override
	public void resetBuffer() {
		baos.reset();
		_outputState = NONE;
	}

	@Override
	public boolean isCommitted() {
		return false;
	}

	@Override
	public void reset() {
		baos.reset();
		_outputState = NONE;
	}

	@Override
	public void setLocale(Locale loc) {
		throw new NotImplementedException();
	}

	@Override
	public Locale getLocale() {
		throw new NotImplementedException();
	}

	@Override
	public void addCookie(Cookie cookie) {

	}

	@Override
	public boolean containsHeader(String name) {
		return headers.containsKey(name);
	}

	@Override
	public String encodeURL(String url) {
		return url;
	}

	@Override
	public String encodeRedirectURL(String url) {
		return url;
	}

	@Override
	public String encodeUrl(String url) {
		return url;
	}

	@Override
	public String encodeRedirectUrl(String url) {
		return url;
	}

	@Override
	public void sendError(int sc, String msg) throws IOException {
		this.sc = sc;
	}

	@Override
	public void sendError(int sc) throws IOException {
		this.sc = sc;
	}

	@Override
	public void sendRedirect(String location) throws IOException {
		this.sc = HttpStatus.MOVED_TEMPORARILY_302;
		setHeader(HttpHeader.LOCATION.asString(), location);
	}

	@Override
	public void setDateHeader(String name, long date) {
		throw new NotImplementedException();
	}

	@Override
	public void addDateHeader(String name, long date) {
		throw new NotImplementedException();
	}

	@Override
	public void setHeader(String name, String value) {
		headers.put(name, Arrays.asList(new String[] { value }));
	}

	@Override
	public void addHeader(String name, String value) {
		if (headers.containsKey(name))
			headers.get(name).add(value);
		else
			setHeader(name, value);
	}

	@Override
	public void setIntHeader(String name, int value) {
		setHeader(name, Integer.toString(value));
	}

	@Override
	public void addIntHeader(String name, int value) {
		addHeader(name, Integer.toString(value));
	}

	@Override
	public void setStatus(int sc) {
		this.sc = sc;
	}

	@Override
	public void setStatus(int sc, String sm) {
		this.sc = sc;
	}

	public Map<String, List<String>> getHeaders() {
		return headers;
	}

	protected byte[] getContentBytes() {
		return baos.toByteArray();
	}

	public int getStatus() {
		return sc;
	}

    @Override
    public String getHeader(String arg0) {
        return headers.get(arg0) != null && headers.get(arg0).size() > 0 ? headers.get(arg0).get(0) : null;
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headers.keySet();
    }

    @Override
    public Collection<String> getHeaders(String arg0) {
        return headers.get(arg0) != null && headers.get(arg0).size() > 0 ? headers.get(arg0) : null;
    }

}
