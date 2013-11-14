package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.component.LifeCycle;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fotonauts.lackr.components.AppStubForESI;
import com.fotonauts.lackr.components.Factory;
import com.fotonauts.lackr.components.RemoteControlledStub;
import com.fotonauts.lackr.components.TestClient;

public class Test304 {

    AppStubForESI remoteApp;
    RemoteControlledStub remoteControlledStub;
    Server proxyServer;
    TestClient client;

    @Before
    public void setup() throws Exception {
        remoteApp = new AppStubForESI();
        remoteApp.pageContent.set("whatever");

        remoteControlledStub = Factory.buildServerForESI(remoteApp);
        remoteControlledStub.start();

        proxyServer = Factory.buildSimpleProxyServer(remoteControlledStub.getPort());
        proxyServer.start();

        client = new TestClient(((ServerConnector) proxyServer.getConnectors()[0]).getLocalPort());
        client.start();
    }

    @After
    public void tearDown() throws Exception {
        LifeCycle[] zombies = new LifeCycle[] { client, proxyServer, remoteControlledStub };
        for (LifeCycle z : zombies)
            z.stop();
        assertTrue(Thread.getAllStackTraces().size() < 10);
    }

    /*
    protected ContentResponse run(String testPage) throws IOException, InterruptedException, TimeoutException, ExecutionException {
    	return run(testPage, null);
    }

    protected ContentResponse run(final String testPage, String etag) throws IOException, InterruptedException, TimeoutException, ExecutionException {
    	remoteControlledStub1.getCurrentHandler().set(new AbstractHandler() {
    		
    		@Override
    		public void handle(String target, org.eclipse.jetty.server.Request request, HttpServletRequest baseRequest, HttpServletResponse response)
    		        throws IOException, ServletException {
    			writeResponse(response, testPage.getBytes(), MimeType.TEXT_HTML);
    		}
    	});
    	Request e = client.newRequest("http://localhost:" + lackrPort + "/page.html");
    	if(etag != null)
    		e.header(HttpHeader.IF_NONE_MATCH.asString(), etag);
    	return e.send();
    }
    */

    @Test
    @Ignore //FIXME
    public void testEtagGeneration() throws Exception {
        remoteApp.pageContent.set("blah");
        ContentResponse e1 = client.runRequest(client.createExchange("/page.html"), "blah");
        String etag1 = e1.getHeaders().getStringField("etag");
        assertNotNull("e1 has etag", etag1);

        remoteApp.pageContent.set("blih");
        ContentResponse e2 = client.runRequest(client.createExchange("/page.html"), "blih");
        String etag2 = e2.getHeaders().getStringField("etag");
        assertNotNull("e2 has etag", etag2);

        assertTrue("etags are different", !etag1.equals(etag2));
    }

    @Test
    @Ignore //FIXME
    public void testEtagAndIfNoneMatch() throws Exception {
        remoteApp.pageContent.set("blah");
        ContentResponse e1 = client.runRequest(client.createExchange("/page.html"), "blah");
        String etag1 = e1.getHeaders().getStringField("etag");
        assertNotNull("e1 has etag", etag1);

        ContentResponse e2 = client.runRequest(client.createExchange("/page.html"), "blah");
        assertEquals(e2.getStatus(), HttpStatus.OK_200);

        Request req3 = client.createExchange("/page.html");
        req3.header(HttpHeader.IF_NONE_MATCH, etag1);
        ContentResponse e3 = client.runRequest(req3, "");
        assertEquals(e3.getStatus(), HttpStatus.NOT_MODIFIED_304);
        assertFalse(e2.getHeaders().getFieldNamesCollection().contains(HttpHeader.CONTENT_LENGTH));
        assertFalse(e2.getHeaders().getFieldNamesCollection().contains(HttpHeader.CONTENT_TYPE));
        assertEquals(e3.getContent(), null);

        remoteApp.pageContent.set("blih");
        Request req4 = client.createExchange("/page.html");
        req4.header(HttpHeader.IF_NONE_MATCH, etag1);
        ContentResponse e4 = client.runRequest(req3, "blih");
        assertEquals(e4.getStatus(), HttpStatus.OK_200);
    }
}
