package com.fotonauts.lackr;

import org.json.JSONObject;

public class TestESI extends BaseTestSubstitution {

	public void testHtmlInHtml() throws Exception {
		String result = expand("before\n<!--# include virtual=\"/esi.html\" -->\nafter\n");
		assertEquals("before\n" + ESI_HTML + "\nafter\n", result);
	}

	public void testJsInHtmlShouldCrash() throws Exception {
		assertNull(expand("<!--# include virtual=\"/esi.json\" -->"));
	}

	public void testHtmlInJs() throws Exception {
		String result = expand("before\n\"ssi:include:virtual:/esi.html\"\nafter\n");
		assertEquals("before\n" + JSONObject.quote(ESI_HTML) + "\nafter\n", result);
	}

	public void testJsInJs() throws Exception {
		String result = expand("before\n\"ssi:include:virtual:/esi.json\"\nafter\n");
		assertEquals("before\n" + ESI_JSON + "\nafter\n", result);
	}

	public void testHtmlInMlJs() throws Exception {
		String result = expand("before\n<!--# include virtual=\\\"/esi.html\\\" -->\nafter\n");
		String json = JSONObject.quote(ESI_HTML);
		assertEquals("before\n" + json.substring(1, json.length() - 1) + "\nafter\n", result);
	}

	public void testJInMlJsShouldCrash() throws Exception {
		assertNull(expand("before\n<!--# include virtual=\\\"/esi.json\\\" -->\nafter\n"));
	}
	
	public void testEscapedHtmlInMlJs() throws Exception {
		String result = expand("before\n\\u003C!--# include virtual=\\\"/esi.html\\\" --\\u003E\nafter\n");
		String json = JSONObject.quote(ESI_HTML);
		assertEquals("before\n" + json.substring(1, json.length() - 1) + "\nafter\n", result);
	}

	public void testJInEscapedMlJsShouldCrash() throws Exception {
		assertNull(expand("before\n\\u003C!--# include virtual=\\\"/esi.json\\\" --\\u003E\nafter\n"));
	}


	public void testHttp() throws Exception {
		String result = expand("before\nhttp://esi.include.virtual/esi.html#\nafter\n");
		assertEquals("before\n" + ESI_HTML + "\nafter\n", result);
	}

	public void testEmptyJS() throws Exception {
		String result = expand("{ something_empty: \"ssi:include:virtual:/empty.html\" }");
		assertEquals("{ something_empty: null }", result);
	}

	public void testEncoding() throws Exception {
		String result = expand("before\nhttp://esi.include.virtual/\u00c9si.html#\nafter\n");
		assertEquals("before\n" + ESI_HTML + "\nafter\n", result);
	}

}
