package com.fotonauts.lackr.components;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.HttpCookieStore;

import com.fotonauts.lackr.BaseProxy;
import com.fotonauts.lackr.ConstantHttpDirector;
import com.fotonauts.lackr.InterpolrProxy;
import com.fotonauts.lackr.MimeType;
import com.fotonauts.lackr.backend.Backend;
import com.fotonauts.lackr.backend.client.ClientBackend;
import com.fotonauts.lackr.esi.HttpESIRule;
import com.fotonauts.lackr.esi.JSESIRule;
import com.fotonauts.lackr.esi.JSEscapedMLESIRule;
import com.fotonauts.lackr.esi.JSMLESIRule;
import com.fotonauts.lackr.esi.MLESIRule;
import com.fotonauts.lackr.interpolr.Interpolr;
import com.fotonauts.lackr.interpolr.InterpolrScope;
import com.fotonauts.lackr.interpolr.Rule;
import com.fotonauts.lackr.mustache.ArchiveRule;
import com.fotonauts.lackr.mustache.DumpArchiveRule;
import com.fotonauts.lackr.mustache.EvalRule;
import com.fotonauts.lackr.mustache.TemplateRule;

public class Factory {

    public static ClientBackend buildFullClientBackend(int port) {
        ClientBackend backend = new ClientBackend();
        backend.setDirector(new ConstantHttpDirector("http://localhost:" + port));
        backend.setActualClient(buildFullClient());
        return backend;
    }

    public static HttpClient buildFullClient() {
        HttpClient client = new HttpClient();
        client.setRequestBufferSize(16000);
        client.setFollowRedirects(false);
        client.setConnectTimeout(15);
        client.setCookieStore(new HttpCookieStore.Empty());
        return client;
    }

    public static Server buildSimpleProxyServer(int port) throws Exception {
        return buildSimpleProxyServer(buildFullClientBackend(port));
    }

    public static Interpolr buildInterpolr(String capabilities) throws Exception {
        Interpolr interpolr = new Interpolr();
        List<String> caps = Arrays.asList(capabilities.split(" "));

        ArrayList<Rule> rules = new ArrayList<>();

        if (caps.indexOf("archive") >= 0)
            rules.addAll(Arrays.asList(new Rule[] { new DumpArchiveRule(), new ArchiveRule() }));
        if (caps.indexOf("mustache") >= 0)
            rules.addAll(Arrays.asList(new Rule[] { new TemplateRule(), new EvalRule() }));
        if (caps.indexOf("esi") >= 0)
            rules.addAll(Arrays.asList(new Rule[] { new HttpESIRule(), new JSESIRule(), new JSEscapedMLESIRule(),
                    new JSMLESIRule(), new MLESIRule() }));

        interpolr.setRules(rules.toArray(new Rule[rules.size()]));
        interpolr.start();
        return interpolr;
    }

    public static Server buildSimpleProxyServer(Backend backend) throws Exception {
        Server proxyServer = new Server();
        BaseProxy proxy = new BaseProxy();
        proxy.setBackend(backend);
        proxyServer.setHandler(proxy);
        proxyServer.addConnector(new ServerConnector(proxyServer));
        return proxyServer;
    }

    public static Server buildInterpolrProxyServer(Interpolr interpolr, Backend backend) throws Exception {
        Server proxyServer = new Server();
        InterpolrProxy proxy = new InterpolrProxy();
        proxy.setInterpolr(interpolr);
        proxy.setBackend(backend);
        proxyServer.setHandler(proxy);
        proxyServer.addConnector(new ServerConnector(proxyServer));
        return proxyServer;
    }

    public static RemoteControlledStub buildServerForESI(final AppStubForESI app) {
        RemoteControlledStub stub = new RemoteControlledStub();
        stub.getCurrentHandler().set(new AbstractHandler() {

            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                    throws IOException, ServletException {
                if (request.getPathInfo().equals("/page.html")) {
                    response.setContentType(MimeType.TEXT_HTML);
                    response.setCharacterEncoding("UTF-8");
                    response.getOutputStream().write(app.pageContent.get().getBytes("UTF-8"));
                    response.flushBuffer();
                } else {
                    InterpolrScope scope = app.getInterpolrScope(null, null, target);
                    response.setContentType(scope.getResultMimeType());
                    response.getOutputStream().write(scope.getBodyBytes());
                    response.flushBuffer();
                }
            };
        });
        return stub;
    }
}
