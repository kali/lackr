package com.fotonauts.lackr.mustache;

import java.io.UnsupportedEncodingException;

import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.InterpolrScope;
import com.fotonauts.lackr.interpolr.MarkupDetectingRule;

public class EvalRule extends MarkupDetectingRule {

	public EvalRule() {
		super("<!-- lackr:mustache:eval name=\"*\" -->*<!-- /lackr:mustache:eval -->");
	}

	@Override
	public Chunk substitute(byte[] buffer, int start, int[] boundPairs, int stop, InterpolrScope scope) {
		String name;
		try {
			name = new String(buffer, boundPairs[0], boundPairs[1] - boundPairs[0], "UTF-8");
			return new MustacheEvalChunk(name, buffer, boundPairs[2], boundPairs[3], scope);
		} catch (UnsupportedEncodingException e) {
			// fu*k off
			return null;
		}
	}
}
