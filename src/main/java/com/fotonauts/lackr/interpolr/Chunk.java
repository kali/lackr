package com.fotonauts.lackr.interpolr;

import java.io.IOException;
import java.io.OutputStream;

public interface Chunk {
	int length();

	void writeTo(OutputStream stream) throws IOException;

	Object toDebugString();
}

