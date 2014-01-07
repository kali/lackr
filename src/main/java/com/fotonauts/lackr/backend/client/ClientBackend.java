package com.fotonauts.lackr.backend.client;

import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.Backend;
import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.LackrBackendRequest;
import com.fotonauts.lackr.backend.hashring.HashRingBackend.NotAvailableException;

public class ClientBackend extends AbstractLifeCycle implements Backend {

    static Logger log = LoggerFactory.getLogger(ClientBackend.class);

    private HttpClient actualClient;

    private String prefix;

    private String probeUrl;

    public void setActualClient(HttpClient actualClient) {
        this.actualClient = actualClient;
    }

    @Override
    public LackrBackendExchange createExchange(LackrBackendRequest request) throws NotAvailableException {
        return new ClientLackrBackendExchange(this, actualClient, prefix, request);
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
    
    public void setProbeUrl(String probeUrl) {
        this.probeUrl = probeUrl;
    }

    @Override
    public String getName() {
        return prefix;
    }

    @Override
    public String toString() {
        return String.format("%s:%s", getClass().getSimpleName(), getName());
    }

    @Override
    public void doStart() throws Exception {
        actualClient.start();
    }

    @Override
    public void doStop() throws Exception {
        actualClient.stop();
    }

    @Override
    public boolean probe() {
        if(probeUrl != null) {
            try {
                ContentResponse resp = actualClient.newRequest(prefix + probeUrl).timeout(2000, TimeUnit.MILLISECONDS).send();
                return resp.getStatus() == HttpStatus.OK_200;
            } catch (Throwable e) {
                log.info("Error while probing: ", e);
                return false;
            }
            
        }
        return true;
    }
}
