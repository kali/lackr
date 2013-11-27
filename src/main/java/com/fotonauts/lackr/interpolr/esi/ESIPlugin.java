package com.fotonauts.lackr.interpolr.esi;

import com.fotonauts.lackr.interpolr.InterpolrContext;
import com.fotonauts.lackr.interpolr.Plugin;
import com.fotonauts.lackr.interpolr.Rule;

public class ESIPlugin implements Plugin {

    private Rule[] rules = new Rule[] { new HttpESIRule(), new JSESIRule(), new JSEscapedMLESIRule(), new JSMLESIRule(),
            new MLESIRule() };

    @Override
    public Rule[] getRules() {
        return rules;
    }

    @Override
    public Object createContext(InterpolrContext context) {
        return null;
    }

    @Override
    public void preflightCheck(InterpolrContext context) {
    }
}
