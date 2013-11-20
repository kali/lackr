package com.fotonauts.lackr.components;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.interpolr.Interpolr;
import com.fotonauts.lackr.interpolr.InterpolrContext;
import com.fotonauts.lackr.interpolr.InterpolrScope;
import com.fotonauts.lackr.mustache.HandlebarsContext;

public class InterpolrContextStub implements InterpolrContext {
    static Logger log = LoggerFactory.getLogger(InterpolrContextStub.class);

    protected HandlebarsContext mustache = new HandlebarsContext(this);
    protected List<LackrPresentableError> errors = new LinkedList<>();
    protected Interpolr interpolr;
    protected InterpolrScope rootScope;

    public InterpolrContextStub(Interpolr interpolr) {
        this.interpolr = interpolr;
    }

    @Override
    public void addBackendExceptions(LackrPresentableError lackrPresentableError) {
        log.debug(lackrPresentableError.getMessage(), lackrPresentableError);
        errors.add(lackrPresentableError);
    }

    @Override
    public InterpolrScope getSubBackendExchange(String url, String syntaxIdentifier, InterpolrScope scope) {
        throw new RuntimeException("not implemented, you need to subclass InterpolrContextStub");
    }

    @Override
    public HandlebarsContext getMustacheContext() {
        return mustache;
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
    public List<LackrPresentableError> getBackendExceptions() {
        return errors;
    }
}