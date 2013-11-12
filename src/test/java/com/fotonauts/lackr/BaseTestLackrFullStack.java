package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.NoJspServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.HttpCookieStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import com.fotonauts.commons.RapportrService;
import com.fotonauts.lackr.backend.Backend;
import com.fotonauts.lackr.backend.client.ClientBackend;
import com.fotonauts.lackr.backend.inprocess.InProcessFemtor;
import com.fotonauts.lackr.components.RemoteControlledStub;
import com.ibm.icu.util.TimeZone;

@Ignore
public class BaseTestLackrFullStack {

    protected void assertContains(String haystack, String needle) {
        assertTrue(haystack + "\n\nexpected to contain\n\n" + needle, haystack.contains(needle));
    }

    @Rule
    public TemporaryFolder assetRoot = new TemporaryFolder();

    protected Server femtorStub;
    protected int femtorStubPort = 38001;
    protected Server lackrServer;
    protected int lackrPort = 38002;
    protected InterpolrProxy lackrService;
    protected HttpClient client;

    protected RemoteControlledStub remoteControlledStub;
    private ClientBackend picorBackend;
    private ServerConnector lackrStubConnector;
    private ServerConnector femtorStubConnector;

    private LackrConfiguration configuration;

    public BaseTestLackrFullStack() throws Exception {
        this(true);
    }

    public BaseTestLackrFullStack(final boolean femtorInProcess) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        remoteControlledStub = new RemoteControlledStub();
        remoteControlledStub.start();
        
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
        }

        configuration = new LackrConfiguration() {
            @Override
            protected ClientBackend buildVarnishAndPicorBackend() throws Exception {
                picorBackend = new ClientBackend();
                picorBackend.setDirector(new ConstantHttpDirector("http://localhost:" + remoteControlledStub.getPort()));
                picorBackend.setActualClient(getJettyClient());
                return picorBackend;
            }

            @Override
            protected Backend buildFemtorBackend() throws Exception {
                if (femtorInProcess)
                    return buildFemtorBackendInprocess();
                else
                    return buildFemtorBackendHttp();
            }

            @Override
            protected ClientBackend buildFemtorBackendHttp() throws Exception {
                ClientBackend femtor = new ClientBackend();
                femtor.setDirector(new ConstantHttpDirector("http://localhost:" + femtorStubPort));
                femtor.setActualClient(getJettyClient());
                return femtor;
            }

            @Override
            protected InProcessFemtor buildFemtorBackendInprocess() throws Exception {
                InProcessFemtor femtor = new InProcessFemtor();
                femtor.setFemtorHandlerClass("com.fotonauts.lackr.DummyFemtor");
                femtor.init();
                return femtor;
            }

            protected RapportrService buildRapportrService() throws Exception {
                return new RapportrService();
            }

        };

        lackrServer = new Server();
        InterpolrProxy proxy = configuration.getLackrService();
        lackrServer.setHandler(proxy);
        lackrStubConnector = new ServerConnector(lackrServer);
        lackrServer.addConnector(lackrStubConnector);
        lackrServer.addBean(configuration.getLackrService());
        lackrServer.start();

        lackrPort = lackrStubConnector.getLocalPort();

        lackrService = configuration.getLackrService();

        client = new HttpClient();
        client.setRequestBufferSize(16000);
        client.setFollowRedirects(false);
        client.setConnectTimeout(15);
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

    protected ContentResponse runRequest(org.eclipse.jetty.client.api.Request e, String expect) throws Exception {
        ContentResponse response = null;
        response = e.timeout(600, TimeUnit.SECONDS).send();
        System.err.println(response.getContentAsString());
        assertEquals(200, response.getStatus());
        assertEquals(expect, response.getContentAsString());

        return response;
    }

    @Before
    public void setup() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
        try {
            /*
            int slept = 0;
            while (slept < 10000
                    && (lackrService.getGateway().getRunningRequests() != 0 || picorBackend.getGateways()[0].getRunningRequests() != 0)) {
                slept += 5;
                Thread.sleep(5);
            }
            assertEquals("all incoming requests done and closed", 0, lackrService.getGateway().getRunningRequests());
            assertEquals("all backend requests done and closed", 0, picorBackend.getGateways()[0].getRunningRequests());
            */
        } finally {
            Object collectables[] = new Object[] { lackrService, lackrServer, lackrStubConnector, picorBackend,
                    remoteControlledStub, femtorStub, femtorStubConnector, client, configuration };
            String methods[] = new String[] { "stop", "close", "destroy" };
            for (Object collectable : collectables) {
                //System.err.println("collectable: " + collectable);
                if (collectable != null)
                    for (String methodName : methods)
                        try {
                            collectable.getClass().getMethod(methodName).invoke(collectable);
                        } catch (Throwable t) {

                        }
            }
            /*
            int slept = 0;
            int targetThreadCount = 5;
            while(slept < 10000 && Thread.getAllStackTraces().size() > targetThreadCount) {
                System.err.println("remaineing thread" + Thread.getAllStackTraces().size());
                System.gc();
                Thread.sleep(5);
                slept += 5;
            }
            if (Thread.getAllStackTraces().size() > targetThreadCount) {
                throw new RuntimeException("thread leak detected !");
            }
            */
        }
    }
}
