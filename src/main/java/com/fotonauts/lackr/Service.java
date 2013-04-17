package com.fotonauts.lackr;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

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
import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.reporting.GraphiteReporter;

public class Service extends AbstractHandler {

    private String LACKR_STATE_ATTRIBUTE = "lackr.state.attribute";
    static Logger log = LoggerFactory.getLogger(Service.class);

    private int timeout;

    protected RapportrService rapportr;

    protected Interpolr interpolr;

    protected Pattern regexpV4 = Pattern.compile("([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)");
    protected Pattern regexpV6 = Pattern.compile("([0-9a-f\\.:]+)");

    protected LookupService lookupV4;
    protected LookupService lookupV6;

    private String grid = "prod";
    
    public Map<String, Counter> countryTable = new HashMap<String, Counter>();
    public Map<String, Counter> statusTable = new HashMap<String, Counter>();
    public Map<String, Counter> endpointCounterTable = new HashMap<String, Counter>();
    public Map<String, Counter> endpointTimerTable = new HashMap<String, Counter>();
    public Map<String, Counter> bePerEPTable = new HashMap<String, Counter>();
    public Map<String, Counter> picorEpPerEPTable = new HashMap<String, Counter>();

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
        for (Backend b : backends) {
            for (Gateway us : b.getGateways()) {
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
        if (graphiteHost != null && graphitePort != 0) {
            String localhostname = InetAddress.getLocalHost().getCanonicalHostName().split("\\.")[0];
            GraphiteReporter.enable(10, TimeUnit.SECONDS, graphiteHost, graphitePort, "10sec.lackr." + localhostname);
        }

        if (new File("/usr/share/maxmind/GeoLiteCity.dat").exists())
            lookupV4 = new LookupService("/usr/share/maxmind/GeoLiteCity.dat", LookupService.GEOIP_MEMORY_CACHE);
        if (new File("/usr/share/maxmind/GeoLiteCityv6.dat").exists())
            lookupV6 = new LookupService("/usr/share/maxmind/GeoLiteCityv6.dat", LookupService.GEOIP_MEMORY_CACHE);

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
            String countryCode = getCountry(request.getHeader("x-forwarded-for"));
            countCountry(countryCode);
        } else {
            log.debug("resuming processing for: " + request.getRequestURL());
            state.writeResponse(response);
        }
    }

    public void countCountry(String countryCode) {
        counter(countryTable, "country", null, countryCode).inc();
    }

    public void countStatus(String statusCode) {
        counter(statusTable, "status", null, statusCode).inc();
    }

    public void countEndpointWithTimer(String endpoint, long d) {
        counter(endpointCounterTable, "EP", endpoint, "request-count").inc();
        counter(endpointTimerTable, "EP", endpoint, "elapsed-millis").inc(d);
    }

    public void countBePerEP(String endpoint, String be, int n) {
        counter(bePerEPTable, "EP", endpoint + ".BE." + be, "request-count").inc(n);
    }

    public void countPicorEpPerEP(String endpoint, String be, int n) {
        counter(picorEpPerEPTable, "EP", endpoint + ".picor." + be, "request-count").inc(n);
    }

    private static Counter counter(Map<String, Counter> table, String type, String scope, String key) {
        String fullKey = scope == null ? key : scope + key;
        synchronized (table) {
            Counter counter = table.get(fullKey);
            if (counter == null) {
                counter = Metrics.newCounter(new MetricName("lackr", type, key, scope));
                table.put(fullKey, counter);
            }
            return counter;
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
            for (Gateway us : backend.getGateways()) {
                ObjectName name = new ObjectName("com.fotonauts.lackr.gw:name=" + us.getMBeanName());
                mbs.unregisterMBean(name);
            }
            backend.stop();
        }
        Metrics.shutdown();
    }

    public Gateway getGateway() {
        return upstreamService;
    }

    public String getCountry(String ip) {
        if (ip == null)
            return "--";
        Location location = null;
        if (regexpV4.matcher(ip).matches())
            location = lookupV4.getLocation(ip);
        else if (regexpV6.matcher(ip).matches())
            location = lookupV6.getLocation(ip);
        if (location != null)
            return location.countryCode;
        return "--";
    }

    public void setGraphiteHostAndPort(String graphiteHostAndPort) {
        if (!StringUtils.hasText(graphiteHostAndPort))
            return;
        String[] tokens = graphiteHostAndPort.split(":");
        graphiteHost = tokens[0];
        graphitePort = Integer.parseInt(tokens[1]);
    }

    public String getGrid() {
        return grid;
    }

    public void setGrid(String grid) {
        this.grid = grid;
    }

}
