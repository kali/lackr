package com.fotonauts.lackr.backend.inprocess;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.Filter;

import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.Backend;
import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.LackrBackendRequest;

public class InProcessBackend extends AbstractLifeCycle implements Backend {

    static Logger log = LoggerFactory.getLogger(InProcessBackend.class);

    private Filter filter;

    protected Thread watchDog;
    final protected Map<InProcessExchange, Long> registeredTimeouts = Collections
            .synchronizedMap(new HashMap<InProcessExchange, Long>());
    protected long timeout = 45 * 1000; // 45s

    public InProcessBackend(Filter filter) {
        this.filter = filter;
    }

    // package visibility on purpose
    void registerTimeout(InProcessExchange t, long when) {
        registeredTimeouts.put(t, when);
    }

    void registerTimeout(InProcessExchange t) {
        t.thread.set(Thread.currentThread());
        registeredTimeouts.put(t, System.currentTimeMillis() + timeout);
    }

    // package visibility on purpose
    void unregisterTimeout(InProcessExchange t) {
        registeredTimeouts.remove(t);
        t.thread.set(null);
    }

    @Override
    protected void doStart() throws Exception {
        registeredTimeouts.clear();
        watchDog = new Thread() {
            @Override
            public void run() {
                while (isStarting() || isRunning()) {
                    try {
                        for (Entry<InProcessExchange, Long> timeout : registeredTimeouts.entrySet()) {
                            if (timeout.getValue() < System.currentTimeMillis()) {
                                log.warn("Interrupting on timeout: {}", timeout.getKey().getBackendRequest());
                                Thread t = timeout.getKey().thread.get();
                                if (t != null)
                                    t.interrupt();
                            }
                        }
                    } finally {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        };
        watchDog.start();
    }

    @Override
    protected void doStop() throws Exception {
        watchDog.join();
    }

    @Override
    public LackrBackendExchange createExchange(LackrBackendRequest request) {
        return new InProcessExchange(this, request);
    }

    @Override
    public String toString() {
        return String.format("%s:%s", getClass().getSimpleName(), getName());
    }

    protected void setFilter(Filter filter) {
        this.filter = filter;
    }

    public Filter getFilter() {
        return filter;
    }

    @Override
    public String getName() {
        return filter.getClass().getName();
    }

    @Override
    public boolean probe() {
        return filter != null;
    }

    public void setTimeout(long i) {
        this.timeout = i;
    }

}
