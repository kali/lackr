package com.fotonauts.lackr.interpolr;

import java.util.List;

import com.fotonauts.lackr.LackrPresentableError;

public interface InterpolrContext {

    void addError(LackrPresentableError lackrPresentableError);

    InterpolrScope getOrCreateSubScope(String url, String syntaxIdentifier, InterpolrScope parentScope);

    Interpolr getInterpolr();

    InterpolrScope getRootScope();

    List<LackrPresentableError> getErrors();

    Object getPluginData(Plugin plugin);
}
