package com.fotonauts.lackr.interpolr.json;

import java.util.HashMap;
import java.util.Map;

public class InlineWrapperValueResolverExtension implements ExtendedMapValueResolver.ValueResolverExtension {

    private String keyname = "$$inline_wrapper";

    public InlineWrapperValueResolverExtension() {
    }

    public InlineWrapperValueResolverExtension(String keyname) {
        this.keyname = keyname;
    }

    public void setKeyname(String keyname) {
        this.keyname = keyname;
    }

    // { a:blah, b: {
    //   $$inline_wrapper: {
    //     c: 12
    //   }
    // }
    // ======> { c: 12 }
    @SuppressWarnings("unchecked")
    @Override
    public Map<String,Object> getInnerData(Map<String,Object> context) {
        if (!(context instanceof Map<?, ?>))
            return null;
        HashMap<String,Object> map = new HashMap<>();
        for(Object value: ((Map<String,Object>) context).values()) {
            if(value instanceof Map<?,?> && ((Map<String,Object>) value).get(keyname) instanceof Map<?,?>) {
                map.putAll((Map<String,Object>) ((Map<String,Object>) value).get(keyname));
            }
        }
        return map;
    };

}
