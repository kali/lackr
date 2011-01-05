package com.fotonauts.lackr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Service extends AbstractHandler {

	private String LACKR_STATE_ATTRIBUTE = "lackr.state.attribute";

	static Logger log = LoggerFactory.getLogger(Service.class);

	protected String backend = "http://localhost";

	protected HttpClient client;

	private List<SubstitutionEngine> substituers = new ArrayList<SubstitutionEngine>();

	public String getBackend() {
		return backend;
	}

	public void setBackend(String backend) {
		this.backend = backend;
	}

	public HttpClient getClient() {
		return client;
	}

	public void setClient(HttpClient client) {
		this.client = client;
	}

	public List<SubstitutionEngine> getSubstituers() {
		return substituers;
	}

	public void setSubstituers(List<SubstitutionEngine> substituers) {
		this.substituers = substituers;
	}

	@Override
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		LackrRequest state = (LackrRequest) request
				.getAttribute(LACKR_STATE_ATTRIBUTE);
		if (state == null) {
			log.debug("starting processing for: " + request.getRequestURL());
			state = new LackrRequest(this, request);
			request.setAttribute(LACKR_STATE_ATTRIBUTE, state);
			state.kick();
		} else {
			log.debug("resuming processing for: " + request.getRequestURL());
			state.writeResponse(response);
		}
	}

}
