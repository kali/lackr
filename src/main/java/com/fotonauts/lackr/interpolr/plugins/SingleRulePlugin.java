package com.fotonauts.lackr.interpolr.plugins;

import com.fotonauts.lackr.interpolr.InterpolrContext;

public class SingleRulePlugin implements Plugin {

    private Rule[] rule;

    public SingleRulePlugin(Rule rule) {
        this.rule = new Rule[] { rule };
    }

    @Override
    public Rule[] getRules() {
        return rule;
    }

    @Override
    public Object createContext(InterpolrContext context) {
        return null;
    }

    @Override
    public void preflightCheck(InterpolrContext context) {
    }

}
