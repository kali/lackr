package com.fotonauts.lackr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.fotonauts.commons.RapportrService;
import com.fotonauts.lackr.interpolr.Interpolr;

@ManagedResource(objectName = "bean:name=LackrMainService", description = "Lackr frontend")
public class Service extends AbstractHandler {

    private String LACKR_STATE_ATTRIBUTE = "lackr.state.attribute";
    static Logger log = LoggerFactory.getLogger(Service.class);
    
    private AtomicLong requestCount = new AtomicLong();
    
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
    private AtomicLong runningFrontendRequest = new AtomicLong();
    private AtomicLong elapsedMillis = new AtomicLong();

    @Override
    protected void doStart() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer(); 
        for(Backend b: backends) {
            for(UpstreamService us: b.getUpstreamServices()) {
                ObjectName name = new ObjectName("com.fotonauts.lackr.upstream:name=" + us.getMBeanName());                 
                mbs.registerMBean(us, name); 
            }
        }
        setExecutor(Executors.newFixedThreadPool(16));
        super.doStart();
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        if (request.getRequestURI().equals("/_lackr_status")) {
            handleStatusQuery(target, baseRequest, request, response);
            return;
        }
        LackrFrontendRequest state = (LackrFrontendRequest) request.getAttribute(LACKR_STATE_ATTRIBUTE);
        if (state == null) {
            log.debug("starting processing for: " + request.getRequestURL());
            state = new LackrFrontendRequest(this, request);
            requestCount.incrementAndGet();
            request.setAttribute(LACKR_STATE_ATTRIBUTE, state);
            state.kick();
        } else {
            log.debug("resuming processing for: " + request.getRequestURL());
            state.writeResponse(response);
        }
    }

    protected void handleStatusQuery(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/plain");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        for (Backend b : backends) {
            b.dumpStatus(ps);
        }
        ps.println();
        ps.flush();

        response.setContentLength(baos.size());
        response.getOutputStream().write(baos.toByteArray());
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

    @Override
    public void doStop() throws Exception {
        for (Backend backend : backends) {
            backend.stop();
        }
    }

    @ManagedAttribute
    public long getRequestCount() {
        return requestCount.get();
    }

    @ManagedAttribute
    public long getRunningRequests() {
        return runningFrontendRequest.get();
    }

    @ManagedAttribute
    public long getElapsedMillis() {
        return elapsedMillis.get();
    }

    public AtomicLong getRunningFrontendRequestsHolder() {
        return runningFrontendRequest;
    }

    public AtomicLong getElapsedMillisHolder() {
        return elapsedMillis;
    }
}
