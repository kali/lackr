package com.fotonauts.lackr.esi;

import com.fotonauts.lackr.BackendRequest;
import com.fotonauts.lackr.BackendRequest.Target;
import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.MimeType;
import com.fotonauts.lackr.esi.filters.JsonQuotingChunk;
import com.fotonauts.lackr.interpolr.Chunk;

public class AbstractJSESIRule extends ESIIncludeRule {

	private Target target;

	public AbstractJSESIRule(String pattern, BackendRequest.Target target) {
		super(pattern);
		this.target = target;
	}
	
	@Override
	protected BackendRequest.Target getTarget() {
		return target;
	}

	@Override
    public Chunk filterDocumentAsChunk(LackrBackendExchange exchange) {
		String mimeType = getMimeType(exchange);
		if (MimeType.isJS(mimeType))
			return exchange.getParsedDocument();
		else if (MimeType.isML(mimeType)) {
			if (exchange.getParsedDocument() == null || exchange.getParsedDocument().length() == 0)
				return NULL_CHUNK;
			else
				return new JsonQuotingChunk(exchange.getParsedDocument(), true);
		} else if(MimeType.isTextPlain(mimeType)) {
            return new JsonQuotingChunk(exchange.getParsedDocument(), true);		    
		}
		return NULL_CHUNK;		
    }

	@Override
	public String getSyntaxIdentifier() {
		return "JS";
	}

	@Override
    public void check(LackrBackendExchange exchange) {
    }

}
