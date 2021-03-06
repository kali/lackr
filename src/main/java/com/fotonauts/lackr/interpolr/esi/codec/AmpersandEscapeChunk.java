package com.fotonauts.lackr.interpolr.esi.codec;

import java.io.IOException;
import java.io.OutputStream;

import com.fotonauts.lackr.interpolr.rope.Chunk;

public class AmpersandEscapeChunk implements Chunk {

    private Chunk inner;
    private int length = -1;

    public AmpersandEscapeChunk(Chunk inner) {
        this.inner = inner;
    }

    private static class SizingOutputStream extends OutputStream {

        public int length = 0;

        @Override
        public void write(int b) throws IOException {
            length++;
        }
    }

    @Override
    public int length() {
        if (length == -1) {
            SizingOutputStream sos = new SizingOutputStream();
            try {
                writeTo(sos);
            } catch (IOException e) {
                // nope
            }
            length = sos.length;
        }
        return length;
    }

    @Override
    public String toDebugString() {
        return "AMPERSANDESCAPE";
    }

    @Override
    public void writeTo(OutputStream stream) throws IOException {
        inner.writeTo(new AmpersandEscapeFilterOutputStream(stream));
    }

    @Override
    public void check() {
        inner.check();
    }

}
