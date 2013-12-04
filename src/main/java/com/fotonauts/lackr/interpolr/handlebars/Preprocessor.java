package com.fotonauts.lackr.interpolr.handlebars;

import java.util.Map;

import com.github.jknack.handlebars.Context.Builder;

public interface Preprocessor {
    void preProcessData(HandlebarsContext handlebarsContext, Map<String, Object> data);
    Builder preProcessContextBuilder(HandlebarsContext handlebarsContext, Builder builder);
}