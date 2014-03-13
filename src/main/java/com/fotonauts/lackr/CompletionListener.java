package com.fotonauts.lackr;

public interface CompletionListener {
    public void complete();

    public void fail(Throwable t);
}