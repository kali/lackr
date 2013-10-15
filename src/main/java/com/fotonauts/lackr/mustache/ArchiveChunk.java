package com.fotonauts.lackr.mustache;

import java.io.IOException;
import java.io.OutputStream;

import com.fotonauts.lackr.backend.LackrBackendRequest;

public class ArchiveChunk extends ParsedJsonChunk {

    private String id;

    public ArchiveChunk(String id, byte[] buffer, int start, int stop, LackrBackendRequest request) {
        super(buffer, start, stop, request);
        this.request = request;
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
        // check is handled by mustache context
    }
    
}
