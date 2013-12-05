package com.fotonauts.lackr.mustache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Archive {

    private Map<String, Object> data;
    private final Map<Integer, Object> straightIndex = new HashMap<Integer, Object>();

    public Archive(Map<String, Object> data) {
        this.data = data;
        process();
    }
    
    @SuppressWarnings("unchecked")
    private void simplifyJavascriptObjects(Object object) {
        if(object instanceof Map<?,?>) {
            Map<String,Object> hash = (Map<String, Object>) object;
            if(hash.containsKey("$ATTR")) {
                Map<String, Object> attrs = (Map<String, Object>) hash.get("$ATTR");
                hash.clear();
                hash.putAll(attrs);
            }
            for(Object value : hash.values())
                simplifyJavascriptObjects(value);
        } else if(object instanceof List<?>) {
            for(Object value : ((List<Object>) object))
                simplifyJavascriptObjects(value);            
        }
    }
    
    @SuppressWarnings("unchecked")
    private void process() {
        simplifyJavascriptObjects(data);
        ParsedJsonChunk.inlineWrapperJsonEvaluation(data);
        Map<String,Object> objects = (Map<String, Object>) data.get("objects");
        for(Entry<String, Object> entry : objects.entrySet()) {
            straightIndex.put(Integer.parseInt(entry.getKey()), entry.getValue());
        }
        new ReferenceResolverWalker() {
            @Override
            public Object resolve(Object datum) {
                if(datum instanceof Map<?,?>) {
                    Map<String,Object> hash = (Map<String, Object>) datum;
                    if(hash.containsKey("$$id") && hash.size() == 1) {
                        return straightIndex.get(hash.get("$$id"));
                    }
                }
                return null;
            }
        }.walk(data);
    }

    public Object getObject(int objectId) {
        return straightIndex.get(objectId);
    }

    public Object getData() {
        return data;
    }

    public Object getRootObject() {
        Integer i = getRootId();
        if(i == null)
            return null;
        return getObject(i);
    }

    public Integer getRootId() {
        Object r = data.get("root_id");
        if(r instanceof Integer)
            return (Integer) r;
        return null;
    }

}
