package com.fotonauts.lackr.interpolr;

import java.util.ArrayList;
import java.util.List;

public abstract class MarkupDetectingRule implements Rule {

	final private BoyerMooreScanner startPlaceholder;
	final private BoyerMooreScanner stopPlaceholder;

	public MarkupDetectingRule(String markup) {
		String[] parts = markup.split("\\*");
		this.startPlaceholder = new BoyerMooreScanner(parts[0].getBytes());
		this.stopPlaceholder = new BoyerMooreScanner(parts[1].getBytes());
	}

	public abstract Chunk substitute(byte[] buffer, int start, int stop, Object context);
	
	@Override
	public List<Chunk> parse(DataChunk chunk, Object context) {
		List<Chunk> result = new ArrayList<Chunk>();
		int current = chunk.getStart();
		while (current < chunk.getStop()) {
			int startFound = startPlaceholder.searchNext(chunk.getBuffer(), current, chunk.getStop());
			if (startFound == -1) {
				result.add(new DataChunk(chunk.getBuffer(), current, chunk.getStop()));
				current = chunk.getStop();
			} else {
				int stopFound = stopPlaceholder.searchNext(chunk.getBuffer(), startFound + startPlaceholder.length(), chunk.getStop());
				if(stopFound == -1) {
					// unclosed tag, bail out
					result.add(new DataChunk(chunk.getBuffer(), current, chunk.getStop()));
					current = chunk.getStop();
				} else {
					if (startFound > 0) {
						result.add(new DataChunk(chunk.getBuffer(), current, startFound));
					}
					result.add(substitute(chunk.getBuffer(), startFound + startPlaceholder.length(), stopFound, context));
					current = stopFound + stopPlaceholder.length();				
				}
			}
		}
		return result;
	}
}
