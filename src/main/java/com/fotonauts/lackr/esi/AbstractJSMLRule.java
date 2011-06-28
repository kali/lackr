package com.fotonauts.lackr.esi;

import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.MimeType;
import com.fotonauts.lackr.esi.filters.JsonQuotingChunk;
import com.fotonauts.lackr.interpolr.Chunk;

public class AbstractJSMLRule extends ESIIncludeRule {

    public AbstractJSMLRule(String pattern) {
        super(pattern);
    }

    @Override
    public String getSyntaxIdentifier() {
        return "ML";
    }

    @Override
    public Chunk filterDocumentAsChunk(LackrBackendExchange exchange) {
        if (exchange.getParsedDocument() == null || exchange.getParsedDocument().length() == 0)
            return NULL_CHUNK;
        return new JsonQuotingChunk(exchange.getParsedDocument(), false);
    }

    @Override
    public void check(LackrBackendExchange exchange) {
        String mimeType = getMimeType(exchange);
        if (MimeType.isJS(mimeType)) {
            exchange.getBackendRequest()
                    .getFrontendRequest()
                    .addBackendExceptions(
                            new LackrPresentableError("unsupported ESI type (js* in js(*ML) context", exchange));
        }
    }
}
