package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.EnumSet;
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
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import com.fotonauts.commons.RapportrService;
import com.fotonauts.lackr.backend.Backend;
import com.fotonauts.lackr.backend.client.ClientBackend;
import com.fotonauts.lackr.backend.inprocess.InProcessFemtor;
import com.ibm.icu.util.TimeZone;

@Ignore
public class BaseTestLackrFullStack {

    // From:
    // http://blog.efftinge.de/2008/10/multi-line-string-literals-in-java.html
    // Takes a comment (/**/) and turns everything inside the comment to a
    // string that is returned from S()
    public static String S() throws FileNotFoundException {
        StackTraceElement element = new RuntimeException().getStackTrace()[1];
        String name = "src/test/java/" + element.getClassName().replace('.', '/') + ".java";
        InputStream in = new FileInputStream(name);
        String s = convertStreamToString(in, element.getLineNumber());
        return s.substring(s.indexOf("/*") + 2, s.indexOf("*/"));
    }

    // From http://www.kodejava.org/examples/266.html
    private static String convertStreamToString(InputStream is, int lineNum) {
        /*
         * To convert the InputStream to String we use the
         * BufferedReader.readLine() method. We iterate until the BufferedReader
         * return null which means there's no more data to read. Each line will
         * appended to a StringBuilder and returned as String.
         */
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        int i = 1;
        try {
            while ((line = reader.readLine()) != null) {
                if (i++ >= lineNum) {
                    sb.append(line + "\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return sb.toString();
    }

    protected void assertContains(String haystack, String needle) {
        assertTrue(haystack + "\n\nexpected to contain\n\n" + needle, haystack.contains(needle));
    }

    @Rule
    public TemporaryFolder assetRoot = new TemporaryFolder();

    protected Server backendStub;
    protected int backendStubPort = 38000;
    protected ServerConnector backendStubConnector;
    protected Server femtorStub;
    protected int femtorStubPort = 38001;
    protected Server lackrServer;
    protected int lackrPort = 38002;
    protected Service lackrService;
    protected HttpClient client;

    protected AtomicReference<Handler> currentHandler;
    private ClientBackend picorBackend;
    private ServerConnector lackrStubConnector;
    private ServerConnector femtorStubConnector;

    private LackrConfiguration configuration;

    public BaseTestLackrFullStack() throws Exception {
        this(true);
    }

    public BaseTestLackrFullStack(final boolean femtorInProcess) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        currentHandler = new AtomicReference<Handler>();

        backendStub = new Server();
        backendStub.setHandler(new AbstractHandler() {

            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                    throws IOException, ServletException {
                currentHandler.get().handle(target, baseRequest, request, response);
            }
        });
        backendStubConnector = new ServerConnector(backendStub);
        backendStub.addConnector(backendStubConnector);
        backendStub.start();
        backendStubPort = backendStubConnector.getLocalPort();

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
                picorBackend.setDirector(new ConstantHttpDirector("http://localhost:" + backendStubPort));
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
        lackrServer.setHandler(configuration.getLackrService());
        lackrStubConnector = new ServerConnector(lackrServer);
        lackrServer.addConnector(lackrStubConnector);
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

    @Before
    public void setup() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
        try {
            int slept = 0;
            while (slept < 10000
                    && (lackrService.getGateway().getRunningRequests() != 0 || picorBackend.getGateways()[0].getRunningRequests() != 0)) {
                slept += 5;
                System.err.println("waiting !");
                Thread.sleep(5);
            }
            assertEquals("all incoming requests done and closed", 0, lackrService.getGateway().getRunningRequests());
            assertEquals("all backend requests done and closed", 0, picorBackend.getGateways()[0].getRunningRequests());
        } finally {
            Object collectables[] = new Object[] { lackrService, lackrServer, lackrStubConnector, picorBackend, backendStub,
                    backendStubConnector, femtorStub, femtorStubConnector, client, configuration };
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
