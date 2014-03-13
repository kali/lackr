package com.fotonauts.lackr.interpolr.json;

import java.io.IOException;
import java.io.OutputStream;

import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.Document;
import com.fotonauts.lackr.interpolr.InterpolrScope;

public class ArchiveChunk implements Chunk {

    private String id;
    protected Document inner;
    protected InterpolrScope scope;

    public ArchiveChunk(String id, byte[] buffer, int start, int stop, InterpolrScope scope) {
        this.scope = scope;
        inner = scope.getInterpolr().parse(buffer, start, stop, scope);
        this.id = id;
    }

    @Override
    public int length() {
        return inner.length();
    }

    @Override
    public void writeTo(OutputStream stream) throws IOException {
        inner.writeTo(stream);
    }

    @Override
    public String toDebugString() {
        return "[ARCHIVE: " + id + " ]";
    }

    @Override
    public void check() {
        // check is handled by context
    }

}
