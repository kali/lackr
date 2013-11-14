package com.fotonauts.lackr.interpolr.esi.codec;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class AmpersandEscapeFilterOutputStream extends FilterOutputStream {

    public AmpersandEscapeFilterOutputStream(OutputStream out) {
        super(out);
    }

    private static byte[] AMP = "&amp;".getBytes();
    private static byte[] QUOT = "&quot;".getBytes();
    private static byte[] LT = "&lt;".getBytes();
    private static byte[] GT = "&gt;".getBytes();
    private static byte[] APOS = "&apos;".getBytes();

    @Override
    public void write(int b) throws IOException {
        if (b == '&')
            out.write(AMP);
        else if (b == '\'')
            out.write(APOS);
        else if (b == '"')
            out.write(QUOT);
        else if (b == '<')
            out.write(LT);
        else if (b == '>')
            out.write(GT);
        else
            out.write(b);
    }
}
