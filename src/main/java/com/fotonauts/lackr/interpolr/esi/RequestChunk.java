package com.fotonauts.lackr.interpolr.esi;

import java.io.IOException;
import java.io.OutputStream;

import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.InterpolrScope;

public class RequestChunk implements Chunk {

	private boolean isChecked = false;
	
	private InterpolrScope sub;
	
	private ESIIncludeRule rule;
	
	public RequestChunk(InterpolrScope sub, ESIIncludeRule rule) {
		this.sub = sub;
		this.rule = rule;
    }
	
	@Override
    public int length() {
		return rule.filterDocumentAsChunk(sub).length();
    }

	@Override
    public String toDebugString() {
	    return "{{{" + rule.getClass().getSimpleName() + ":" + sub.toString() + "}}}";
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
		    sub.getInterpolrContext().addBackendExceptions(new LackrPresentableError("expected a parsed document here, found nothing", sub));
		else {
		    sub.getParsedDocument().check();
		    rule.check(sub);
		}
		isChecked = true;
	}

}
