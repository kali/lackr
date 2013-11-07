package com.fotonauts.lackr.interpolr;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

public class Document implements Chunk {

    Chunk[] chunks;
    private int cumulativeLengths[];
    private int length;

    /*
    public Document(DataChunk dataChunk) {
    	getChunks().add(dataChunk);
    }
    */
    public Document(Chunk[] chunks) {
        this.chunks = new Chunk[chunks.length];
        postLoad();
    }

    public Document(List<Chunk> result) {
        this.chunks = result.toArray(new Chunk[result.size()]);
        postLoad();
    }

    private void postLoad() {
        length = 0;
        int j = 0;
        for (int i = 0; i < chunks.length; i++) {
            int l = chunks[i].length();
            if (l > 0) {
                cumulativeLengths[j++] = length;
                length += chunks[i].length();
            }
        }
        if (j < chunks.length) {
            chunks = Arrays.copyOf(chunks, j);
        }
    }

    public String toDebugString() {
        StringBuilder builder = new StringBuilder();
        for (Chunk chunk : chunks) {
            builder.append(chunk.toDebugString());
        }
        return builder.toString();
    }

    public int length() {
        return length;
    }

    public void writeTo(OutputStream stream) throws IOException {
        for (Chunk chunk : chunks) {
            chunk.writeTo(stream);
        }
    }

    public void check() {
        for (Chunk chunk : chunks) {
            chunk.check();
        }
    }

    /*
    	public void addAll(List<Chunk> added) {
    		getChunks().addAll(added);
        }

    	public void add(Chunk added) {
    		getChunks().add(added);
        }

    	public List<Chunk> getChunks() {
    		if(chunks == null)
    			chunks = new LinkedList<Chunk>();
    		return chunks;
        }
        */

    @Override
    public byte at(int cursor) {
        if (cursor > length)
            throw new ArrayIndexOutOfBoundsException();
        int chunkIndex = Arrays.binarySearch(cumulativeLengths, cursor);
        // found, we are at the first position of one chunk
        if (chunkIndex >= 0)
            return chunks[chunkIndex].at(0);
        // not found, binarySearch returns -(insertion point+1). insertion point is the first position *after* us  
        int insertionPoint = -chunkIndex - 1;
        if (insertionPoint == 0) // first chunk
            return chunks[0].at(cursor);
        else
            return chunks[insertionPoint].at(cursor - cumulativeLengths[insertionPoint - 1]);
    }
}
