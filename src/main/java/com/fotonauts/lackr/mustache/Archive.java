package com.fotonauts.lackr.mustache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Archive {

    private Map<String, Object> data;
    private Map<Integer, Object> straightIndex = new HashMap<Integer, Object>();

    public Archive(Map<String, Object> data) {
        this.data = data;
        process();
    }
    
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
    
    private void process() {
        simplifyJavascriptObjects(data);
        Map<String,Object> objects = (Map<String, Object>) data.get("objects");
        for(Entry<String, Object> entry : objects.entrySet()) {
            straightIndex.put(Integer.parseInt(entry.getKey()), entry.getValue());
        }
    }

    public Map<String, Object> getObject(int objectId) {
        return (Map<String, Object>) straightIndex.get(objectId);
    }

    public Object getData() {
        return data;
    }

}
