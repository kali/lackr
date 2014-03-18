package com.fotonauts.lackr.interpolr.plugins;

import com.fotonauts.lackr.interpolr.Interpolr;

public interface AdvancedPlugin extends Plugin {
    public void start();

    public void stop();

    public void setInterpolr(Interpolr interpolr);
}