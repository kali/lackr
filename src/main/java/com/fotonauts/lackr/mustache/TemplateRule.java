package com.fotonauts.lackr.mustache;

import java.io.UnsupportedEncodingException;

import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.ConstantChunk;
import com.fotonauts.lackr.interpolr.MarkupDetectingRule;
import com.samskivert.mustache.MustacheParseException;

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
		String template = null;
        try {
	        name = new String(buffer, boundPairs[0], boundPairs[1] - boundPairs[0], "UTF-8");
			template = new String(buffer, boundPairs[2], boundPairs[3] - boundPairs[2], "UTF-8");
			exchange.getBackendRequest().getFrontendRequest().getMustacheContext().registerTemplate(name, template);
			return EMPTY_CHUNK;
        } catch (MustacheParseException e) {
        	StringBuilder builder = new StringBuilder();
        	builder.append("MustacheParseException\n");
        	builder.append("url: " + exchange.getBackendRequest().getQuery() + "\n");
        	builder.append(e.getMessage() + "\n");
        	builder.append("template name: " + name + "\n");
        	String lines[] = template.split("\n");
        	for(int i = 0; i < lines.length; i++)
        		builder.append(String.format("% 3d %s\n", i+1, lines[i]));
        	builder.append("\n");
        	throw new LackrPresentableError(builder.toString());
        } catch (UnsupportedEncodingException e) {
        	// now way :)
			return EMPTY_CHUNK;
        }
    }

}
