package com.fotonauts.lackr.interpolr.esi.codec;

import java.io.IOException;
import java.io.OutputStream;

import com.fotonauts.lackr.interpolr.rope.Chunk;

public class JsonQuotingChunk implements Chunk {

    private Chunk inner;
    private int length = -1;
    private boolean addSurrondingQuotes;

    public JsonQuotingChunk(Chunk inner, boolean addSurrondingQuotes) {
        this.inner = inner;
        this.addSurrondingQuotes = addSurrondingQuotes;
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
        return "JSONIZER";
    }

    @Override
    public void writeTo(OutputStream stream) throws IOException {
        if (addSurrondingQuotes)
            stream.write('\"');
        inner.writeTo(new JsonQuoteFilterOutputStream(stream));
        if (addSurrondingQuotes)
            stream.write('\"');
    }

    @Override
    public void check() {
        inner.check();
    }

}
