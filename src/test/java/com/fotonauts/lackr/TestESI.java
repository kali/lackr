package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;

public class TestESI extends BaseTestSubstitution {

	public TestESI() throws Exception {
	    super();
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
	public void testPlainToML() throws Exception {
		String result = expand("before\nhttp://esi.include.virtual/some.text#\nafter\n");
		assertEquals("before\n" + ESI_TEXT.replace("&", "&amp;").replace("\"", "&quot;") + "\nafter\n", result);
	}
	
    @Test
    public void testPlainToJS() throws Exception {
        String result = expand("{ something_empty: \"ssi:include:virtual:/some.text\" }");
        assertEquals("{ something_empty: \"" + ESI_TEXT.replace("\"", "\\\"").replace("/", "\\/").replace("\n", "\\n") + "\" }", result);
    }

    @Ignore
    @Test
    public void testFemtorJSESI() throws Exception {
        String result = expand("{ something: \"ssi:include:femtor:/blah\" }");
        assertEquals("{ something: \"Femtor says hi!\" }", result);
    }

    @Test
	public void testUrlEncoding() throws Exception {
		String result = expand("before\nhttp://esi.include.virtual/\u00c9si.html#\nafter\n");
		assertEquals("before\n" + ESI_HTML + "\nafter\n", result);
	}
	
	@Test
	public void testIgnorable500() throws Exception {
		String result = expand("before\nhttp://esi.include.virtual/500.html#\nafter\n");
		assertEquals("before\n<!-- ignore me -->\nafter\n", result);	    
    }
}
