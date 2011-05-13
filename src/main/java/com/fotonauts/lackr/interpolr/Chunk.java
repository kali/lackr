package com.fotonauts.lackr.interpolr;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public interface Chunk {
	int length();

	void writeTo(OutputStream stream) throws IOException;

	String toDebugString();

	void check(List<Throwable> exceptions);
}

