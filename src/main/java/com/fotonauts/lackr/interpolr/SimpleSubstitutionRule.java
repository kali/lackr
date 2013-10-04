package com.fotonauts.lackr.interpolr;

import java.io.UnsupportedEncodingException;
import java.util.List;

public class SimpleSubstitutionRule extends SimpleTriggerRule implements Rule {

	private Chunk replacement;

	public void setReplacement(String replacement) {
		try {
			this.replacement = new ConstantChunk(replacement.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// no way
		}
	}
	
	public void setPlaceholder(String placeholder) {
	    setTrigger(placeholder);
	}

	public SimpleSubstitutionRule() {
	}

	public SimpleSubstitutionRule(String placeholder, String replacement) {
		setTrigger(placeholder);
		setReplacement(replacement);
	}
	
    @Override
    protected int onFound(List<Chunk> result, DataChunk chunk, int index, Object context) {
        result.add(replacement);
        return trigger.length();
    }
}
