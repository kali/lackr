package com.fotonauts.lackr.esi;

import java.io.IOException;
import java.io.OutputStream;

import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.backend.LackrBackendRequest;
import com.fotonauts.lackr.interpolr.Chunk;

public class RequestChunk implements Chunk {

	private boolean isChecked = false;
	
	private LackrBackendRequest sub;
	
	private ESIIncludeRule rule;
	
	public RequestChunk(LackrBackendRequest sub, ESIIncludeRule rule) {
		this.sub = sub;
		this.rule = rule;
    }
	
	@Override
    public int length() {
		return rule.filterDocumentAsChunk(sub).length();
    }

	@Override
    public String toDebugString() {
	    return "{{{" + rule.getClass().getSimpleName() + ":" + sub.getQuery() + "}}}";
    }

	@Override
    public void writeTo(OutputStream stream) throws IOException {
		rule.filterDocumentAsChunk(sub).writeTo(stream);
    }
	
	@Override
	public void check() {
		if(isChecked)
			return;
		if(sub.getParsedDocument() == null)
		    sub.getFrontendRequest().addBackendExceptions(new LackrPresentableError("expected a parsed document here, found nothing", sub.getExchange()));
		else {
		    sub.getParsedDocument().check();
		    rule.check(sub);
		}
		isChecked = true;
	}

    @Override
    public byte at(int cursor) {
        return rule.filterDocumentAsChunk(sub).at(cursor);
    }

}
