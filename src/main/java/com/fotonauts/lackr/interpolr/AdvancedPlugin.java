package com.fotonauts.lackr.interpolr;

public interface AdvancedPlugin extends Plugin {
    public void start();
    public void stop();
    public void setInterpolr(Interpolr interpolr);
}