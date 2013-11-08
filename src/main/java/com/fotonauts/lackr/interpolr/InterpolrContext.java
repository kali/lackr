package com.fotonauts.lackr.interpolr;

import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.mustache.MustacheContext;


public interface InterpolrContext {

    void addBackendExceptions(LackrPresentableError lackrPresentableError);

    InterpolrScope getSubBackendExchange(String url, String syntaxIdentifier, InterpolrScope scope);

    MustacheContext getMustacheContext();

    Interpolr getInterpolr();
    
}
