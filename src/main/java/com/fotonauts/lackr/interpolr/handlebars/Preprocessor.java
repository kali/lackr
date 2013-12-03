package com.fotonauts.lackr.interpolr.handlebars;

import java.util.Map;

import com.github.jknack.handlebars.Context.Builder;

public interface Preprocessor {
    void preProcess(HandlebarsContext handlebarsContext, Map<String, Object> data);
    Builder preProcess(HandlebarsContext handlebarsContext, Builder builder);
}