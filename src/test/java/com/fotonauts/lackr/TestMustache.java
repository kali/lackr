package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestMustache extends BaseTestSubstitution {

	public TestMustache(String clientImplementation) throws Exception {
		super(clientImplementation);
	}

	@Test
	public void testMustache() throws Exception {
		String result = expand(
				"<!-- lackr:mustache:template name=\"template_name\" -->\n"
				+ "some text from the template name:{{name}} value:{{value}}\n"
				+ "<!-- /lackr:mustache:template -->\n"
				+ "<!-- lackr:mustache:eval name=\"template_name\" -->\n"
				+ "{ \"name\": \"the name\", \"value\": \"the value\" }\n"
				+ "<!-- /lackr:mustache:eval -->\n");
		assertEquals("\n\nsome text from the template name:the name value:the value\n\n", result);
	}

	@Test
	public void testMustacheWithEsi() throws Exception {
		String result = expand(
				"<!-- lackr:mustache:template name=\"template_name\" -->\n"
				+ "some text from the template name:{{name}} value:{{value}} some:{{esi.some}}\n"
				+ "<!-- /lackr:mustache:template -->\n"
				+ "<!-- lackr:mustache:eval name=\"template_name\" -->\n"
				+ "{ \"name\": \"the name\", \"value\": \"the value\", \"esi\":\"ssi:include:virtual:/esi.json\" }\n"
				+ "<!-- /lackr:mustache:eval -->\n");
		assertEquals("\n\nsome text from the template name:the name value:the value some:json crap\n\n", result);
	}

}
