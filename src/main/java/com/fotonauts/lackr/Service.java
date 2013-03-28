package com.fotonauts.lackr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
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
import org.springframework.util.StringUtils;

import com.fotonauts.commons.RapportrService;
import com.fotonauts.lackr.interpolr.Interpolr;
import com.yammer.metrics.reporting.GraphiteReporter;

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
    
    private String graphiteHost = null;
    private int graphitePort = 0;

    private Gateway upstreamService = new Gateway() {
        @Override
        public String getMBeanName() {
            return "front";
        }
    };

    @Override
    protected void doStart() throws Exception {
        upstreamService.start();
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        mbs.registerMBean(upstreamService, new ObjectName("com.fotonauts.lackr.gw:name=front"));
        for(Backend b: backends) {
            for(Gateway us: b.getGateways()) {
//                String beanName = us.getMBeanName();
                try {
                    ObjectName name = new ObjectName("com.fotonauts.lackr.gw:name=" + us.getMBeanName());
                    mbs.registerMBean(us, name);
                } catch (MalformedObjectNameException e) {
                    log.error("Bean name exception: " + us.getMBeanName());
                    throw e;
                }
            }
        }
        setExecutor(Executors.newFixedThreadPool(64));
        if(graphiteHost != null && graphitePort != 0) {
            String localhostname = InetAddress.getLocalHost().getCanonicalHostName().split("\\.")[0];
            GraphiteReporter.enable(10, TimeUnit.SECONDS, graphiteHost, graphitePort, "10sec.lackr." + localhostname + ".");
        }
        super.doStart();
    }

    private void dumpSelector() {
        System.err.println("gna gna gna");
        backends[0].dumpStatus(System.err);
    }
    
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        
        if (request.getRequestURI().equals("/_lackr_status")) {
            handleStatusQuery(target, baseRequest, request, response);
            return;
        }
        if (request.getRequestURI().equals("/_dump_selector")) {
            dumpSelector();
            response.setStatus(200);
            response.flushBuffer();
            return;
        }
        LackrFrontendRequest state = (LackrFrontendRequest) request.getAttribute(LACKR_STATE_ATTRIBUTE);
        if (state == null) {
            log.debug("starting processing for: " + request.getRequestURL());
            state = new LackrFrontendRequest(this, request);
            upstreamService.getRequestCountHolder().inc();
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
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        mbs.unregisterMBean(new ObjectName("com.fotonauts.lackr.gw:name=front"));
        for (Backend backend : backends) {
            for(Gateway us: backend.getGateways()) {
                ObjectName name = new ObjectName("com.fotonauts.lackr.gw:name=" + us.getMBeanName());
                mbs.unregisterMBean(name);
            }
            backend.stop();
        }
    }

    public Gateway getGateway() {
        return upstreamService;
    }

    public void setGraphiteHostAndPort(String graphiteHostAndPort) {
        if(!StringUtils.hasText(graphiteHostAndPort))
            return;
        String[] tokens = graphiteHostAndPort.split(":");
        graphiteHost = tokens[0];
        graphitePort = Integer.parseInt(tokens[1]);
    }
    
}
