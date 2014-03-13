package com.fotonauts.lackr.interpolr;

public class SimpleInterpolrScope implements InterpolrScope {

    protected InterpolrContext context;
    protected Document parsedDocument;
    protected String mimeType;
    private byte[] bodyBytes;

    public SimpleInterpolrScope(InterpolrContext context, byte[] bodyBytes, String mimeType) {
        this.context = context;
        this.mimeType = mimeType;
        this.bodyBytes = bodyBytes;
    }

    @Override
    public InterpolrContext getInterpolrContext() {
        return context;
    }

    @Override
    public Interpolr getInterpolr() {
        return context.getInterpolr();
    }

    @Override
    public Document getParsedDocument() {
        return parsedDocument;
    }

    @Override
    public String getResultMimeType() {
        return mimeType;
    }

    @Override
    public void setParsedDocument(Document result) {
        parsedDocument = result;
    }

    @Override
    public byte[] getBodyBytes() {
        return bodyBytes;
    }

    @Override
    public String toString() {
        if (parsedDocument != null)
            return "<<" + parsedDocument.toDebugString() + ">>";
        else
            return new String(bodyBytes);
    }
}