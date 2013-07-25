package com.fotonauts.lackr.mustache.helpers;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.mustache.MustacheContext;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.StringTemplateSource;

public class MiscelaneousHelpers {

    static Logger log = LoggerFactory.getLogger(MiscelaneousHelpers.class);

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
        if(targetAsObject == null)
            return "";
        @SuppressWarnings("unchecked")
        Map<String, Object> target = (Map<String, Object>) targetAsObject;
        MustacheContext mustacheContext = (MustacheContext) options.context.get("_ftn_mustache_context");
        String templateString = (String) target.get("wrapped_mustache_template");

        if (log.isDebugEnabled()) {
            log.debug(String.format("Rendering template \"%s\"", templateString));
        }
        Template template;
        try {
            template = mustacheContext.getHandlebars().compile(new StringTemplateSource("inner view", templateString));
        } catch (Throwable e) {
            mustacheContext.getLackrFrontendRequest().addBackendExceptions(e);
            return "";
        }
        try {
            return mustacheContext.eval(template, target);
        } catch (Throwable e) {
            return "";
        }
    }

    public static CharSequence dom_compatible_id(Object targetAsObject, Options options) {
        if(targetAsObject == null)
            return "";
        String input = targetAsObject.toString();
        StringBuilder builder = new StringBuilder("_");
        for(char c: input.toCharArray()) {
            switch (c) {
                case ' ': builder.append("_spc_"); break;
                case '!': builder.append("_bang_"); break;
                case ':': builder.append("_colon_"); break;
                case '.': builder.append("_dot_"); break;
                case '#': builder.append("_hash_"); break;
                case '(': builder.append("_lpar_"); break;
                case ')': builder.append("_rpar_"); break;
                case '/': builder.append("_slash_"); break;
                case '\'': builder.append("_q_"); break;
                case '&': builder.append("_amp_"); break;
                case '-': builder.append("_dash_"); break;
                case '_': builder.append("_under_"); break;
                default: builder.append(c); break;
            }
        }
        return builder.toString();
    }
    
}
