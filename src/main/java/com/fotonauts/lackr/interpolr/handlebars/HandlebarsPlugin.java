package com.fotonauts.lackr.interpolr.handlebars;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
    private List<HandlebarsExtension> handlebarsExtensions = new ArrayList<>();

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
        for(HandlebarsExtension prep:handlebarsExtensions) {
            Collection<ValueResolver> r = prep.getValueResolvers(handlebarsContext);
            if(r!=null)
                resolvers.addAll(r);
        }
        resolvers.add(MapValueResolver.INSTANCE);

        Builder contextBuilder = Context
                .newBuilder(data)
                .combine("_ftn_handlebars_context", handlebarsContext)
                .resolver(resolvers.toArray(new ValueResolver[resolvers.size()]));
        
        for(HandlebarsExtension prep:handlebarsExtensions) {
            Map<String,Object> combined = prep.getCombinedValues(handlebarsContext);
            if(combined != null)
                contextBuilder.combine(combined);
        }

        return contextBuilder.build();
    }
    
    public void registerExtension(HandlebarsExtension handlebarsExtension) {
        handlebarsExtensions.add(handlebarsExtension);
    }
    
    public String getPrefix() {
        return prefix;
    }
}
