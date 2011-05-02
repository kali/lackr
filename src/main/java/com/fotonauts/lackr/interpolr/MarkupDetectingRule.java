package com.fotonauts.lackr.interpolr;

import java.util.ArrayList;
import java.util.List;

public abstract class MarkupDetectingRule implements Rule {

	final private BoyerMooreScanner[] patterns;

	public MarkupDetectingRule(String markup) {
		String[] parts = markup.split("\\*");
		patterns = new BoyerMooreScanner[parts.length];
		for (int i = 0; i < parts.length; i++)
			patterns[i] = new BoyerMooreScanner(parts[i].getBytes());
	}

	public abstract Chunk substitute(byte[] buffer, int[] boundPairs, Object context);

	@Override
	public List<Chunk> parse(DataChunk chunk, Object context) {
		List<Chunk> result = new ArrayList<Chunk>();
		int boundPairs[] = new int[2 * (patterns.length - 1)];
		int current = chunk.getStart();
		while (current < chunk.getStop()) {
			int startFound = patterns[0].searchNext(chunk.getBuffer(), current, chunk.getStop());
			if (startFound == -1) {
				result.add(new DataChunk(chunk.getBuffer(), current, chunk.getStop()));
				current = chunk.getStop();
			} else {
				boolean broken = false;
				int lookahead = startFound + patterns[0].length();
				for (int i = 1; !broken && i < patterns.length; i++) {
					boundPairs[2 * (i - 1)] = lookahead;
					lookahead = patterns[i].searchNext(chunk.getBuffer(), lookahead, chunk.getStop());
					if (lookahead == -1) {
						// unclosed tag, bail out
						broken = true;
					} else {
						boundPairs[2 * i - 1] = lookahead;
						lookahead += patterns[i].length();
					}
				}
				if (broken) {
					// unclosed tag, bail out
					result.add(new DataChunk(chunk.getBuffer(), current, chunk.getStop()));
					current = chunk.getStop();
				} else {
					if (startFound > 0) {
						result.add(new DataChunk(chunk.getBuffer(), current, startFound));
					}
					result.add(substitute(chunk.getBuffer(), boundPairs, context));
					current = lookahead;
				}
			}
		}
		return result;
	}
}
