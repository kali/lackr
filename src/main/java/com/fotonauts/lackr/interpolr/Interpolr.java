package com.fotonauts.lackr.interpolr;

import java.util.LinkedList;
import java.util.List;

public class Interpolr {

	private List<Rule> rules;
	
	public Document parse(byte[] data, Object context) {
		Document chunks = new Document();
		chunks.add(new DataChunk(data));
		for (Rule rule : getRules()) {
			chunks = parse(rule, chunks, context);
		}
		return chunks;
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

}