package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

// disable formatting in this file
// @formatter:off

public class TestArchive extends BaseTestSubstitution {

    public TestArchive() throws Exception {
    }

    @Test
    public void testMultilineStringHack() throws Exception {
        assertEquals("blah", TextUtils.S(/*blah*/));
        assertEquals("blah\n                    blah", 
                TextUtils.S(/*blah
                    blah*/));
    }

    @Test
    public void testCapture() throws Exception {
        String archive = TextUtils.S(/*
            <script type="vnd.fotonauts/picordata" id="archive_1">
                { "root_id": 1, "objects": { "1" : { "name": "object number 1" } } }
            </script><!-- END OF ARCHIVE -->
        */);

        String page = archive + TextUtils.S(/*DUMP: <!-- lackr:mustache:dump archive="archive_1" -->*/);
        String result = expand(page);
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
                <script type="vnd.fotonauts/picordata" id="archive_1">

                </script><!-- END OF ARCHIVE -->
            */);

            String page = archive + TextUtils.S(/*Woohoo!*/);
            String result = expand(page);
            assertContains(result, "Woohoo!");
    }

    @Test
    public void testMustacheEval() throws Exception {
        String archive = TextUtils.S(/*
                <script type="vnd.fotonauts/picordata" id="archive_1">
                    { "root_id": 1, "objects": { "1" : { "name": "object number 1" } } }
                </script><!-- END OF ARCHIVE -->
            */);
        String result = expand(archive + TextUtils.S(/*
            <!-- lackr:mustache:template name="template_name" -->
                NAME1: {{object.name}}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="template_name" -->
                { "object": { "$$archive" : "archive_1", "$$id" : 1 } }
            <!-- /lackr:mustache:eval -->
            
            <!-- lackr:mustache:template name="toplevel" -->
                NAME2: {{name}}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="toplevel" -->
                { "$$archive" : "archive_1", "$$id" : 1 }
            <!-- /lackr:mustache:eval -->

            <!-- lackr:mustache:template name="items" -->
                NAME3: {{#items}}{{name}} {{/items}}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="items" -->
                { "items": [ { "$$archive" : "archive_1", "$$id" : 1 }, { "name": "crap" } ] } 
            <!-- /lackr:mustache:eval -->
        */));
        assertContains(result.trim(), "NAME1: object number 1");
        assertContains(result.trim(), "NAME2: object number 1");
        assertContains(result.trim(), "NAME3: object number 1 crap");
    }

    @Test
    public void testMustacheEvalReversed() throws Exception {
        String archive = TextUtils.S(/*
                <script type="vnd.fotonauts/picordata" id="archive_1">
                    { "root_id": 1, "objects": { "1" : { "name": "object number 1" } } }
                </script><!-- END OF ARCHIVE -->
            */);
        String result = expand(TextUtils.S(/*
            <!-- lackr:mustache:template name="template_name" -->
                NAME1: {{object.name}}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="template_name" -->
                { "object": { "$$archive" : "archive_1", "$$id" : 1 } }
            <!-- /lackr:mustache:eval -->
        */) + archive);
        assertContains(result.trim(), "NAME1: object number 1");
    }

    @Test
    public void testArchiveJSObjectDeser() throws Exception {
        String result = expand(TextUtils.S(/*
                <script type="vnd.fotonauts/picordata" id="archive_1">
                    { "root_id": 1, "objects": { 
                          "1" : { "$ATTR" : { "name": "object number 1" } } 
                    } }
                </script><!-- END OF ARCHIVE -->
                
                <!-- lackr:mustache:template name="template_name" -->
                    NAME1: {{name}}
                <!-- /lackr:mustache:template -->
                
                <!-- lackr:mustache:eval name="template_name" -->
                    { "$$archive" : "archive_1", "$$id" : 1 }
                <!-- /lackr:mustache:eval -->
        */));
        assertContains(result.trim(), "NAME1: object number 1");        
    }

    @Test
    public void testArchiveReferenceDeser() throws Exception {
        String result = expand(TextUtils.S(/*
                <script type="vnd.fotonauts/picordata" id="archive_1">
                    { "root_id": 1, "objects": {
                          "1" : { "$ATTR" : { "name": "darth", "kids" : [ { "$$id" : 2 }, { "$$id" : 3 } ] } }, 
                          "2" : { "name": "luke", "dad": { "$$id" : 1 } }, 
                          "3" : { "$ATTR" : { "name": "leia", "dad": { "$$id" : 1 } } } 
                    } }
                </script><!-- END OF ARCHIVE -->
                
                <!-- lackr:mustache:template name="kids" -->
                    KIDS: {{#kids}}{{name}} {{/kids}}
                <!-- /lackr:mustache:template -->
                
                <!-- lackr:mustache:eval name="kids" -->
                    { "$$archive" : "archive_1", "$$id" : 1 }
                <!-- /lackr:mustache:eval -->
                
                <!-- lackr:mustache:template name="dad" -->
                    DAD: {{dad.name}}
                <!-- /lackr:mustache:template -->
                
                <!-- lackr:mustache:eval name="dad" -->
                    { "$$archive" : "archive_1", "$$id" : 2 }
                <!-- /lackr:mustache:eval -->
        */));
        assertContains(result.trim(), "KIDS: luke leia");        
        assertContains(result.trim(), "DAD: darth");        
    }

}
