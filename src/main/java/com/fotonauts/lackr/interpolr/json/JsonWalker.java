package com.fotonauts.lackr.interpolr.json;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class JsonWalker {

    public Object onValue(Object datum) {
        return datum;
    }

    final public void walk(Object data) {
        walk(new HashSet<>(), data);
    }

    @SuppressWarnings("unchecked")
    final private void walk(Set<Object> precursors, Object data) {
        if (precursors.contains(data))
            return;
        try {
            precursors.add(data);

            if (data instanceof List<?>) {
                List<Object> dataAsList = (List<Object>) data;
                boolean shouldChange = false;
                for (Object s : dataAsList) {
                    walk(precursors, s);
                    Object newValue = onValue(s);
                    shouldChange = shouldChange || (newValue != null && newValue != s);
                }
                if (shouldChange) {
                    for (int i = 0; i < dataAsList.size(); i++) {
                        Object resolved = onValue(dataAsList.get(i));
                        if (resolved != null && resolved != dataAsList.get(i))
                            dataAsList.set(i, resolved);
                    }
                }

            } else if (data instanceof Map<?, ?>) {
                Map<String, Object> dataAsMap = (Map<String, Object>) data;
                Map<String, Object> changes = new HashMap<>();
                for (Entry<String, Object> e : dataAsMap.entrySet()) {
                    walk(precursors, e.getValue());
                    Object resolved = onValue(e.getValue());
                    if (resolved != null && resolved != e.getValue())
                        changes.put(e.getKey(), resolved);
                }
                dataAsMap.putAll(changes);
            }
        } finally {
            precursors.remove(data);
        }
    }

}
