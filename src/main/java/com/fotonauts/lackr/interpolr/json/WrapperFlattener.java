package com.fotonauts.lackr.interpolr.json;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.fotonauts.lackr.interpolr.handlebars.HandlebarsContext;
import com.fotonauts.lackr.interpolr.handlebars.Preprocessor;
import com.github.jknack.handlebars.Context.Builder;

public class WrapperFlattener implements Preprocessor {

    private String keyname = "$$inline_wrapper";

    public WrapperFlattener() {
    }

    public WrapperFlattener(String keyname) {
        this.keyname = keyname;
    }

    public String getKeyname() {
        return keyname;
    }

    public void setKeyname(String keyname) {
        this.keyname = keyname;
    }

    // { a:blah, b: {
    //   $$inline_wrapper: {
    //     c: 12
    //   }
    // }
    // ======> { a:blah, c: 12}
    @Override
    public void preProcess(final HandlebarsContext handlebarsContext, Map<String, Object> data) {
        new JsonWalker() {
            @SuppressWarnings("unchecked")
            @Override
            public Object onValue(Object datum) {
                if (!(datum instanceof Map<?, ?>))
                    return datum;

                boolean needRewrite = false;
                Map<String, Serializable> dataAsMap = (Map<String, Serializable>) datum;
                for (Serializable value : dataAsMap.values()) {
                    if (!needRewrite && value instanceof Map<?, ?>) {
                        if (((Map<?, ?>) value).containsKey(keyname)) {
                            needRewrite = true;
                        }
                    }
                }

                if (!needRewrite)
                    return datum;

                Map<String, Serializable> result = new HashMap<>();
                for (Entry<String, Serializable> entry : dataAsMap.entrySet()) {
                    if (entry.getValue() instanceof Map<?, ?> && ((Map<?, ?>) (entry.getValue())).containsKey(keyname)
                            && ((Map<?, ?>) (entry.getValue())).get(keyname) instanceof Map<?, ?>) {

                        for (Entry<String, Serializable> innerEntry : ((Map<String, Serializable>) ((Map<?, ?>) (entry.getValue()))
                                .get(keyname)).entrySet())
                            result.put(innerEntry.getKey(), innerEntry.getValue());
                    } else
                        result.put(entry.getKey(), entry.getValue());
                }
                return result;
            }
        }.walk(data);
    }

    @Override
    public Builder preProcess(HandlebarsContext handlebarsContext, Builder builder) {
        return builder;
    };

}
