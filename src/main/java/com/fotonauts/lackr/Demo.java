package com.fotonauts.lackr;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import com.fotonauts.lackr.interpolr.Interpolr;
import com.fotonauts.lackr.testutils.Factory;

public class Demo {

    public static void main(String[] args) throws Exception {
        String backendUrl = "http://localhost/~" + System.getProperty("user.name") + "/";
        if (args.length > 1 && args[1] != null && !"".equals(args[1]))
            backendUrl = args[1];
        
        Backend backend = Factory.buildFullClientBackend(backendUrl, null);
        Interpolr interpolr = Factory.buildInterpolr("handlebars esi");
        
        Server server = Factory.buildInterpolrProxyServer(interpolr, backend);

        server.start();
        System.out.println("Backend is " + backendUrl);
        System.out.println("Proxy Server listenning on http://localhost:" + ((ServerConnector) (server.getConnectors()[0])).getLocalPort() + "/");
        server.join();
    }

}
