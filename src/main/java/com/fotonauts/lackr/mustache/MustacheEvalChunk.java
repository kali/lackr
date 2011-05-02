package com.fotonauts.lackr.mustache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.fotonauts.lackr.LackrBackendExchange;
import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.ConstantChunk;
import com.fotonauts.lackr.interpolr.Document;
import com.fotonauts.lackr.interpolr.Rule.InterpolrException;
import com.samskivert.mustache.MustacheException;

public class MustacheEvalChunk implements Chunk {

	private static Chunk EMPTY = new ConstantChunk(new byte[0]);

	private Document inner;
	private LackrBackendExchange exchange;
	private Chunk result = EMPTY;
	private String name;

	public MustacheEvalChunk(String name, byte[] buffer, int start, int stop, LackrBackendExchange exchange) {
		this.exchange = exchange;
		this.name = name;
		inner = exchange.getBackendRequest().getFrontendRequest().getService().getInterpolr().parse(buffer, start,
		        stop, exchange);
	}

	@Override
	public void check(List<InterpolrException> exceptions) {
		inner.check(exceptions);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ObjectMapper mapper = exchange.getBackendRequest().getFrontendRequest().getService()
			        .getJacksonObjectMapper();
			inner.writeTo(baos);
			@SuppressWarnings("unchecked")
			Map data = mapper.readValue(baos.toByteArray(), Map.class);
			result = new ConstantChunk(exchange.getBackendRequest().getFrontendRequest().getMustacheContext().eval(
			        name, data).getBytes("UTF-8"));
		} catch (MustacheException e) {
			exchange.getBackendRequest().getFrontendRequest().addBackendExceptions(e);
		} catch (JsonParseException e) {
			exchange.getBackendRequest().getFrontendRequest().addBackendExceptions(
			        new RuntimeException("JSON parse error: mustache template\n" + "template:" + name +"\ndata:" + baos.toString()));
		} catch (UnsupportedEncodingException e) {
			// unlikely
			exchange.getBackendRequest().getFrontendRequest().addBackendExceptions(e);
		} catch (JsonMappingException e) {
			// unlikely
			exchange.getBackendRequest().getFrontendRequest().addBackendExceptions(e);
		} catch (IOException e) {
			// unlikely
			exchange.getBackendRequest().getFrontendRequest().addBackendExceptions(e);
		}

	}

	@Override
	public int length() {
		return result.length();
	}

	@Override
	public String toDebugString() {
		return "{{//" + name + "//}}";
	}

	@Override
	public void writeTo(OutputStream stream) throws IOException {
		result.writeTo(stream);
	}
}
