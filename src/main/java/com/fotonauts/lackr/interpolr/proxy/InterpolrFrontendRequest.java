package com.fotonauts.lackr.interpolr.proxy;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.BaseFrontendRequest;
import com.fotonauts.lackr.CompletionListener;
import com.fotonauts.lackr.LackrBackendRequest;
import com.fotonauts.lackr.interpolr.Interpolr;
import com.fotonauts.lackr.interpolr.InterpolrContext;
import com.fotonauts.lackr.interpolr.InterpolrScope;
import com.fotonauts.lackr.interpolr.Plugin;

public class InterpolrFrontendRequest extends BaseFrontendRequest implements InterpolrContext {

    static Logger log = LoggerFactory.getLogger(InterpolrFrontendRequest.class);

    private AtomicInteger pendingCount;

    protected InterpolrProxy service;

    private ConcurrentHashMap<String, InterpolrScope> backendRequestCache = new ConcurrentHashMap<String, InterpolrScope>();

    private ProxyInterpolrScope rootScope;

    protected HashMap<Plugin, Object> pluginData = new HashMap<>();

    protected InterpolrFrontendRequest(final InterpolrProxy baseProxy, HttpServletRequest request) {
        super(baseProxy, request);
        this.service = baseProxy;
        this.pendingCount = new AtomicInteger(0);
        for (Plugin p : baseProxy.getInterpolr().getPlugins()) {
            pluginData.put(p, p.createContext(this));
        }
    }

    public InterpolrScope getSubBackendExchange(String url, String format, InterpolrScope dad) {
        log.debug("{} requires {} (as {})", dad, url, format);
        String key = format + "::" + url;
        InterpolrScope ex = backendRequestCache.get(key);
        if (ex != null)
            return ex;

        final ProxyInterpolrScope newBorn = new ProxyInterpolrScope(this);
        backendRequestCache.put(key, newBorn);
        LackrBackendRequest dadRequest = ((ProxyInterpolrScope) dad).getRequest();
        LackrBackendRequest req = service.createSubRequest(this, dadRequest, url, format, new CompletionListener() {

            @Override
            public void fail(Throwable t) {
                addBackendExceptions(t);
                log.debug("with: ", t);
                if (pendingCount.decrementAndGet() == 0)
                    service.yieldRootRequestProcessing((InterpolrFrontendRequest) getRootRequest().getFrontendRequest());
            }

            @Override
            public void complete() {
                try {
                    getInterpolr().processResult(newBorn);
                } finally {
                    if (pendingCount.decrementAndGet() == 0)
                        service.yieldRootRequestProcessing((InterpolrFrontendRequest) getRootRequest().getFrontendRequest());
                }
            }
        });
        newBorn.setRequest(req);
        pendingCount.incrementAndGet();
        service.scheduleSubBackendRequest(req);
        return newBorn;
    }

    public Interpolr getInterpolr() {
        return service.getInterpolr();
    }

    @Override
    public String toString() {
        return rootRequest.toString();
    }

    public InterpolrScope getRootScope() {
        return rootScope;
    }

    @Override
    public int getContentLength() {
        return getRootScope().getParsedDocument().length();
    }

    @Override
    public Object getPluginData(Plugin plugin) {
        return pluginData.get(plugin);
    }

    public void setRootScope(ProxyInterpolrScope rootScope) {
        this.rootScope = rootScope;
    }

    public int getPendingCount() {
        return pendingCount.get();
    }
}
