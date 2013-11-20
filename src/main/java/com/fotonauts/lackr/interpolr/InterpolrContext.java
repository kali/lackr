package com.fotonauts.lackr.interpolr;

import java.util.List;

import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.mustache.HandlebarsContext;


public interface InterpolrContext {

    //FIXME rename me
    void addBackendExceptions(LackrPresentableError lackrPresentableError);

    //FIXME rename me
    InterpolrScope getSubBackendExchange(String url, String syntaxIdentifier, InterpolrScope scope);

    HandlebarsContext getMustacheContext();

    Interpolr getInterpolr();

    InterpolrScope getRootScope();

    //FIXME rename me
    List<LackrPresentableError> getBackendExceptions();
    
}
