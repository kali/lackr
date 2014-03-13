package com.fotonauts.lackr.interpolr.json;

import java.util.Map;

class ArchiveValueResolverExtension implements ExtendedMapValueResolver.ValueResolverExtension {

    private JsonContext jsonContext;

    public ArchiveValueResolverExtension(JsonContext jsonContext) {
        this.jsonContext = jsonContext;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getInnerData(Map<String, Object> context) {
        if (context instanceof Map<?, ?> && ((Map<String, Object>) context).containsKey("$$archive")
                && ((Map<String, Object>) context).containsKey("$$id")) {
            Map<String, Object> datumAsMap = (Map<String, Object>) context;
            Archive arch = jsonContext.getArchive((String) datumAsMap.get("$$archive"));
            if (arch == null)
                return null;
            Object target = arch.getObject((Integer) datumAsMap.get("$$id"));
            if (target instanceof Map<?, ?>)
                return (Map<String, Object>) target;
        }
        return null;
    }

}