package com.fotonauts.lackr.interpolr.json;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.interpolr.Document;
import com.fotonauts.lackr.interpolr.InterpolrContext;
import com.fotonauts.lackr.interpolr.JsonParseUtils;

public class JsonContext {
    static Logger log = LoggerFactory.getLogger(JsonContext.class);

    protected JsonPlugin plugin;
    protected Map<String, Document> registeredArchiveDocuments;
    protected Map<String, Archive> expandedArchives;
    protected InterpolrContext interpolrContext;

    public JsonContext(JsonPlugin jsonPlugin, InterpolrContext context) {
        this.plugin = jsonPlugin;
        this.interpolrContext = context;
        registeredArchiveDocuments = Collections.synchronizedMap(new HashMap<String, Document>());
        expandedArchives = new HashMap<String, Archive>();

    }

    public void checkAndCompileAll() {
        log.debug("checkAndCompileAll");
        for (Entry<String, Document> registered : registeredArchiveDocuments.entrySet()) {
            registered.getValue().check();
            Map<String, Object> parsedData = JsonParseUtils.parse(registered.getValue(), interpolrContext, registered.getKey());
            if (parsedData != null)
                expandedArchives.put(registered.getKey(), new Archive(parsedData));
        }
    }

    public void registerArchive(String name, Document archive) {
        log.debug("registerArchive({}, {})", name, archive);
        registeredArchiveDocuments.put(name, archive);
    }

    public Archive getArchive(String name) {
        return expandedArchives.get(name);
    }
    
    public Set<String> getAllArchiveNames() {
        return expandedArchives.keySet();
    }

}
