package com.fotonauts.lackr.interpolr;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import com.eaio.stringsearch.BoyerMooreHorspool;

public class SimpleSubstitutionRule implements Rule {

	private static BoyerMooreHorspool boyerMooreHorspool = new BoyerMooreHorspool();

	private byte[] needle;
	private Object processedNeedle;
	private Chunk replacement;

	public void setNeedle(String needle) {
		try {
			this.needle = needle.getBytes("UTF-8");
			processedNeedle = boyerMooreHorspool.processBytes(this.needle);
		} catch (UnsupportedEncodingException e) {
			// no way
		}
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

	public SimpleSubstitutionRule(String needle, String replacement) {
		setNeedle(needle);
		setReplacement(replacement);
	}

	protected int searchNext(byte[] buffer, int start, int stop) {
		return boyerMooreHorspool.searchBytes(buffer, start, stop, needle, processedNeedle);
	}

	@Override
	public List<Chunk> parse(DataChunk chunk) {
		List<Chunk> result = new ArrayList<Chunk>();
		int current = chunk.getStart();
		while (current < chunk.getStop()) {
			int found = searchNext(chunk.getBuffer(), current, chunk.getStop());
			if (found == -1) {
				result.add(new DataChunk(chunk.getBuffer(), current, chunk.getStop()));
				current = chunk.getStop();
			} else {
				if (found > 0) {
					result.add(new DataChunk(chunk.getBuffer(), current, found));
				}
				result.add(replacement);
				current = found + needle.length;
			}
		}
		return result;
	}
}
