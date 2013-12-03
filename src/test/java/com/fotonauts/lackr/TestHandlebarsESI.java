package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fotonauts.lackr.interpolr.Interpolr;
import com.fotonauts.lackr.testutils.Factory;
import com.fotonauts.lackr.testutils.InterpolrTestUtils;
import com.fotonauts.lackr.testutils.TextUtils;

//@formatter:off
public class TestHandlebarsESI {
    
    Interpolr interpolr;
    
    @Before()
    public void setup() throws Exception {
        interpolr = Factory.buildInterpolr("json handlebars esi $$inline_wrapper");
        interpolr.start();
    }

    @After()
    public void tearDown() throws Exception {
        interpolr.stop();
        assertTrue(Thread.getAllStackTraces().size() < 10);
    }

    private String expand(String s) {
        return InterpolrTestUtils.expand(interpolr, s);
    }

    @Test
    public void testMustache() throws Exception {
        String result = expand(TextUtils.S(/*
            <!-- lackr:handlebars:template name="template_name" -->
                some text from the template name:{{name}} value:{{value}}
            <!-- /lackr:handlebars:template -->
            <!-- lackr:handlebars:eval name="template_name" -->
                { "name": "the name", "value": "the value" }
            <!-- /lackr:handlebars:eval -->
        */));
        assertNearlyEquals("some text from the template name:the name value:the value", result);
    }

    @Test
    public void testMustacheReversed() throws Exception {
        String result = expand(TextUtils.S(/*
            <!-- lackr:handlebars:eval name="template_name" -->
                { "name": "the name", "value": "the value" }
            <!-- /lackr:handlebars:eval -->
            <!-- lackr:handlebars:template name="template_name" -->
                some text from the template name:{{name}} value:{{value}}
            <!-- /lackr:handlebars:template -->
        */));
        assertNearlyEquals("some text from the template name:the name value:the value", result);
    }

    @Test
    public void testMustacheWithEsi() throws Exception {
        String result = expand(TextUtils.S(/*
            <!-- lackr:handlebars:template name="template_name" -->
                some text from the template name:{{name}} value:{{value}} some:{{esi.some}}
            <!-- /lackr:handlebars:template -->
            <!-- lackr:handlebars:eval name="template_name" -->
                { "name": "the name", "value": "the value", "esi":"ssi:include:virtual:/esi.json" }
            <!-- /lackr:handlebars:eval --> */));
        assertNearlyEquals("some text from the template name:the name value:the value some:json crap", result);
    }

    @Test
    public void testMustacheTemplateWithEsi() throws Exception {
        String result = expand(TextUtils.S(/*
            <!-- lackr:handlebars:template name="template_name" -->
                <!--# include virtual="/esi.must" -->
            <!-- /lackr:handlebars:template -->
            <!-- lackr:handlebars:eval name="template_name" -->
                { "name": "the name", "value": "the value", "esi":"ssi:include:virtual:/esi.json" }
            <!-- /lackr:handlebars:eval -->*/));
        assertNearlyEquals("some text from the template name:the name value:the value some:json crap", result);
    }

    @Test
    @Ignore // this was compatible with mustache, but not handlebars
    public void testMustacheLenientParsing() throws Exception {
        String result = expand(TextUtils.S(/*
            <!-- lackr:handlebars:template name="template_name" -->
                some text from the template name:{{name} value:{{value}}
            <!-- /lackr:handlebars:template -->
            <!-- lackr:handlebars:eval name="template_name" -->
                { "name": "the name", "value": "the value" }
            <!-- /lackr:handlebars:eval -->*/));
        assertNearlyEquals("some text from the template name:{{name} value:the value", result);
    }

    @Test(expected=LackrPresentableError.class)
    public void testMustacheJsonParseException() throws Exception {
        InterpolrTestUtils.parseToContext(interpolr, TextUtils.S(/*
            <!-- lackr:handlebars:template name="template_name" -->
                some text from the template name:{{name}} value:{{value}}
            <!-- /lackr:handlebars:template -->
            <!-- lackr:handlebars:eval name="template_name" -->
                { "name": "the name "value": "the value" }
            <!-- /lackr:handlebars:eval -->*/));
    }

    // This test is now irrelevant (and broken) as we use a default value in
    // case a value is missing or null,
    // and I'm at a loss trying
    // to find another possible error case.
    @Ignore
    @Test
    public void testMustacheException() throws Exception {
        String result = expand(TextUtils.S(/*
            <!-- lackr:handlebars:template name="template_name" -->
                some text from the template name:{{name}} value:{{value}} blah:{{esi.blih}}
            <!-- /lackr:handlebars:template -->
            <!-- lackr:handlebars:eval name="template_name" -->
                { "name": "the name", "value": "the value", "esi":"ssi:include:virtual:/esi.json" }
            <!-- /lackr:handlebars:eval -->*/));
        assertNotNull("result is an error", result);
        assertTrue(result.contains("MustacheException"));
    }

    @Test
    public void testMustacheAbsentKeyInHybridKeys() throws Exception {
        String result = expand(TextUtils.S(/*
            <!-- lackr:handlebars:template name="template_name" -->
                {{ absent.stuff }}
            <!-- /lackr:handlebars:template -->
            <!-- lackr:handlebars:eval name="template_name" --> 
                { }
            <!-- /lackr:handlebars:eval -->*/));
        assertNearlyEquals("", result);
    }

    @Test(expected=LackrPresentableError.class)
    public void testMustacheNoTemplates() throws Exception {
        InterpolrTestUtils.parseToContext(interpolr, TextUtils.S(/*
            <!-- lackr:handlebars:eval name="bogus_template_name" -->
                { "name": "the name", "value": "the value" }
            <!-- /lackr:handlebars:eval -->*/));
    }

    @Test(expected=LackrPresentableError.class)
    public void testMustacheTemplateNotFound() throws Exception {
        InterpolrTestUtils.parseToContext(interpolr, TextUtils.S(/*
            <!-- lackr:handlebars:template name="template_name" -->
                some text from the template name:{{name}} value:{{value}} blah:{{esi.blih}}
            <!-- /lackr:handlebars:template -->
            <!-- lackr:handlebars:eval name="bogus_template_name" -->
                { "name": "the name", "value": "the value" }
            <!-- /lackr:handlebars:eval -->*/));
    }

    @Test
    public void testPartial() throws Exception {
        String result = expand(TextUtils.S(/*
            <!-- lackr:handlebars:template name="partial" -->
                some text from the template name:{{name}} value:{{value}}
            <!-- /lackr:handlebars:template -->
            <!-- lackr:handlebars:template name="main" -->
                main opens {{>partial}} and closes
            <!-- /lackr:handlebars:template -->
            <!-- lackr:handlebars:eval name="main" -->
                { "name": "the name", "value": "the value" }
            <!-- /lackr:handlebars:eval -->*/));
        assertEquals("main opens\nsome text from the template name:the name value:the value\nand closes", result.replaceAll(" *\n *", "\n").trim());
    }


    protected void assertNearlyEquals(String expected, String got) {
        assertNotNull(got);
        if(!expected.trim().replaceAll("[ \n]+"," ").equals(got.trim().replaceAll("[ \n]+"," "))) {
            assertEquals(expected, got);
        }
    }
    protected void assertContains(String haystack, String needle) {
        assertTrue(haystack + "\n\nexpected to contain\n\n" + needle, haystack.contains(needle));
    }
}
