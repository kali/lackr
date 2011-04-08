package com.fotonauts.lackr.esi;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class JsonQuoteFilterOutputStream extends FilterOutputStream {
	
	public JsonQuoteFilterOutputStream(OutputStream out) {
	    super(out);
    }
	
	@Override
	public void write(int b) throws IOException {
		if(b == '"' || b == '\\' || b == '/') {
			out.write('\\');
			out.write(b);
		} else if(b == '\b') {
			out.write('\\');
			out.write('b');
		} else if(b == '\f') {
			out.write('\\');
			out.write('f');			
		} else if(b == '\n') {
			out.write('\\');
			out.write('n');			
		} else if(b == '\r') {
			out.write('\\');
			out.write('r');			
		} else if(b == '\t') {
			out.write('\\');
			out.write('t');			
		} else
			out.write(b);
	}
}
