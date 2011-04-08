package com.fotonauts.lackr.interpolr;

import java.io.UnsupportedEncodingException;

import com.eaio.stringsearch.BoyerMooreHorspool;

public class BMHPattern {

	private static BoyerMooreHorspool boyerMooreHorspool = new BoyerMooreHorspool();


	private final byte[] needle;
	private final Object processedNeedle;

	public BMHPattern(byte[] needle) {
		this.needle = needle;
		processedNeedle = boyerMooreHorspool.processBytes(this.needle);
    }
	
	public BMHPattern(String needle) {
		byte[] bytes = null;
		try {
			bytes = needle.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
        	// yeah, yeah, yeah
        }
        this.needle = bytes;
		this.processedNeedle = boyerMooreHorspool.processBytes(this.needle);
    }

	protected final int searchNext(byte[] buffer, int start, int stop) {
		//System.err.format("search (%s) in %s\n", new String(needle), new String(buffer, start, stop-start));
		return boyerMooreHorspool.searchBytes(buffer, start, stop, needle, processedNeedle);
	}

	public final int length() {
	    return needle.length;
    }
}
