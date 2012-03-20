package com.fotonauts.lackr.esi;

import com.fotonauts.lackr.BackendRequest;
import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.MimeType;
import com.fotonauts.lackr.esi.filters.AmpersandEscapeChunk;
import com.fotonauts.lackr.interpolr.Chunk;

public abstract class AbstractMLESIRule extends ESIIncludeRule {

    public AbstractMLESIRule(String markup) {
        super(markup);
    }

    @Override
    public String getSyntaxIdentifier() {
        return "ML";
    }

    @Override
    public Chunk filterDocumentAsChunk(BackendRequest request) {
        String mimeType = getMimeType(request.getExchange());
        // JS is detected by check()
        if (MimeType.isML(mimeType))
            return request.getParsedDocument();
        else
            // so this is most likely plain text
            return new AmpersandEscapeChunk(request.getParsedDocument());
    }

    @Override
    public void check(BackendRequest request) {
        String mimeType = getMimeType(request.getExchange());
        if (MimeType.isJS(mimeType))
            request
                    .getFrontendRequest()
                    .addBackendExceptions(
                            new LackrPresentableError("unsupported ESI type (js* in *ML context)", request.getExchange()));
    }
}
