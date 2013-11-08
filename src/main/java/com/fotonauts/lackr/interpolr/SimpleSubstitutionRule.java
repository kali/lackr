package com.fotonauts.lackr.interpolr;

import java.io.UnsupportedEncodingException;

public class SimpleSubstitutionRule extends PrefixDetectingRule {

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
	    super(null);
	}

	public SimpleSubstitutionRule(String placeholder, String replacement) {
	    super(placeholder);
		setReplacement(replacement);
	}

    @Override
    public int lookaheadForEnd(byte[] buffer, int start, int stop) {
        return start + trigger.length();
    }

    @Override
    public Chunk substitute(byte[] buffer, int start, int stop, InterpolrScope scope) {
        return replacement;
    }
	
}
