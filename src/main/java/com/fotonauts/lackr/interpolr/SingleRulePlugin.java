package com.fotonauts.lackr.interpolr;

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
