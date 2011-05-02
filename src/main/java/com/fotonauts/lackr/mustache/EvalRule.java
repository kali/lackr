package com.fotonauts.lackr.mustache;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.ConstantChunk;
import com.fotonauts.lackr.interpolr.MarkupDetectingRule;

public class EvalRule extends MarkupDetectingRule {

	protected static ConstantChunk EMPTY_CHUNK = new ConstantChunk("".getBytes());

	public EvalRule() {
		super("<!-- lackr:mustache:eval name=\"*\" -->*<!-- /lackr:mustache:eval -->");
	}

    @Override
	public Chunk substitute(byte[] buffer, int[] boundPairs, Object context) {
		LackrBackendExchange exchange = (LackrBackendExchange) context;
		try {
			String name = new String(buffer, boundPairs[0], boundPairs[1] - boundPairs[0], "UTF-8");
			ByteArrayInputStream bais = new ByteArrayInputStream(buffer, boundPairs[2], boundPairs[3] - boundPairs[2]);
			ObjectMapper mapper = exchange.getBackendRequest().getFrontendRequest().getService()
	        .getJacksonObjectMapper();
			@SuppressWarnings("unchecked")
			Map data = mapper.readValue(bais, Map.class);
			String result = exchange.getBackendRequest().getFrontendRequest().getMustacheContext().eval(name, data);
			return new ConstantChunk(result.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// no way :)
			return EMPTY_CHUNK;
		} catch (JsonParseException e) {
			exchange.getBackendRequest().getFrontendRequest().addBackendExceptions(e);
			return EMPTY_CHUNK;
        } catch (JsonMappingException e) {
        	// unlikely
			exchange.getBackendRequest().getFrontendRequest().addBackendExceptions(e);
			return EMPTY_CHUNK;
        } catch (IOException e) {
        	// unlikely
			exchange.getBackendRequest().getFrontendRequest().addBackendExceptions(e);
			return EMPTY_CHUNK;
        }
	}
}
