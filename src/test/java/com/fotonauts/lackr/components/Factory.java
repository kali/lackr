package com.fotonauts.lackr.components;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.HttpCookieStore;

import com.fotonauts.lackr.BaseProxy;
import com.fotonauts.lackr.ConstantHttpDirector;
import com.fotonauts.lackr.backend.client.ClientBackend;

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

    public static Server buildSimpleProxyServer(int port) {
        Server proxyServer = new Server();
        BaseProxy proxy = new BaseProxy();
        proxy.setBackend(buildFullClientBackend(port));
        proxyServer.setHandler(proxy);
        proxyServer.addConnector(new ServerConnector(proxyServer));
        return proxyServer;
    }

    
}
