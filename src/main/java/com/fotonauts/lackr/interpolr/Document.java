package com.fotonauts.lackr.interpolr;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

public class Document implements Chunk {

    List<Chunk> chunks;

    public Document() {

    }

    public Document(DataChunk dataChunk) {
        getChunks().add(dataChunk);
    }

    public Document(Chunk[] chunks) {
        for (Chunk chunk : chunks)
            getChunks().add(chunk);
    }

    public String toDebugString() {
        if (chunks == null)
            return "{EMPTY DOC}";
        StringBuilder builder = new StringBuilder();
        for (Chunk chunk : chunks) {
            builder.append(chunk.toDebugString());
        }
        return builder.toString();
    }

    public int length() {
        if (chunks == null)
            return 0;
        int l = 0;
        for (Chunk chunk : chunks)
            l += chunk.length();
        return l;
    }

    public void writeTo(OutputStream stream) throws IOException {
        if (chunks == null)
            return;
        for (Chunk chunk : chunks) {
            chunk.writeTo(stream);
        }
    }

    public void check() {
        if (chunks == null)
            return;
        for (Chunk chunk : chunks) {
            chunk.check();
        }
    }

    public void addAll(List<Chunk> added) {
        getChunks().addAll(added);
    }

    public void add(Chunk added) {
        getChunks().add(added);
    }

    public List<Chunk> getChunks() {
        if (chunks == null)
            chunks = new LinkedList<Chunk>();
        return chunks;
    }
}
