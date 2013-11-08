package com.fotonauts.lackr.interpolr;

public interface InterpolrScope {
    InterpolrContext getInterpolrContext();

    Interpolr getInterpolr();

    Document getParsedDocument();
    void setParsedDocument(Document result);

    String getResultMimeType();

    byte[] getBodyBytes();
}
