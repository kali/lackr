package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fotonauts.lackr.interpolr.Interpolr;
import com.fotonauts.lackr.interpolr.InterpolrContext;
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
            <!-- lackr:mustache:template name="template_name" -->
                some text from the template name:{{name}} value:{{value}}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="template_name" -->
                { "name": "the name", "value": "the value" }
            <!-- /lackr:mustache:eval -->
        */));
        assertNearlyEquals("some text from the template name:the name value:the value", result);
    }

    @Test
    public void testMustacheReversed() throws Exception {
        String result = expand(TextUtils.S(/*
            <!-- lackr:mustache:eval name="template_name" -->
                { "name": "the name", "value": "the value" }
            <!-- /lackr:mustache:eval -->
            <!-- lackr:mustache:template name="template_name" -->
                some text from the template name:{{name}} value:{{value}}
            <!-- /lackr:mustache:template -->
        */));
        assertNearlyEquals("some text from the template name:the name value:the value", result);
    }

    @Test
    public void testMustacheWithEsi() throws Exception {
        String result = expand(TextUtils.S(/*
            <!-- lackr:mustache:template name="template_name" -->
                some text from the template name:{{name}} value:{{value}} some:{{esi.some}}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="template_name" -->
                { "name": "the name", "value": "the value", "esi":"ssi:include:virtual:/esi.json" }
            <!-- /lackr:mustache:eval --> */));
        assertNearlyEquals("some text from the template name:the name value:the value some:json crap", result);
    }

    @Test
    public void testMustacheTemplateWithEsi() throws Exception {
        String result = expand(TextUtils.S(/*
            <!-- lackr:mustache:template name="template_name" -->
                <!--# include virtual="/esi.must" -->
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="template_name" -->
                { "name": "the name", "value": "the value", "esi":"ssi:include:virtual:/esi.json" }
            <!-- /lackr:mustache:eval -->*/));
        assertNearlyEquals("some text from the template name:the name value:the value some:json crap", result);
    }

    @Test
    @Ignore // this was compatible with mustache, but not handlebars
    public void testMustacheLenientParsing() throws Exception {
        String result = expand(TextUtils.S(/*
            <!-- lackr:mustache:template name="template_name" -->
                some text from the template name:{{name} value:{{value}}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="template_name" -->
                { "name": "the name", "value": "the value" }
            <!-- /lackr:mustache:eval -->*/));
        assertNearlyEquals("some text from the template name:{{name} value:the value", result);
    }

    @Test()
    public void testMustacheJsonParseException() throws Exception {
        InterpolrContext result = InterpolrTestUtils.parseToContext(interpolr, TextUtils.S(/*
            <!-- lackr:mustache:template name="template_name" -->
                some text from the template name:{{name}} value:{{value}}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="template_name" -->
                { "name": "the name "value": "the value" }
            <!-- /lackr:mustache:eval -->*/));
        assertTrue(result.getErrors().size() > 0);
        assertContains(result.getErrors().get(0).getMessage(), "JsonParseException");
    }

    // This test is now irrelevant (and broken) as we use a default value in
    // case a value is missing or null,
    // and I'm at a loss trying
    // to find another possible error case.
    @Ignore
    @Test
    public void testMustacheException() throws Exception {
        String result = expand(TextUtils.S(/*
            <!-- lackr:mustache:template name="template_name" -->
                some text from the template name:{{name}} value:{{value}} blah:{{esi.blih}}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="template_name" -->
                { "name": "the name", "value": "the value", "esi":"ssi:include:virtual:/esi.json" }
            <!-- /lackr:mustache:eval -->*/));
        assertNotNull("result is an error", result);
        assertTrue(result.contains("MustacheException"));
    }

    @Test
    public void testMustacheAbsentKeyInHybridKeys() throws Exception {
        String result = expand(TextUtils.S(/*
            <!-- lackr:mustache:template name="template_name" -->
                {{ absent.stuff }}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="template_name" --> 
                { }
            <!-- /lackr:mustache:eval -->*/));
        assertNearlyEquals("", result);
    }

    @Test()
    public void testMustacheNoTemplates() throws Exception {
        InterpolrContext result = InterpolrTestUtils.parseToContext(interpolr, TextUtils.S(/*
            <!-- lackr:mustache:eval name="bogus_template_name" -->
                { "name": "the name", "value": "the value" }
            <!-- /lackr:mustache:eval -->*/));
        assertTrue(result.getErrors().size() > 0);
        assertContains(result.getErrors().get(0).getMessage(), "Mustache template not found");
    }

    @Test
    public void testMustacheTemplateNotFound() throws Exception {
        InterpolrContext result = InterpolrTestUtils.parseToContext(interpolr, TextUtils.S(/*
            <!-- lackr:mustache:template name="template_name" -->
                some text from the template name:{{name}} value:{{value}} blah:{{esi.blih}}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="bogus_template_name" -->
                { "name": "the name", "value": "the value" }
            <!-- /lackr:mustache:eval -->*/));
        assertNotNull("result is an error", result);
        assertFalse("error found", result.getErrors().isEmpty());
    }

    @Test
    public void testPartial() throws Exception {
        String result = expand(TextUtils.S(/*
            <!-- lackr:mustache:template name="partial" -->
                some text from the template name:{{name}} value:{{value}}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:template name="main" -->
                main opens {{>partial}} and closes
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="main" -->
                { "name": "the name", "value": "the value" }
            <!-- /lackr:mustache:eval -->*/));
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