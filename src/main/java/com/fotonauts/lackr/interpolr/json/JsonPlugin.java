package com.fotonauts.lackr.interpolr.json;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import com.github.jknack.handlebars.ValueResolver;
import com.github.jknack.handlebars.Context.Builder;
import com.github.jknack.handlebars.context.MapValueResolver;

public class JsonPlugin implements AdvancedPlugin {

    static class ArchiveResolver implements ValueResolver {

        @SuppressWarnings("unchecked")
        @Override
        public Object resolve(Object context, String name) {
            if (context instanceof Reference) {
                Object target = ((Reference) context).getTarget();
                if (target instanceof Map<?, ?>) {
                    if(((Map) target).containsKey(name))
                        return ((Map<String,Object>) target).get(name);
                }
            }
            return UNRESOLVED;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Set<Entry<String, Object>> propertySet(Object context) {
            if (context instanceof Reference) {
                Object target = ((Reference) context).getTarget();
                if (target instanceof Map<?, ?>) {
                    return ((Map<String, Object>) target).entrySet();
                }
            }
            return Collections.emptySet();
        }

    }

    static Logger log = LoggerFactory.getLogger(JsonPlugin.class);

    static ArchiveResolver resolverSingleton = new ArchiveResolver();

    private Rule[] rules;
    private Interpolr interpolr;
    private String archiveCaptureTrigger = "<script type=\"vnd.fotonauts/lackrarchive\" id=\"*\">*</script><!-- END OF ARCHIVE -->";

    public JsonPlugin(String archiveCaptureTrigger) {
        this.archiveCaptureTrigger = archiveCaptureTrigger;
        buildRules();
    }

    public JsonPlugin() {
        buildRules();
    }

    private void buildRules() {
        rules = new Rule[] { new DumpArchiveRule(this), new ArchiveRule(this, archiveCaptureTrigger) };
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

    private void resolveArchiveReferences(Object data, final HandlebarsContext context) {
        final JsonContext jsonContext = (JsonContext) context.getInterpolrContext().getPluginData(this);
        new JsonWalker() {
            @Override
            public Object onValue(Object datum) {
                if (datum instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> datumAsMap = (Map<String, Object>) datum;
                    if (datumAsMap.containsKey("$$archive") && datumAsMap.containsKey("$$id")) {
                        Archive arch = jsonContext.getArchive((String) datumAsMap.get("$$archive"));
                        if (arch != null)
                            return new Reference(arch, (Integer) datumAsMap.get("$$id"));
                    }
                }
                return null;
            }
        }.walk(data);
    }

    @Override
    public void start() {
        for (Plugin p : interpolr.getPlugins()) {
            if (p instanceof HandlebarsPlugin) {
                log.debug("Registering archive plugin as a HandlebarsPlugin preprocessor.");
                ((HandlebarsPlugin) p).registerPreprocessor(new Preprocessor() {

                    @Override
                    public Builder preProcessContextBuilder(HandlebarsContext handlebarsContext, Builder builder) {
                        return builder.resolver(MapValueResolver.INSTANCE, resolverSingleton);
                    }

                    @Override
                    public void preProcessData(HandlebarsContext handlebarsContext, Map<String, Object> data) {
                        log.debug("preprocess {}", data);
                        resolveArchiveReferences(data, handlebarsContext);
                        log.debug("preprocess result: {}", data);
                    }

                });
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
