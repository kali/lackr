package com.fotonauts.lackr.interpolr.handlebars;

import java.util.Map;

public interface Preprocessor {
    void preProcess(HandlebarsContext handlebarsContext, Map<String, Object> data);
}