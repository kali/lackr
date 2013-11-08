package com.fotonauts.lackr.components;

import java.util.ArrayList;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.HttpCookieStore;

import com.fotonauts.lackr.BaseProxy;
import com.fotonauts.lackr.ConstantHttpDirector;
import com.fotonauts.lackr.backend.client.ClientBackend;
import com.fotonauts.lackr.esi.FemtorJSESIRule;
import com.fotonauts.lackr.esi.HttpESIRule;
import com.fotonauts.lackr.esi.JSESIRule;
import com.fotonauts.lackr.esi.JSEscapedMLESIRule;
import com.fotonauts.lackr.esi.JSMLESIRule;
import com.fotonauts.lackr.esi.MLESIRule;
import com.fotonauts.lackr.interpolr.Interpolr;
import com.fotonauts.lackr.interpolr.Rule;

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

    public static Interpolr buildInterpolr(boolean esi) throws Exception {
        Interpolr interpolr = new Interpolr();
        ArrayList<Rule> rules = new ArrayList<>();
        
        if(esi)
            for(Rule rule: new Rule[] { new HttpESIRule(), new FemtorJSESIRule(), new JSESIRule(), new JSEscapedMLESIRule(),
                new JSMLESIRule(), new MLESIRule() })
                rules.add(rule);
        
        interpolr.setRules(rules.toArray(new Rule[rules.size()]));
        interpolr.start();
        return interpolr;
    }
}
