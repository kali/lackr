package com.fotonauts.lackr.interpolr.json;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.interpolr.AdvancedPlugin;
import com.fotonauts.lackr.interpolr.Interpolr;
import com.fotonauts.lackr.interpolr.InterpolrContext;
import com.fotonauts.lackr.interpolr.Plugin;
import com.fotonauts.lackr.interpolr.Rule;
import com.fotonauts.lackr.interpolr.handlebars.HandlebarsContext;
import com.fotonauts.lackr.interpolr.handlebars.HandlebarsPlugin;
import com.fotonauts.lackr.interpolr.handlebars.Preprocessor;

public class JsonPlugin implements AdvancedPlugin, Preprocessor {
    
    static Logger log = LoggerFactory.getLogger(JsonPlugin.class);

    private Rule[] rules;
    private Interpolr interpolr;

    public JsonPlugin() {
        rules = new Rule[] { new DumpArchiveRule(this), new ArchiveRule(this) };
    }
    
    @Override
    public Rule[] getRules() {
        return rules;
    }

    @Override
    public Object createContext(InterpolrContext context) {
        return new JsonContext(this, context);
    }

    @Override
    public void preflightCheck(InterpolrContext context) {
        ((JsonContext) context.getPluginData(this)).checkAndCompileAll();
    }

    @Override
    public void preProcess(HandlebarsContext handlebarsContext, Map<String, Object> data) {
        log.debug("preprocess {}", data);
        resolveArchiveReferences(data, handlebarsContext);
        log.debug("preprocess result: {}", data);
    }
    
    private void resolveArchiveReferences(Object data, final HandlebarsContext context) {
        final JsonContext jsonContext = (JsonContext) context.getInterpolrContext().getPluginData(this);
        new JsonWalker() {
            @Override
            public Object resolve(Object datum) {
                if (datum instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> datumAsMap = (Map<String, Object>) datum;
                    if (datumAsMap.containsKey("$$archive") && datumAsMap.containsKey("$$id")) {
                        Archive arch = jsonContext.getArchive((String) datumAsMap.get("$$archive"));
                        if(arch != null)
                            return arch.getObject((Integer) datumAsMap.get("$$id"));
                    }
                }
                return null;
            }
        }.walk(data);
    }

    @Override
    public void start() {
        for(Plugin p: interpolr.getPlugins()) {
            if(p instanceof HandlebarsPlugin) {
                log.info("Registering archive plugin as a HandlebarsPlugin preprocessor.");
                ((HandlebarsPlugin) p).registerPreprocessor(this);
            }
        }
    }

    @Override
    public void stop() {
    }

    @Override
    public void setInterpolr(Interpolr interpolr) {
        this.interpolr = interpolr;
    }


}
