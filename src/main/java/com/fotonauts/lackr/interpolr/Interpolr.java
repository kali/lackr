package com.fotonauts.lackr.interpolr;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.jetty.util.component.AbstractLifeCycle;

public class Interpolr extends AbstractLifeCycle {

    private ObjectMapper jacksonObjectMapper;
    
	private List<Rule> rules;

	@Override
	protected void doStart() throws Exception {
	    jacksonObjectMapper = new ObjectMapper();
	}
	
	public Document parse(byte[] buffer, int start, int stop, Object context) {
		Document chunks = new Document();
		chunks.add(new DataChunk(buffer, start, stop));
		for (Rule rule : getRules()) {
			chunks = parse(rule, chunks, context);
		}
		return chunks;
    }

	
	public Document parse(byte[] data, Object context) {
		return parse(data, 0, data.length, context);
	}

	public Document parse(Rule rule, Document input, Object context) {
		Document result = new Document();
		for (Chunk chunk : input.getChunks()) {
			if (chunk instanceof DataChunk) {
				List<Chunk> replacements = rule.parse((DataChunk) chunk, context);
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