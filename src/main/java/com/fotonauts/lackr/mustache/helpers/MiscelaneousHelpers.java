package com.fotonauts.lackr.mustache.helpers;

import java.util.Map;

import com.fotonauts.lackr.mustache.MustacheContext;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.StringTemplateSource;

public class MiscelaneousHelpers {

    public static CharSequence humanize_integer(Object numberAsObject, Options options) {
        if (numberAsObject == null)
            return "";
        if (numberAsObject instanceof Number) {
            long n = ((Number) numberAsObject).longValue();
            if (n >= 10_000_000) {
                return (n / 1_000_000) + "M";
            } else if (n >= 10_000) {
                return (n / 1_000) + "k";
            }
        }
        return numberAsObject.toString();
    }

    public static CharSequence tag_subview(Object targetAsObject, Options options) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> target = (Map<String, Object>) targetAsObject;
            MustacheContext mustacheContext = (MustacheContext) options.context.get("_ftn_mustache_context");

            String templateString = (String) target.get("mustache_template");

            Template template = mustacheContext.getHandlebars().compile(new StringTemplateSource("inner view", templateString));
            return mustacheContext.eval(template, target);
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            return "";
        }
    }

}
