package com.fotonauts.lackr.interpolr.json;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public abstract class JsonWalker {

    public abstract Object resolve(Object datum);

    @SuppressWarnings("unchecked")
    public void walk(Object data) {
        if (data instanceof List<?>) {
            List<Object> dataAsList = (List<Object>) data;
            boolean shouldChange = false;
            for (Object s : dataAsList) {
                walk(s);
                shouldChange = shouldChange || resolve(s) != null;
            }
            if (shouldChange) {
                for (int i = 0; i < dataAsList.size(); i++) {
                    Object resolved = resolve(dataAsList.get(i));
                    if (resolved != null)
                        dataAsList.set(i, resolved);
                }
            }

        } else if (data instanceof Map<?, ?>) {
            Map<String, Object> dataAsMap = (Map<String, Object>) data;
            Map<String, Object> changes = new HashMap<>();
            for (Entry<String, Object> e : dataAsMap.entrySet()) {
                walk(e.getValue());
                Object resolved = resolve(e.getValue());
                if (resolved != null)
                    changes.put(e.getKey(), resolved);
            }
            dataAsMap.putAll(changes);
        }

    }

}
