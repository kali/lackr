package com.fotonauts.lackr.interpolr;

import com.fotonauts.lackr.interpolr.rope.Document;

public interface InterpolrScope {
    InterpolrContext getInterpolrContext();

    Interpolr getInterpolr();

    Document getParsedDocument();

    void setParsedDocument(Document result);

    String getResultMimeType();

    byte[] getBodyBytes();
}
