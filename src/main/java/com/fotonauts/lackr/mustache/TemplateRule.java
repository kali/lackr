package com.fotonauts.lackr.mustache;

import java.io.UnsupportedEncodingException;

import com.fotonauts.lackr.BackendRequest;
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
    public Chunk substitute(byte[] buffer, int start, int[] boundPairs, int stop, Object context) {
		BackendRequest exchange = (BackendRequest) context;
		String name = null;
		Document template = null;
        try {
	        name = new String(buffer, boundPairs[0], boundPairs[1] - boundPairs[0], "UTF-8");
			template = exchange.getFrontendRequest().getService().getInterpolr()
	        .parse(buffer, boundPairs[2], boundPairs[3], exchange);
			exchange.getFrontendRequest().getMustacheContext().registerTemplate(name, template);
            // Someday, we will not want lackr to delete parsed templates on the fly
			// return new DataChunk(buffer, boundPairs[2], boundPairs[3]);
            return EMPTY_CHUNK;
        } catch (UnsupportedEncodingException e) {
        	// no way :)
			return EMPTY_CHUNK;
        }
    }

}
