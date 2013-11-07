package com.fotonauts.lackr.interpolr;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Interpolr {

	private List<Rule> rules;

	public Chunk parse(Chunk input, Object context) {
	    Chunk current = input;
		for (Rule rule : getRules())
			current = rule.parse(current, context);
		return current;
    }

	
    /*
	public Document parse(byte[] data, Object context) {
		return parse(data, 0, data.length, context);
	}

	public Chunk parse(Rule rule, Chunk input, Object context) {
        Chunk replacement = rule.parse(input, context);
        result.addAll(replacements);
        
		for (Chunk chunk : input.getChunks()) {
			if (chunk instanceof DataChunk) {
			} else
				result.add(chunk);
		}
		return result;
	}
	*/

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

}