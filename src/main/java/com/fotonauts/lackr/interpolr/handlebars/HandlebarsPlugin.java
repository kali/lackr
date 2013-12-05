package com.fotonauts.lackr.interpolr.handlebars;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.interpolr.InterpolrContext;
import com.fotonauts.lackr.interpolr.Plugin;
import com.fotonauts.lackr.interpolr.Rule;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Context.Builder;
import com.github.jknack.handlebars.ValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;

public class HandlebarsPlugin implements Plugin {

    static Logger log = LoggerFactory.getLogger(HandlebarsPlugin.class);

    private Rule[] rules;
    private List<ValueResolverProvider> valueResolverProviders = new ArrayList<>();

    private String prefix = "lackr:handlebars";

    public HandlebarsPlugin(String prefix) {
        this.prefix = prefix;
        buildRules();
    }

    public HandlebarsPlugin() {
        buildRules();
    }

    private void buildRules() {
        rules = new Rule[] { new TemplateRule(this), new EvalRule(this) };
    }
    
    @Override
    public Rule[] getRules() {
        return rules;
    }

    @Override
    public Object createContext(InterpolrContext context) {
        return new HandlebarsContext(this, context);
    }

    @Override
    public void preflightCheck(InterpolrContext context) {
        HandlebarsContext hbsContext = (HandlebarsContext) context.getPluginData(this);
        hbsContext.checkAndCompileAll();
    }

    public Context makeHbsContext(HandlebarsContext handlebarsContext, Object data) {
        
        ArrayList<ValueResolver> resolvers = new ArrayList<>();
        for(ValueResolverProvider prep:valueResolverProviders) {
            resolvers.addAll(prep.provide(handlebarsContext));
        }
        resolvers.add(MapValueResolver.INSTANCE);

        Builder contextBuilder = Context
                .newBuilder(data)
                .combine("_ftn_handlebars_context", handlebarsContext)
                .resolver(resolvers.toArray(new ValueResolver[resolvers.size()]));

        return contextBuilder.build();
    }
    
    public void registerPreprocessor(ValueResolverProvider valueResolverProvider) {
        log.debug("registering preprocessor {}", valueResolverProvider);
        valueResolverProviders.add(valueResolverProvider);
    }
    
    public String getPrefix() {
        return prefix;
    }
}
