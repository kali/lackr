package com.fotonauts.lackr.interpolr.plugins;

import com.fotonauts.lackr.interpolr.InterpolrContext;

public interface Plugin {
    public Rule[] getRules();

    public Object createContext(InterpolrContext context);

    public void preflightCheck(InterpolrContext context);
}