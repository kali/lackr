package com.fotonauts.lackr.interpolr.handlebars;

import java.util.Collection;

import com.github.jknack.handlebars.ValueResolver;

public interface ValueResolverProvider {
    Collection<ValueResolver> provide(HandlebarsContext handlebarsContext);
}