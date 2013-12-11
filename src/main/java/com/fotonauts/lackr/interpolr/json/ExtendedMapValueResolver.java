package com.fotonauts.lackr.interpolr.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jknack.handlebars.ValueResolver;

public class ExtendedMapValueResolver implements ValueResolver {

    static Logger log = LoggerFactory.getLogger(ExtendedMapValueResolver.class);

    static interface ValueResolverExtension {
        Map<String, Object> getInnerData(Map<String, Object> context);
    }

    public ExtendedMapValueResolver() {
    }

    public ExtendedMapValueResolver(ValueResolverExtension... extensions) {
        this.extensions = Arrays.asList(extensions);
    }
    
    public ExtendedMapValueResolver(List<ValueResolverExtension> extensions) {
        this.extensions = extensions;
    }

    List<ValueResolverExtension> extensions = new ArrayList<>();

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getInnerData(Object context) {
        Map<String, Object> current = new HashMap<String, Object>();
        current.putAll((Map<? extends String, ? extends Object>) context);
        int sizeLastLoop;
        do {
            sizeLastLoop = current.size();
            for (ValueResolverExtension ext : extensions) {
                Map<String,Object> tmp = ext.getInnerData(current); 
                if(tmp != null) 
                    current.putAll(tmp);
            }
        } while (sizeLastLoop < current.size());
        log.debug("INNER DATA FOR {} -> {}", context, current);
        return current;
    }

    @Override
    public Object resolve(Object context, String name) {
        Map<String, Object> inner = getInnerData(context);
        Object result = inner.containsKey(name) ? inner.get(name) : UNRESOLVED;
        return result;
    }

    @Override
    public Set<Entry<String, Object>> propertySet(Object context) {
        return getInnerData(context).entrySet();
    }

}