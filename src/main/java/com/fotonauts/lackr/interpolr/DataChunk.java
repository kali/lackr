package com.fotonauts.lackr.interpolr;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class DataChunk implements Chunk {

	public DataChunk(byte[] buffer, int start, int stop) {
		super();
		this.buffer = buffer;
		this.start = start;
		this.stop = stop;
	}

	public DataChunk(byte[] data) {
		this.buffer = data;
		this.start = 0;
		this.stop = data.length;
    }

	private byte[] buffer;
	private int start;
	private int stop;

	public int length() {
		return stop - start;
	}

	public int getStart() {
		return start;
	}

	public int getStop() {
		return stop;
	}

	public byte[] getBuffer() {
		return buffer;
    }

	@Override
	public String toDebugString() {
		try {
	        return "(" + new String(buffer, start, stop-start, "UTF-8") + ")";
        } catch (UnsupportedEncodingException e) {
        	// nope.
        }
        return null;
	}

	
	@Override
    public void writeTo(OutputStream stream) throws IOException {
		stream.write(buffer, start, stop-start);
    }

	@Override
    public void check(List<Throwable> exceptions) {
    }
}
