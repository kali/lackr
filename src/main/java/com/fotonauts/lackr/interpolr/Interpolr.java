package com.fotonauts.lackr.interpolr;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.jetty.util.component.AbstractLifeCycle;

import com.fotonauts.lackr.MimeType;

public class Interpolr extends AbstractLifeCycle {

    private ObjectMapper jacksonObjectMapper;
    
	private List<Rule> rules;

	@Override
	protected void doStart() throws Exception {
	    jacksonObjectMapper = new ObjectMapper();
	}
	
	public void processResult(InterpolrScope scope) {
        String mimeType = scope.getResultMimeType();
        if (MimeType.isML(mimeType) || MimeType.isJS(mimeType))
            scope.setParsedDocument(parse(scope.getBodyBytes(), scope));
        else
            scope.setParsedDocument(new Document(new DataChunk(scope.getBodyBytes())));
	}

	public Document parse(byte[] buffer, int start, int stop, InterpolrScope scope) {
		Document chunks = new Document();
		chunks.add(new DataChunk(buffer, start, stop));
		for (Rule rule : getRules()) {
			chunks = parse(rule, chunks, scope);
		}
		return chunks;
    }

	
	public Document parse(byte[] data, InterpolrScope scope) {
		return parse(data, 0, data.length, scope);
	}

	public Document parse(Rule rule, Document input, InterpolrScope scope) {
		Document result = new Document();
		for (Chunk chunk : input.getChunks()) {
			if (chunk instanceof DataChunk) {
				List<Chunk> replacements = rule.parse((DataChunk) chunk, scope);
				result.addAll(replacements);
			} else
				result.add(chunk);
		}
		return result;
	}

	public void setRules(List<Rule> rules) {
		this.rules = rules;
	}

	public List<Rule> getRules() {
		if (rules == null)
			rules = new LinkedList<Rule>();
		return rules;
	}

	public void addRule(Rule rule) {
		getRules().add(rule);
	}

    public void setRules(Rule[] rules2) {
        rules = Arrays.asList(rules2);
    }

    public ObjectMapper getJacksonObjectMapper() {
        return jacksonObjectMapper;
    }

}