package com.fotonauts.lackr;

import static com.fotonauts.lackr.testutils.TextUtils.assertContains;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fotonauts.lackr.interpolr.Interpolr;
import com.fotonauts.lackr.testutils.Factory;
import com.fotonauts.lackr.testutils.InterpolrTestUtils;
import com.fotonauts.lackr.testutils.TextUtils;

// disable formatting in this file
// @formatter:off

public class TestJsonPlugin {
    
    Interpolr interpolr;
    
    @Before()
    public void setup() throws Exception {
        interpolr = Factory.buildInterpolr("json handlebars $$inline_wrapper");
        interpolr.start();
    }

    @After()
    public void tearDown() throws Exception {
        interpolr.stop();
        assertTrue(Thread.getAllStackTraces().size() < 10);
    }
    
    @Test
    public void testCapture() throws Exception {
        String archive = TextUtils.S(/*
            <script type="vnd.fotonauts/lackrarchive" id="archive_1">
                { "root_id": 1, "objects": { "1" : { "name": "object number 1" } } }
            </script><!-- END OF ARCHIVE -->
        */);

        String page = archive + TextUtils.S(/*DUMP: <!-- lackr:handlebars:dump archive="archive_1" -->*/);
        String result = InterpolrTestUtils.expand(interpolr, page);
        assertNotNull(result);
        assertContains(result, archive); // archive is not swallowed
        assertContains(result, TextUtils.S(
/*
DUMP: {
  "root_id" : 1,
  "objects" : {
    "1" : {
      "name" : "object number 1"
    }
  }
}*/).trim());
    }

    @Test
    public void testSurviveEmptyArchiveTag() throws Exception {
        String archive = TextUtils.S(/*
                <script type="vnd.fotonauts/lackrarchive" id="archive_1">

                </script><!-- END OF ARCHIVE -->
            */);

            String page = archive + TextUtils.S(/*Woohoo!*/);
            String result = InterpolrTestUtils.expand(interpolr, page);
            assertContains(result, "Woohoo!");
    }

    @Test
    public void testMustacheEvalSimple() throws Exception {
        String archive = TextUtils.S(/*
                <script type="vnd.fotonauts/lackrarchive" id="archive_1">
                    { "root_id": 1, "objects": { "1" : { "name": "object number 1" } } }
                </script><!-- END OF ARCHIVE -->
            */);
        String result =  InterpolrTestUtils.expand(interpolr, archive + TextUtils.S(/*
            <!-- lackr:handlebars:template name="template_name" -->
                NAME1: {{object.name}}
            <!-- /lackr:handlebars:template -->
            <!-- lackr:handlebars:eval name="template_name" -->
                { "object": { "$$archive" : "archive_1", "$$id" : 1 } }
            <!-- /lackr:handlebars:eval -->
            */));
        assertContains(result.trim(), "NAME1: object number 1");
    }

    @Test
    public void testMustacheEval() throws Exception {
        String archive = TextUtils.S(/*
                <script type="vnd.fotonauts/lackrarchive" id="archive_1">
                    { "root_id": 1, "objects": { "1" : { "name": "object number 1" } } }
                </script><!-- END OF ARCHIVE -->
            */);
        String result =  InterpolrTestUtils.expand(interpolr, archive + TextUtils.S(/*
            <!-- lackr:handlebars:template name="template_name" -->
                NAME1: {{object.name}}
            <!-- /lackr:handlebars:template -->
            <!-- lackr:handlebars:eval name="template_name" -->
                { "object": { "$$archive" : "archive_1", "$$id" : 1 } }
            <!-- /lackr:handlebars:eval -->
            
            <!-- lackr:handlebars:template name="toplevel" -->
                NAME2: {{name}}
            <!-- /lackr:handlebars:template -->
            <!-- lackr:handlebars:eval name="toplevel" -->
                { "$$archive" : "archive_1", "$$id" : 1 }
            <!-- /lackr:handlebars:eval -->

            <!-- lackr:handlebars:template name="items" -->
                NAME3: {{#items}}{{name}} {{/items}}
            <!-- /lackr:handlebars:template -->
            <!-- lackr:handlebars:eval name="items" -->
                { "items": [ { "$$archive" : "archive_1", "$$id" : 1 }, { "name": "foo" } ] } 
            <!-- /lackr:handlebars:eval -->
        */));
        assertContains(result.trim(), "NAME1: object number 1");
        assertContains(result.trim(), "NAME2: object number 1");
        assertContains(result.trim(), "NAME3: object number 1 foo");
    }

