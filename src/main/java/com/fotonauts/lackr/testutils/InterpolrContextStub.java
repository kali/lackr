package com.fotonauts.lackr.testutils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.interpolr.Interpolr;
import com.fotonauts.lackr.interpolr.InterpolrContext;
import com.fotonauts.lackr.interpolr.InterpolrScope;
import com.fotonauts.lackr.interpolr.Plugin;

public class InterpolrContextStub implements InterpolrContext {
    static Logger log = LoggerFactory.getLogger(InterpolrContextStub.class);

    protected List<LackrPresentableError> errors = new LinkedList<>();
    protected Interpolr interpolr;
    protected InterpolrScope rootScope;
    protected Map<Plugin, Object> pluginData = new HashMap<>();

    public InterpolrContextStub(Interpolr interpolr) {
        this.interpolr = interpolr;
        for (Plugin p : interpolr.getPlugins())
            pluginData.put(p, p.createContext(this));
    }

    @Override
    public void addError(LackrPresentableError lackrPresentableError) {
        log.debug(lackrPresentableError.getMessage(), lackrPresentableError);
        errors.add(lackrPresentableError);
    }

    @Override
    public InterpolrScope getOrCreateSubScope(String url, String syntaxIdentifier, InterpolrScope scope) {
        throw new RuntimeException("not implemented, you need to subclass InterpolrContextStub");
    }

    @Override
    public Interpolr getInterpolr() {
        return interpolr;
    }

    @Override
    public InterpolrScope getRootScope() {
        return rootScope;
    }

    public void setRootScope(InterpolrScope rootScope) {
        this.rootScope = rootScope;
    }

    @Override
    public List<LackrPresentableError> getErrors() {
        return errors;
    }

    @Override
    public Object getPluginData(Plugin plugin) {
        return pluginData.get(plugin);
    }
}