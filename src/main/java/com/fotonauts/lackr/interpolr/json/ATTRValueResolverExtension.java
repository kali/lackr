package com.fotonauts.lackr.interpolr.json;

import java.util.Map;

public class ATTRValueResolverExtension implements ExtendedMapValueResolver.ValueResolverExtension {

    private String keyname = "$ATTR";

    public ATTRValueResolverExtension() {
    }

    public ATTRValueResolverExtension(String keyname) {
        this.keyname = keyname;
    }

    public void setKeyname(String keyname) {
        this.keyname = keyname;
    }

    // { $ATTR : { c: 12 } }
    // ======> { c: 12 }
    @Override
    @SuppressWarnings("unchecked")
        public Map<String, Object> getInnerData(Map<String, Object> context) {
        if (!(context instanceof Map<?, ?>))
            return null;
        Map<String, Object> map = (Map<String, Object>) context;
        if (map.containsKey(keyname) && map.get(keyname) instanceof Map<?,?>)
            return (Map<String, Object>) map.get(keyname);
        else
            return null;
    }

}