    @Test
    public void testMustacheEvalReversed() throws Exception {
        String archive = TextUtils.S(/*
                <script type="vnd.fotonauts/lackrarchive" id="archive_1">
                    { "root_id": 1, "objects": { "1" : { "name": "object number 1" } } }
                </script><!-- END OF ARCHIVE -->
            */);
        String result =  InterpolrTestUtils.expand(interpolr, TextUtils.S(/*
            <!-- lackr:handlebars:template name="template_name" -->
                NAME1: {{object.name}}
            <!-- /lackr:handlebars:template -->
            <!-- lackr:handlebars:eval name="template_name" -->
                { "object": { "$$archive" : "archive_1", "$$id" : 1 } }
            <!-- /lackr:handlebars:eval -->
        */) + archive);
        assertContains(result.trim(), "NAME1: object number 1");
    }

    @Test
    public void testArchiveJSObjectDeser() throws Exception {
        String result = InterpolrTestUtils.expand(interpolr, TextUtils.S(/*
                <script type="vnd.fotonauts/lackrarchive" id="archive_1">
                    { "root_id": 1, "objects": { 
                          "1" : { "$ATTR" : { "name": "object number 1" } } 
                    } }
                </script><!-- END OF ARCHIVE -->
                
                <!-- lackr:handlebars:template name="template_name" -->
                    NAME1: {{name}}
                <!-- /lackr:handlebars:template -->
                
                <!-- lackr:handlebars:eval name="template_name" -->
                    { "$$archive" : "archive_1", "$$id" : 1 }
                <!-- /lackr:handlebars:eval -->
        */));
        assertContains(result.trim(), "NAME1: object number 1");        
    }

    @Test
    public void testArchiveReferenceDeser() throws Exception {
        String result = InterpolrTestUtils.expand(interpolr, TextUtils.S(/*
                <script type="vnd.fotonauts/lackrarchive" id="archive_1">
                    { "root_id": 1, "objects": {
                          "1" : { "$ATTR" : { "name": "darth", "kids" : [ { "$$id" : 2 }, { "$$id" : 3 } ] } }, 
                          "2" : { "name": "luke", "dad": { "$$id" : 1 } }, 
                          "3" : { "$ATTR" : { "name": "leia", "dad": { "$$id" : 1 } } } 
                    } }
                </script><!-- END OF ARCHIVE -->
                
                <!-- lackr:handlebars:template name="kids" -->
                    KIDS: {{#kids}}{{name}} {{/kids}}
                <!-- /lackr:handlebars:template -->
                
                <!-- lackr:handlebars:eval name="kids" -->
                    { "$$archive" : "archive_1", "$$id" : 1 }
                <!-- /lackr:handlebars:eval -->
                
                <!-- lackr:handlebars:template name="dad" -->
                    DAD: {{dad.name}}
                <!-- /lackr:handlebars:template -->
                
                <!-- lackr:handlebars:eval name="dad" -->
                    { "$$archive" : "archive_1", "$$id" : 2 }
                <!-- /lackr:handlebars:eval -->
        */));
        assertContains(result.trim(), "KIDS: luke leia");        
        assertContains(result.trim(), "DAD: darth");        
    }

    @Test
    public void testNullTokenInPath() throws Exception {
        String result = InterpolrTestUtils.expand(interpolr, TextUtils.S(/*
                <!-- lackr:handlebars:template name="t" -->
                    expect empty string: {{ ad_model._id }} :
                <!-- /lackr:handlebars:template -->

                <!-- lackr:handlebars:eval name="t" -->
                    { "ad_model": null }
                <!-- /lackr:handlebars:eval -->
        */));
        assertContains(result.trim(), "expect empty string:  :");
    }

    @Test
    public void testInlineWrapperSubstitution() throws Exception {
        String page = TextUtils.S(/*
            <!-- lackr:handlebars:template name="t" -->
                {{toplevel}} {{innerInt}} {{innerString}}
            <!-- /lackr:handlebars:template -->
            <!-- lackr:handlebars:eval name="t" -->
                { "toplevel": "TOP", "toplevelkey":{ "$$inline_wrapper" : { "innerInt" : 42, "innerString" : "foo" } } }
            <!-- /lackr:handlebars:eval -->*/);
        String result = InterpolrTestUtils.expand(interpolr, page);
        assertContains(result, "TOP 42 foo");
    }

    @Test
    public void testInlineWrapperSubstitutionInArchive() throws Exception {
        String page = TextUtils.S(/*
                <script type="vnd.fotonauts/lackrarchive" id="archive_1">
                    { "root_id": 1, "objects": {
                          "1" : { "$ATTR": { "stuff" : "some", "foo": { "junk": { "$$inline_wrapper" : { "items" : [ { "name" : "name" }] } } } } }
                    } }
                </script><!-- END OF ARCHIVE -->

            <!-- lackr:handlebars:template name="t" -->
                expectname:{{#root.foo.items}}{{name}} {{/root.foo.items}}
            <!-- /lackr:handlebars:template -->

            <!-- lackr:handlebars:eval name="t" -->
                    { "root" : { "$$archive" : "archive_1", "$$id" : 1 } }
            <!-- /lackr:handlebars:eval -->*/);
        String result = InterpolrTestUtils.expand(interpolr, page);
        assertContains(result, "expectname:name");
    }

}
