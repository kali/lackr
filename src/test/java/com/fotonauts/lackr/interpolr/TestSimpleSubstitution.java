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
	    inter = new Interpolr();
		Document r = parse("foobar");
	    assertEquals("(foobar)", r.toDebugString());
	    assertEquals("foobar", expand(r));		
	}
	
	public void testNoMatch() throws Exception {
	    inter = new Interpolr();
	    inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
		Document r = parse("foobar");
	    assertEquals("(foobar)", r.toDebugString());
	    assertEquals("foobar", expand(r));
	}
	
	public void testFull() throws Exception {
	    inter = new Interpolr();
	    inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
		Document r = parse("titi");
	    assertEquals("<toto>", r.toDebugString());
	    assertEquals("toto", expand(r));		
	}
	
	public void testBegin() throws Exception {
	    inter = new Interpolr();
	    inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
		Document r = parse("tatatiti");
	    assertEquals("(tata)<toto>", r.toDebugString());
	    assertEquals("tatatoto", expand(r));				
	}

	public void testEnd() throws Exception {
	    inter = new Interpolr();
	    inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
		Document r = parse("tititata");
	    assertEquals("<toto>(tata)", r.toDebugString());
	    assertEquals("tototata", expand(r));				
	}

	public void testMiddle() throws Exception {
	    inter = new Interpolr();
	    inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
		Document r = parse("tatatititata");
	    assertEquals("(tata)<toto>(tata)", r.toDebugString());
	    assertEquals("tatatototata", expand(r));				
	}

	public void testSeveral() throws Exception {
	    inter = new Interpolr();
	    inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
		Document r = parse("tatatititututititete");
	    assertEquals("(tata)<toto>(tutu)<toto>(tete)", r.toDebugString());
	    assertEquals("tatatototututototete", expand(r));				
	}

	public void testMultiple() throws Exception {
	    inter = new Interpolr();
	    inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
	    inter.addRule(new SimpleSubstitutionRule("lili", "lolo"));
		Document r = parse("tatalilitatatititata");
	    assertEquals("(tata)<lolo>(tata)<toto>(tata)", r.toDebugString());
	    assertEquals("tatalolotatatototata", expand(r));				
	}
}
