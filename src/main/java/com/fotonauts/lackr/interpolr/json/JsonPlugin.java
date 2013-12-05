package com.fotonauts.lackr.interpolr.json;

import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.interpolr.AdvancedPlugin;
import com.fotonauts.lackr.interpolr.Interpolr;
import com.fotonauts.lackr.interpolr.InterpolrContext;
import com.fotonauts.lackr.interpolr.Plugin;
import com.fotonauts.lackr.interpolr.Rule;
import com.fotonauts.lackr.interpolr.handlebars.HandlebarsContext;
import com.fotonauts.lackr.interpolr.handlebars.HandlebarsPlugin;
import com.fotonauts.lackr.interpolr.handlebars.ValueResolverProvider;
import com.github.jknack.handlebars.ValueResolver;

public class JsonPlugin implements AdvancedPlugin {

    static Logger log = LoggerFactory.getLogger(JsonPlugin.class);

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

    @Override
    public void start() {
        for (Plugin p : interpolr.getPlugins()) {
            if (p instanceof HandlebarsPlugin) {
                log.debug("Registering archive plugin as a HandlebarsPlugin preprocessor.");
                ((HandlebarsPlugin) p).registerPreprocessor(new ValueResolverProvider() {

                    @Override
                    public Collection<ValueResolver> provide(HandlebarsContext handlebarsContext) {
                        JsonContext jsonContext = (JsonContext) handlebarsContext.getInterpolrContext().getPluginData(
                                JsonPlugin.this);
                        ArrayList<ValueResolver> list = new ArrayList<>(1);
                        list.add(new ExtendedMapValueResolver(new ArchiveValueResolverExtension(jsonContext),
                                new InlineWrapperValueResolverExtension(), new ATTRValueResolverExtension()));
                        return list;
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
