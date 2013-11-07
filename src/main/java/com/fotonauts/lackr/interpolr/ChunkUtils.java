package com.fotonauts.lackr.interpolr;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;

public class ChunkUtils {
    
    public static byte[] extractBytes(Chunk chunk, final int start, final int stop) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            chunk.writeTo(new FilterOutputStream(baos) {
                int position;
                @Override
                public void write(int b) throws IOException {
                    if(position >= start && position < stop)
                        out.write(b);
                    position++;
                }
            });
        } catch (IOException e) {
            // unlikely
        }
        return baos.toByteArray();
    }
}
