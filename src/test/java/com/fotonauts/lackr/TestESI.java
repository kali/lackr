package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.json.JSONObject;
import org.junit.Test;

public class TestESI extends BaseTestSubstitution {

	public TestESI(String clientImplementation) throws Exception {
	    super(clientImplementation);
    }

	@Test
	public void testHtmlInHtml() throws Exception {
		String result = expand("before\n<!--# include virtual=\"/esi.html\" -->\nafter\n");
		assertEquals("before\n" + ESI_HTML + "\nafter\n", result);
	}

	@Test
	public void testJsInHtmlShouldCrash() throws Exception {
		assertNull(expand("<!--# include virtual=\"/esi.json\" -->"));
	}

	@Test
	public void testHtmlInJs() throws Exception {
		String result = expand("before\n\"ssi:include:virtual:/esi.html\"\nafter\n");
		assertEquals("before\n" + JSONObject.quote(ESI_HTML) + "\nafter\n", result);
	}

	@Test
	public void testJsInJs() throws Exception {
		String result = expand("before\n\"ssi:include:virtual:/esi.json\"\nafter\n");
		assertEquals("before\n" + ESI_JSON + "\nafter\n", result);
	}

	@Test
	public void testHtmlInMlJs() throws Exception {
		String result = expand("before\n<!--# include virtual=\\\"/esi.html\\\" -->\nafter\n");
		String json = JSONObject.quote(ESI_HTML);
		assertEquals("before\n" + json.substring(1, json.length() - 1) + "\nafter\n", result);
	}

	@Test
	public void testJInMlJsShouldCrash() throws Exception {
		assertNull(expand("before\n<!--# include virtual=\\\"/esi.json\\\" -->\nafter\n"));
	}
	
	@Test
	public void testEscapedHtmlInMlJs() throws Exception {
		String result = expand("before\n\\u003C!--# include virtual=\\\"/esi.html\\\" --\\u003E\nafter\n");
		String json = JSONObject.quote(ESI_HTML);
		assertEquals("before\n" + json.substring(1, json.length() - 1) + "\nafter\n", result);
	}

	@Test
	public void testJInEscapedMlJsShouldCrash() throws Exception {
		assertNull(expand("before\n\\u003C!--# include virtual=\\\"/esi.json\\\" --\\u003E\nafter\n"));
	}

	@Test
	public void testHttp() throws Exception {
		String result = expand("before\nhttp://esi.include.virtual/esi.html#\nafter\n");
		assertEquals("before\n" + ESI_HTML + "\nafter\n", result);
	}

	@Test
	public void testEmptyJS() throws Exception {
		String result = expand("{ something_empty: \"ssi:include:virtual:/empty.html\" }");
		assertEquals("{ something_empty: null }", result);
	}

	@Test
	public void testEncoding() throws Exception {
		String result = expand("before\nhttp://esi.include.virtual/\u00c9si.html#\nafter\n");
		assertEquals("before\n" + ESI_HTML + "\nafter\n", result);
	}
}
