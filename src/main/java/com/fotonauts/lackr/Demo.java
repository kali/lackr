package com.fotonauts.lackr;

import java.io.IOException;
import java.net.ServerSocket;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import com.fotonauts.lackr.interpolr.Interpolr;
import com.fotonauts.lackr.testutils.Factory;

public class Demo {

    public static int findFreeTCPPort() {
        for (int port = 8000; port < 8100; port++) {
            ServerSocket ss = null;
            try {
                ss = new ServerSocket(port);
                ss.setReuseAddress(true);
                return port;
            } catch (IOException e) {
            } finally {
                if (ss != null) {
                    try {
                        ss.close();
                    } catch (IOException e) {
                        /* should not be thrown */
                    }
                }
            }
        }
        throw new RuntimeException("No available port found");

    }

    public static void main(String[] args) throws Exception {
        String backendUrl = "http://localhost:8888/lackr-examples/";
        if (args.length > 0 && args[0] != null && !"".equals(args[0]))
            backendUrl = args[0];

        Backend backend = Factory.buildFullClientBackend(backendUrl, null);
        Interpolr interpolr = Factory.buildInterpolr("handlebars esi");

        int port = findFreeTCPPort();
        Server server = Factory.buildInterpolrProxyServer(interpolr, backend, port);

        server.start();
        System.out.println("");
        System.out.println("export BACKEND=" + backendUrl);
        System.out.println("export PROXY=http://localhost:" + ((ServerConnector) (server.getConnectors()[0])).getLocalPort() + "/");
        server.join();
    }

}
