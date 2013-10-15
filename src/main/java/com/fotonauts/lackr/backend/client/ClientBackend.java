package com.fotonauts.lackr.backend.client;

import java.io.PrintStream;

import org.eclipse.jetty.client.HttpClient;

import com.fotonauts.lackr.HttpDirectorInterface;
import com.fotonauts.lackr.BaseGatewayMetrics;
import com.fotonauts.lackr.backend.Backend;
import com.fotonauts.lackr.backend.LackrBackendExchange;
import com.fotonauts.lackr.backend.LackrBackendRequest;
import com.fotonauts.lackr.backend.hashring.HashRing.NotAvailableException;

public class ClientBackend implements Backend {

    private HttpClient actualClient;

    private HttpDirectorInterface director;

    public void setActualClient(HttpClient actualClient) {
        this.actualClient = actualClient;
    }

    @Override
    public LackrBackendExchange createExchange(LackrBackendRequest request) throws NotAvailableException {
        return new ClientLackrBackendExchange(this, actualClient, director, request);
    }

    public void setDirector(HttpDirectorInterface director) {
        this.director = director;
    }

    @Override
    public void stop() throws Exception {
        director.stop();
    }

    @Override
    public void dumpStatus(PrintStream ps) {
        ps.format("Jetty HTTP Client\n");
        actualClient.dumpStdErr();
        director.dumpStatus(ps);
    }

    @Override
    public BaseGatewayMetrics[] getGateways() {
        return director.getGateways();
    }

    @Override
    public String getName() {
        return director.getName();
    }

    @Override
    public String toString() {
        return String.format("%s:%s", getClass().getSimpleName(), getName());
    }
}
