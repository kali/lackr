package com.fotonauts.lackr.interpolr;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class SimpleSubstitutionRule implements Rule {

	private BMHPattern placeholder;
	private Chunk replacement;

	public void setPlaceholder(String placeholder) {
		this.placeholder = new BMHPattern(placeholder);
	}

	public void setReplacement(String replacement) {
		try {
			this.replacement = new ConstantChunk(replacement.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// no way
		}
	}

	public SimpleSubstitutionRule() {
	}

	public SimpleSubstitutionRule(String placeholder, String replacement) {
		setPlaceholder(placeholder);
		setReplacement(replacement);
	}

	@Override
	public List<Chunk> parse(DataChunk chunk, Object context) {
		List<Chunk> result = new ArrayList<Chunk>();
		int current = chunk.getStart();
		while (current < chunk.getStop()) {
			int found = placeholder.searchNext(chunk.getBuffer(), current, chunk.getStop());
			if (found == -1) {
				result.add(new DataChunk(chunk.getBuffer(), current, chunk.getStop()));
				current = chunk.getStop();
			} else {
				if (found > 0) {
					result.add(new DataChunk(chunk.getBuffer(), current, found));
				}
				result.add(replacement);
				current = found + placeholder.length();
			}
		}
		return result;
	}
}
