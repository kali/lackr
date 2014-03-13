package com.fotonauts.lackr.interpolr.esi;

import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.MimeType;
import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.InterpolrScope;
import com.fotonauts.lackr.interpolr.esi.codec.AmpersandEscapeChunk;

public abstract class AbstractMLESIRule extends ESIIncludeRule {

    public AbstractMLESIRule(String markup) {
        super(markup);
    }

    @Override
    public String getSyntaxIdentifier() {
        return "ML";
    }

    @Override
    public Chunk filterDocumentAsChunk(InterpolrScope scope) {
        String mimeType = scope.getResultMimeType();
        // JS is detected by check()
        if (MimeType.isML(mimeType))
            return scope.getParsedDocument();
        else
            // so this is most likely plain text
            return new AmpersandEscapeChunk(scope.getParsedDocument());
    }

    @Override
    public void check(InterpolrScope scope) {
        String mimeType = scope.getResultMimeType();
        if (MimeType.isJS(mimeType))
            scope.getInterpolrContext().addError(
                    new LackrPresentableError("unsupported ESI type (js* in *ML context): " + scope.toString()));
    }
}
