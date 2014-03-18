package com.fotonauts.lackr.interpolr;

import java.util.List;

import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.interpolr.plugins.Plugin;

public interface InterpolrContext {

    void addError(LackrPresentableError lackrPresentableError);

    InterpolrScope getOrCreateSubScope(String url, String syntaxIdentifier, InterpolrScope parentScope);

    Interpolr getInterpolr();

    InterpolrScope getRootScope();

    List<LackrPresentableError> getErrors();

    Object getPluginData(Plugin plugin);
}
