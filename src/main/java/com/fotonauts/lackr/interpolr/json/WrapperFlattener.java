package com.fotonauts.lackr.interpolr.json;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fotonauts.lackr.interpolr.handlebars.HandlebarsContext;
import com.fotonauts.lackr.interpolr.handlebars.Preprocessor;

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
    
    @Override
    public void preProcess(HandlebarsContext handlebarsContext, Map<String, Object> data) {
        inlineWrapperJsonEvaluation(data);
    }

    @SuppressWarnings("unchecked")
    public void inlineWrapperJsonEvaluation(Object data) {
        if (data instanceof List<?>) {
            List<Serializable> dataAsList = (List<Serializable>) data;
            for (Serializable s : dataAsList)
                inlineWrapperJsonEvaluation(s);

        } else if (data instanceof Map<?, ?>) {
            Map<String, Serializable> dataAsMap = (Map<String, Serializable>) data;
            List<String> keysToRemove = new LinkedList<>();
            Map<String, Serializable> stuffToInline = new HashMap<>();
            for (Entry<String, Serializable> pair : dataAsMap.entrySet()) {
                if (pair.getValue() instanceof Map<?, ?>) {
                    Map<String, Serializable> valueAsMap = (Map<String, Serializable>) pair.getValue();
                    if (valueAsMap.size() == 1 && valueAsMap.containsKey(keyname)) {
                        if (valueAsMap.get(keyname) instanceof Map<?, ?>) {
                            stuffToInline
                                    .putAll((Map<? extends String, ? extends Serializable>) valueAsMap.get(keyname));
                            keysToRemove.add(pair.getKey());
                        }
                    }
                }
            }
            for (String k : keysToRemove)
                dataAsMap.remove(k);
            dataAsMap.putAll(stuffToInline);
            for (Serializable value : dataAsMap.values())
                inlineWrapperJsonEvaluation(value);
        }
    }

}
