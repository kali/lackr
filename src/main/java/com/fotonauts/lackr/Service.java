package com.fotonauts.lackr;

import java.io.IOException;

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
    
    protected HttpClient client;
    
    public HttpClient getClient() {
        return client;
    }

    @Override
    public final void doStart() throws Exception {
        client = new HttpClient();
        client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
        client.start();
        log.debug("Started client");
    }
    
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        LackrRequest state = (LackrRequest) request.getAttribute(LACKR_STATE_ATTRIBUTE);
        if(state == null) {
            state = new LackrRequest(this, request);
            request.setAttribute(LACKR_STATE_ATTRIBUTE, state);
            state.kick();
        } else {
            state.writeResponse(response);
        }
    }

}
