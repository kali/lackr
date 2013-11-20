package com.fotonauts.lackr.interpolr;

public interface Plugin {
    public Rule[] getRules();
    public Object createContext(InterpolrContext context);
    public void preflightCheck(InterpolrContext context);
}