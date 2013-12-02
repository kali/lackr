package com.fotonauts.lackr.interpolr.handlebars;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.interpolr.InterpolrContext;
import com.fotonauts.lackr.interpolr.Plugin;
import com.fotonauts.lackr.interpolr.Rule;

public class HandlebarsPlugin implements Plugin {

    static Logger log = LoggerFactory.getLogger(HandlebarsPlugin.class);

    private Rule[] rules;
    private List<Preprocessor> preprocessors = new ArrayList<>();

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

    @SuppressWarnings("unchecked")
    public Map<String, Object>  preProcess(HandlebarsContext handlebarsContext, Map<String, Object> data) {
        log.debug("preprocess {} with {} preprocessors", data, preprocessors.size());
        
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("root", data);
        for(Preprocessor prep: preprocessors) {
            try {
                prep.preProcess(handlebarsContext, wrapper);
            } catch (Throwable e) {
                throw LackrPresentableError.fromThrowable(e);
            }
        }
        return (Map<String, Object>) wrapper.get("root");
    }
    
    public void registerPreprocessor(Preprocessor preprocessor) {
        log.debug("registering preprocessor {}", preprocessor);
        preprocessors.add(preprocessor);
    }
    
    public String getPrefix() {
        return prefix;
    }
}
