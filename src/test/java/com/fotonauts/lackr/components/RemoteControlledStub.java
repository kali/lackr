package com.fotonauts.lackr.components;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.component.AbstractLifeCycle;

public class RemoteControlledStub extends AbstractLifeCycle {

    private AtomicReference<Handler> currentHandler = new AtomicReference<>();
    protected Server backendStub;

    public RemoteControlledStub() {
        backendStub = new Server();
        backendStub.setHandler(new AbstractHandler() {

            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                    throws IOException, ServletException {
                currentHandler.get().handle(target, baseRequest, request, response);
            }
        });
        ServerConnector backendStubConnector = new ServerConnector(backendStub);
        backendStub.addConnector(backendStubConnector);
    }

    public AtomicReference<Handler> getCurrentHandler() {
        return currentHandler;
    }

    public void setCurrentHandler(AtomicReference<Handler> currentHandler) {
        this.currentHandler = currentHandler;
    }

    public static void writeResponse(HttpServletResponse response, byte[] data, String type) throws IOException {
        response.setContentType(type);
        response.setCharacterEncoding("UTF-8");
        response.getOutputStream().write(data);
        response.flushBuffer();
    }

    @Override
    public void doStart() throws Exception {
        backendStub.start();
    }

    public int getPort() {
        return ((ServerConnector) backendStub.getConnectors()[0]).getLocalPort();
    }

    @Override
    public void doStop() throws Exception {
        backendStub.stop();
    }

}