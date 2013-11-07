package com.fotonauts.lackr.interpolr;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class ViewChunk implements Chunk {

    public ViewChunk(Chunk inner, int start, int stop) {
        this.inner = inner;
        this.start = start;
        this.stop = stop;
    }

    private Chunk inner;
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

    @Override
    public String toDebugString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            writeTo(baos);
            return "(" + new String(baos.toByteArray(), "UTF-8") + ")";
        } catch (UnsupportedEncodingException e) {
            // nope.
        } catch (IOException e) {
            // unlikely
        }
        return null;
    }

    @Override
    public void writeTo(OutputStream stream) throws IOException {
        inner.writeTo(new FilterOutputStream(stream) {
            int position;
            @Override
            public void write(int b) throws IOException {
                if(position >= start && position < stop)
                    out.write(b);
                position++;
            }
        });
    }

    @Override
    public void check() {
    }

    @Override
    public byte at(int cursor) {
        if(cursor + start >= stop)
            throw new ArrayIndexOutOfBoundsException();
        return inner.at(cursor + start);
    }
}