package com.fotonauts.lackr.testutils;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.HttpCookieStore;

import com.fotonauts.lackr.Backend;
import com.fotonauts.lackr.BaseProxy;
import com.fotonauts.lackr.MimeType;
import com.fotonauts.lackr.backend.client.ClientBackend;
import com.fotonauts.lackr.interpolr.Interpolr;
import com.fotonauts.lackr.interpolr.InterpolrScope;
import com.fotonauts.lackr.interpolr.Plugin;
import com.fotonauts.lackr.interpolr.esi.ESIPlugin;
import com.fotonauts.lackr.interpolr.handlebars.HandlebarsPlugin;
import com.fotonauts.lackr.interpolr.json.JsonPlugin;
import com.fotonauts.lackr.interpolr.proxy.InterpolrProxy;

public class Factory {

    public static Backend buildFullClientBackend(int port) throws Exception {
        return buildFullClientBackend(port, null);
    }

    public static Backend buildFullClientBackend(int port, String probe) throws Exception {
        return buildFullClientBackend("http://localhost:" + port, probe);
    }

    public static Backend buildFullClientBackend(String prefix, String probe) throws Exception {
        ClientBackend backend = new ClientBackend();
        backend.setPrefix(prefix);
        backend.setActualClient(buildFullClient());
        backend.setProbeUrl(probe);
        return backend;
    }

    public static HttpClient buildFullClient() {
        HttpClient client = new HttpClient();
        client.setRequestBufferSize(16000);
        client.setFollowRedirects(false);
        client.setConnectTimeout(1000);
        client.setCookieStore(new HttpCookieStore.Empty());
        return client;
    }

    public static Server buildSimpleProxyServer(int port) throws Exception {
        return buildSimpleProxyServer(buildFullClientBackend(port));
    }

    public static Interpolr buildInterpolr(String capabilities) throws Exception {
        Interpolr interpolr = new Interpolr();
        ArrayList<Plugin> plugins = new ArrayList<>();
        HandlebarsPlugin handlebarsPlugin = new HandlebarsPlugin();
        for(String cap: capabilities.split(" ")) {
            switch(cap) {
            case "esi":
                plugins.add(new ESIPlugin());
                break;
            case "handlebars":
                plugins.add(handlebarsPlugin);
                break;
            case "json":
                plugins.add(new JsonPlugin());
                break;
                /*
            case "$$inline_wrapper":
                handlebarsPlugin.registerPreprocessor(new WrapperFlattener());
                break;
                */
            }
        }
        interpolr.setPlugins(plugins.toArray(new Plugin[plugins.size()]));
        interpolr.start();
        return interpolr;
    }

    public static BaseProxy buildSimpleBaseProxy(Backend backend) {
        BaseProxy proxy = new BaseProxy();
        proxy.setManageIfNoneMatch(true);
        proxy.setBackend(backend);
        return proxy;
    }

    public static Server buildSimpleProxyServer(Backend backend) throws Exception {
        return buildProxyServer(buildSimpleBaseProxy(backend));
    }

    public static Server buildProxyServer(BaseProxy proxy) throws Exception {
        Server proxyServer = new Server();
        proxyServer.setHandler(proxy);
        proxyServer.addConnector(new ServerConnector(proxyServer));
        return proxyServer;
    }

    public static Server buildInterpolrProxyServer(Interpolr interpolr, Backend backend) throws Exception {
        return buildProxyServer(buildInterpolrProxy(interpolr, backend));
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

    public static BaseProxy buildInterpolrProxy(Interpolr interpolr, Backend backend) {
        InterpolrProxy proxy = new InterpolrProxy();
        proxy.setManageIfNoneMatch(true);
        proxy.setInterpolr(interpolr);
        proxy.setBackend(backend);
        return proxy;
    }

}
