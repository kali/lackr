package com.fotonauts.lackr.interpolr.proxy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

    protected InterpolrProxy service;

    private ConcurrentHashMap<String, InterpolrScope> backendRequestCache = new ConcurrentHashMap<String, InterpolrScope>();
    private Set<String> pendingQueries = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private ProxyInterpolrScope rootScope;

    protected HashMap<Plugin, Object> pluginData = new HashMap<>();

    protected InterpolrFrontendRequest(final InterpolrProxy baseProxy, HttpServletRequest request) {
        super(baseProxy, request);
        this.service = baseProxy;
        for (Plugin p : baseProxy.getInterpolr().getPlugins()) {
            pluginData.put(p, p.createContext(this));
        }
    }

    public InterpolrScope getOrCreateSubScope(String url, String format, InterpolrScope dad) {
        log.debug("{} requires {} (as {})", dad, url, format);
        final String key = format + "::" + url;
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
                pendingQueries.remove(key);
                if (pendingQueries.isEmpty())
                    service.yieldRootRequestProcessing((InterpolrFrontendRequest) getBackendRequest().getFrontendRequest());
            }

            @Override
            public void complete() {
                try {
                    getInterpolr().processResult(newBorn);
                } finally {
                    pendingQueries.remove(key);
                    if (pendingQueries.isEmpty())
                        service.yieldRootRequestProcessing((InterpolrFrontendRequest) getBackendRequest().getFrontendRequest());
                }
            }
        });
        newBorn.setRequest(req);
        pendingQueries.add(key);
        service.scheduleSubBackendRequest(req);
        return newBorn;
    }

    public Interpolr getInterpolr() {
        return service.getInterpolr();
    }

    @Override
    public String toString() {
        return backendRequest.toString();
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
        return pendingQueries.size();
    }

    public String dumpCurrentState() {
        StringBuilder builder = new StringBuilder();
        for (Entry<String, InterpolrScope> subQuery : backendRequestCache.entrySet()) {
            builder.append(String.format("- %s %s\n", pendingQueries.contains(subQuery.getKey()) ? "[WAITING]" : "[   ok  ]",
                    subQuery.getKey()));
        }
        return builder.toString();
    }
}
