package com.fotonauts.lackr.mustache;

import java.io.UnsupportedEncodingException;

import com.fotonauts.lackr.backend.LackrBackendRequest;
import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.ChunkUtils;
import com.fotonauts.lackr.interpolr.MarkupDetectingRule;
import com.fotonauts.lackr.interpolr.ViewChunk;

public class EvalRule extends MarkupDetectingRule {

	public EvalRule() {
		super("<!-- lackr:mustache:eval name=\"*\" -->*<!-- /lackr:mustache:eval -->");
	}

	@Override
	public Chunk substitute(Chunk buffer, int start, int[] boundPairs, int stop, Object context) {
		LackrBackendRequest request = (LackrBackendRequest) context;
		String name;
		try {
			name = new String(ChunkUtils.extractBytes(buffer, boundPairs[0], boundPairs[1]), "UTF-8");
			return new MustacheEvalChunk(name, new ViewChunk(buffer, boundPairs[2], boundPairs[3]), request);
		} catch (UnsupportedEncodingException e) {
			// fu*k off
			return null;
		}
	}
}
