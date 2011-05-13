package com.fotonauts.lackr.mustache;

import java.io.UnsupportedEncodingException;

import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.ConstantChunk;
import com.fotonauts.lackr.interpolr.Document;
import com.fotonauts.lackr.interpolr.MarkupDetectingRule;

public class TemplateRule extends MarkupDetectingRule {

	protected static ConstantChunk EMPTY_CHUNK = new ConstantChunk(
			"".getBytes());

	
	public TemplateRule() {
	    super("<!-- lackr:mustache:template name=\"*\" -->*<!-- /lackr:mustache:template -->");
    }

	@Override
    public Chunk substitute(byte[] buffer, int[] boundPairs, Object context) {
		LackrBackendExchange exchange = (LackrBackendExchange) context;
		String name = null;
		Document template = null;
        try {
	        name = new String(buffer, boundPairs[0], boundPairs[1] - boundPairs[0], "UTF-8");
			template = exchange.getBackendRequest().getFrontendRequest().getService().getInterpolr()
	        .parse(buffer, boundPairs[2], boundPairs[3], exchange);
			exchange.getBackendRequest().getFrontendRequest().getMustacheContext().registerTemplate(name, template);
			return EMPTY_CHUNK;
        } catch (UnsupportedEncodingException e) {
        	// now way :)
			return EMPTY_CHUNK;
        }
    }

}
