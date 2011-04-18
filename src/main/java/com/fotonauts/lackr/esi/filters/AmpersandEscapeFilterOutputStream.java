package com.fotonauts.lackr.esi.filters;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class AmpersandEscapeFilterOutputStream extends FilterOutputStream {

	public AmpersandEscapeFilterOutputStream(OutputStream out) {
		super(out);
	}

	private static byte[] AMPERSAND = "&amp;".getBytes();

	@Override
	public void write(int b) throws IOException {
		if (b == '&') {
			out.write(AMPERSAND);
		} else
			out.write(b);
	}
}
