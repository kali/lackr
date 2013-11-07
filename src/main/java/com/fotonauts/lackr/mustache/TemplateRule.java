package com.fotonauts.lackr.mustache;

import java.io.UnsupportedEncodingException;

import com.fotonauts.lackr.backend.LackrBackendRequest;
import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.ChunkUtils;
import com.fotonauts.lackr.interpolr.ConstantChunk;
import com.fotonauts.lackr.interpolr.Document;
import com.fotonauts.lackr.interpolr.MarkupDetectingRule;
import com.fotonauts.lackr.interpolr.ViewChunk;

public class TemplateRule extends MarkupDetectingRule {

	protected static ConstantChunk EMPTY_CHUNK = new ConstantChunk(
			"".getBytes());

	
	public TemplateRule() {
	    super("<!-- lackr:mustache:template name=\"*\" -->*<!-- /lackr:mustache:template -->");
    }

	@Override
    public Chunk substitute(Chunk buffer, int start, int[] boundPairs, int stop, Object context) {
		LackrBackendRequest exchange = (LackrBackendRequest) context;
		String name = null;
		Chunk template = null;
        try {
	        name = new String(ChunkUtils.extractBytes(buffer, boundPairs[0], boundPairs[1]), "UTF-8");
			template = exchange.getFrontendRequest().getService().getInterpolr()
	        .parse(new ViewChunk(buffer, boundPairs[2], boundPairs[3]), exchange);
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
