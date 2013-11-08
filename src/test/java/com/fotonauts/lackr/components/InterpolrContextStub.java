package com.fotonauts.lackr.components;

import java.util.LinkedList;
import java.util.List;

import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.interpolr.Interpolr;
import com.fotonauts.lackr.interpolr.InterpolrContext;
import com.fotonauts.lackr.interpolr.InterpolrScope;
import com.fotonauts.lackr.mustache.MustacheContext;

public class InterpolrContextStub implements InterpolrContext {

    protected MustacheContext mustache = new MustacheContext(this);
    protected List<LackrPresentableError> errors = new LinkedList<>();
    protected Interpolr interpolr;

    public InterpolrContextStub(Interpolr interpolr) {
        this.interpolr = interpolr;
    }

    @Override
    public void addBackendExceptions(LackrPresentableError lackrPresentableError) {
        errors.add(lackrPresentableError);
    }

    @Override
    public InterpolrScope getSubBackendExchange(String url, String syntaxIdentifier, InterpolrScope scope) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MustacheContext getMustacheContext() {
        return mustache;
    }

    @Override
    public Interpolr getInterpolr() {
        return interpolr;
    }

}