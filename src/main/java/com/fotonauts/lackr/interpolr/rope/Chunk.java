package com.fotonauts.lackr.interpolr.rope;

import java.io.IOException;
import java.io.OutputStream;

public interface Chunk {
    int length();

    void writeTo(OutputStream stream) throws IOException;

    String toDebugString();

    void check();
}
