package com.fotonauts.lackr.picorassets;

public interface AssetResolver {
    public String resolve(String asset);

    public abstract String getMagicPrefix();
}
