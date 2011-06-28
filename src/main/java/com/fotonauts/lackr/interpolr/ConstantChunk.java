package com.fotonauts.lackr.interpolr;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class ConstantChunk implements Chunk {
    private byte[] constant;

    public ConstantChunk(byte[] constant) {
        super();
        this.constant = constant;
    }

    @Override
    public int length() {
        return constant.length;
    }

    @Override
    public void writeTo(OutputStream stream) throws IOException {
        stream.write(constant);
    }

    @Override
    public String toDebugString() {
        try {
            return "<" + new String(constant, "UTF-8") + ">";
        } catch (UnsupportedEncodingException e) {
            // nope.
        }
        return null;
    }

    @Override
    public void check() {
    }
}
