package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.servlet.NoJspServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.HttpCookieStore;
import org.junit.After;
import org.junit.Ignore;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.Log4jConfigurer;

import com.fotonauts.lackr.client.JettyBackend;
import com.ibm.icu.util.TimeZone;

@Ignore
public class BaseTestLackrFullStack {

    protected Server backendStub;
    protected int backendStubPort = 38000;
    protected ServerConnector backendStubConnector;
    protected Server femtorStub;
    protected int femtorStubPort = 38001;
    protected Server lackrServer;
    protected int lackrPort = 38002;
    protected Service lackrService;
    protected HttpClient client;
    protected ClassPathXmlApplicationContext ctx;

    protected AtomicReference<Handler> currentHandler;
    private JettyBackend picorBackend;
    private ServerConnector lackrStubConnector;
    private ServerConnector femtorStubConnector;

    public BaseTestLackrFullStack() throws Exception {
        this(true);
    }

    public BaseTestLackrFullStack(boolean femtorInProcess) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        Log4jConfigurer.initLogging("classpath:log4j.properties");
        currentHandler = new AtomicReference<Handler>();

        backendStub = new Server();
        backendStub.setHandler(new AbstractHandler() {

            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                    throws IOException, ServletException {
                currentHandler.get().handle(target, baseRequest, request, response);
            }
        });
        backendStub.start();

        File propFile = File.createTempFile("lackr.test.", ".props");
        propFile.deleteOnExit();

        Properties props = PropertiesLoaderUtils.loadProperties(new ClassPathResource("lackr.test.properties"));
        if (!femtorInProcess) {
            femtorStub = new Server();
            femtorStubConnector = new ServerConnector(femtorStub);
            femtorStub.addConnector(femtorStubConnector);
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            femtorStub.setHandler(context);

            context.addFilter("com.fotonauts.lackr.DummyFemtor", "/*", EnumSet.of(DispatcherType.REQUEST));
            context.addServlet(new ServletHolder(new NoJspServlet()), "/*");

            femtorStub.start();
            femtorStubPort = femtorStubConnector.getLocalPort();
            props.setProperty("lackr.femtorImpl", "Http");
            props.setProperty("lackr.femtorBackend", "http://localhost:" + femtorStubPort);
        }

        props.store(new FileOutputStream(propFile), "properties for lackr test run");

        System.setProperty("lackr.properties", "file:" + propFile.getCanonicalPath());

        ctx = new ClassPathXmlApplicationContext("lackr.xml");
        System.err.println("GET BEAN(picorBackend)");
        picorBackend = (JettyBackend) ctx.getBean("picorBackend");
        System.err.println("GET BEAN(picorBackend) DONE");

        backendStubConnector = new ServerConnector(backendStub);
        backendStub.addConnector(backendStubConnector);
        backendStubPort = backendStubConnector.getLocalPort();
        picorBackend.setDirector(new ConstantHttpDirector("http://localhost:" + backendStubPort));

        lackrServer = new Server();
        lackrServer.setHandler((Handler) ctx.getBean("proxyService"));
        lackrStubConnector = new ServerConnector(lackrServer);
        lackrServer.addConnector(lackrStubConnector);
        lackrServer.start();

        lackrPort = lackrStubConnector.getLocalPort();

        lackrService = (Service) ctx.getBean("proxyService");

        client = new HttpClient();
        client.setFollowRedirects(false);
        client.setConnectTimeout(5);
        client.setCookieStore(new HttpCookieStore.Empty());
        client.start();
    }

    public static void writeResponse(HttpServletResponse response, byte[] data, String type) throws IOException {
        response.setContentType(type);
        response.setCharacterEncoding("UTF-8");
        response.getOutputStream().write(data);
        response.flushBuffer();
    }

    protected org.eclipse.jetty.client.api.Request createExchange(String url) {
        return client.newRequest(url);
    }

    protected ContentResponse runRequest(org.eclipse.jetty.client.api.Request e, String expect) {
        ContentResponse response = null;
        try {
            response = e.timeout(15, TimeUnit.SECONDS).send();
            assertEquals(200, response.getStatus());
            assertEquals(expect, response.getContentAsString());
        } catch (InterruptedException | TimeoutException | ExecutionException e2) {
            e2.printStackTrace();
        }

        return response;
    }

    @After
    public void tearDown() throws Exception {
        try {
            int slept = 0;
            while (slept < 10000
                    && (lackrService.getGateway().getRunningRequests() != 0 || picorBackend.getGateways()[0].getRunningRequests() != 0)) {
                slept += 5;
                Thread.sleep(5);
            }
            assertEquals("all incoming requests done and closed", 0, lackrService.getGateway().getRunningRequests());
            assertEquals("all backend requests done and closed", 0, picorBackend.getGateways()[0].getRunningRequests());
        } finally {
            Object collectables[] = new Object[] { lackrService, lackrServer, lackrStubConnector, picorBackend, backendStub,
                    backendStubConnector, femtorStub, femtorStubConnector, client, ctx };
            String methods[] = new String[] { "stop", "close", "destroy" };
            for (Object collectable : collectables) {
                if (collectable != null)
                    for (String methodName : methods)
                        try {
                            collectable.getClass().getMethod(methodName).invoke(collectable);
                        } catch (Throwable t) {

                        }
            }
            System.err.println("remaining thread after collection: " + Thread.getAllStackTraces().size());
        }
    }
}
