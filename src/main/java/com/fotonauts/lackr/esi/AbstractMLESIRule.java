package com.fotonauts.lackr.esi;

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
    public Chunk filterDocumentAsChunk(LackrBackendExchange exchange) {
        String mimeType = getMimeType(exchange);
        // JS is detected by check()
        if (MimeType.isML(mimeType))
            return exchange.getParsedDocument();
        else
            // so this is most likely plain text
            return new AmpersandEscapeChunk(exchange.getParsedDocument());
    }

    @Override
    public void check(LackrBackendExchange exchange) {
        String mimeType = getMimeType(exchange);
        if (MimeType.isJS(mimeType))
            exchange.getBackendRequest()
                    .getFrontendRequest()
                    .addBackendExceptions(
                            new LackrPresentableError("unsupported ESI type (js* in *ML context)", exchange));
    }
}
