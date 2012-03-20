package com.fotonauts.lackr;


import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import com.fotonauts.commons.RapportrService;
import com.fotonauts.lackr.interpolr.Interpolr;


public class Service extends AbstractHandler {

    private String LACKR_STATE_ATTRIBUTE = "lackr.state.attribute";
    static Logger log = LoggerFactory.getLogger(Service.class);

    private int timeout;
    
    protected RapportrService rapportr;

    protected Interpolr interpolr;

    public Service() {
    }

    public Interpolr getInterpolr() {
        return interpolr;
    }

    @Required
    public void setInterpolr(Interpolr interpolr) {
        this.interpolr = interpolr;
    }

    private Backend[] backends;
    
    private Executor executor;
    private String femtorBackend;
    private ObjectMapper objectMapper = new ObjectMapper();
    @Override
    protected void doStart() throws Exception {
        setExecutor(Executors.newFixedThreadPool(16));
        super.doStart();
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        if(request.getRequestURI().equals("/_lackr_status")) {
            handleStatusQuery(target, baseRequest, request, response);
            return;
        }
        LackrFrontendRequest state = (LackrFrontendRequest) request.getAttribute(LACKR_STATE_ATTRIBUTE);
        if (state == null) {
            log.debug("starting processing for: " + request.getRequestURL());
            state = new LackrFrontendRequest(this, request);
            request.setAttribute(LACKR_STATE_ATTRIBUTE, state);
            state.kick();
        } else {
            log.debug("resuming processing for: " + request.getRequestURL());
            state.writeResponse(response);
        }
    }
    
    protected void handleStatusQuery(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
    	/*
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/plain");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        Map<Host, Long> weights = new HashMap<Host,Long>();
        for(Host h: getRing().getHosts())
        	weights.put(h, 0L);
        Entry<Integer, Host> previous = null;
        for(Entry<Integer, Host> e : getRing().getRing().entrySet()) {
        	if(previous != null)
            	weights.put(previous.getValue(), weights.get(previous.getValue()) + e.getKey() - previous.getKey());
        	ps.format("ring-boundary\t%08x\t\n", e.getKey(), e.getValue().getHostname());
        	previous = e;
        }
    	weights.put(previous.getValue(), weights.get(previous.getValue()) + (getRing().getRing().firstKey() - Integer.MIN_VALUE));
    	weights.put(previous.getValue(), weights.get(previous.getValue()) + (Integer.MAX_VALUE - getRing().getRing().lastKey()));
        for(Host h: getRing().getHosts()) {
            ps.format("picor-ring-weight\t%s\t%s\t%d\n", h.getHostname(), h.isUp() ? "UP" : "DOWN", weights.get(h));
        }
        ps.println();
        ps.flush();
        
        response.setContentLength(baos.size());
        response.getOutputStream().write(baos.toByteArray());
        */
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public Executor getExecutor() {
        return executor;
    }


    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getTimeout() {
        return timeout;
    }

    public ObjectMapper getJacksonObjectMapper() {
        return objectMapper;
    }
    
    public String getFemtorBackend() {
		return femtorBackend;
    }

	public void setFemtorBackend(String femtorBackend) {
		this.femtorBackend = femtorBackend;
    }

	public RapportrService getRapportr() {
    	return rapportr;
    }

	public void setRapportr(RapportrService rapportr) {
    	this.rapportr = rapportr;
    }

	public Backend[] getBackends() {
	    return backends;
    }

	public void setBackends(Backend[] backends) {
	    this.backends = backends;
    }
}
