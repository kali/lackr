package com.fotonauts.lackr.interpolr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import junit.framework.TestCase;


public class TestSimpleSubstitution extends TestCase {

	Interpolr inter;
	
	@Override
	protected void setUp() throws Exception {
	    super.setUp();
	    inter = new Interpolr();
	    inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
	}
	
	protected Document parse(String data) {
         try {
	        return inter.parse(data.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
        	// no way
        }
		return null;		
	}
	
	protected String expand(Document chunks) {
		try {
	        int length = chunks.length();
	        ByteArrayOutputStream baos = new ByteArrayOutputStream(length);
	        chunks.writeTo(baos);
	        byte[] bytes = baos.toByteArray();
	        assertEquals("result length computation is fine", length, bytes.length);
	        return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
        	// no way
        } catch (IOException e) {
        	// now way here
        }
		return null;
	}
	
	public void testNoop() throws Exception {
		Document r = parse("foobar");
	    assertEquals("(foobar)", r.toDebugString());
	    assertEquals("foobar", expand(r));
	}
	
	public void testFull() throws Exception {
		Document r = parse("titi");
	    assertEquals("<toto>", r.toDebugString());
	    assertEquals("toto", expand(r));		
	}
	
	public void testBegin() throws Exception {
		Document r = parse("tatatiti");
	    assertEquals("(tata)<toto>", r.toDebugString());
	    assertEquals("tatatoto", expand(r));				
	}

	public void testEnd() throws Exception {
		Document r = parse("tititata");
	    assertEquals("<toto>(tata)", r.toDebugString());
	    assertEquals("tototata", expand(r));				
	}
}
