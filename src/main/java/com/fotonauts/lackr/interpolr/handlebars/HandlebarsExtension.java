package com.fotonauts.lackr.interpolr.handlebars;

import java.util.Collection;
import java.util.Map;

import com.github.jknack.handlebars.ValueResolver;

public interface HandlebarsExtension {

    Collection<ValueResolver> getValueResolvers(HandlebarsContext handlebarsContext);

    Map<String, Object> getCombinedValues(HandlebarsContext handlebarsContext);

    public static class Noop implements HandlebarsExtension {

        @Override
        public Collection<ValueResolver> getValueResolvers(HandlebarsContext handlebarsContext) {
            return null;
        }

        @Override
        public Map<String, Object> getCombinedValues(HandlebarsContext handlebarsContext) {
            return null;
        }

    }
}