package com.fotonauts.lackr;

import static org.junit.Assert.*;

import org.junit.Test;

// disable formatting in this file
// @formatter:off

public class TestArchive extends BaseTestSubstitution {

    public TestArchive() throws Exception {
    }

    @Test
    public void testMultilineStringHack() throws Exception {
        assertEquals("blah", S(/*blah*/));
        assertEquals("blah\n                    blah", 
                S(/*blah
                    blah*/));
    }

    @Test
    public void testCapture() throws Exception {
        String archive = S(/*
            <script type="vnd.fotonauts/picordata" id="archive_1">
                { "root_id": 1, "objects": { "1" : { "name": "object number 1" } } }
            </script><!-- END OF ARCHIVE -->
        */);

        String page = archive + S(/*DUMP: <!-- lackr:mustache:dump archive="archive_1" -->*/);
        String result = expand(page);

        assertContains(result, archive); // archive is not swallowed
        assertContains(result, S(
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
    public void testMustacheEval() throws Exception {
        String archive = S(/*
                <script type="vnd.fotonauts/picordata" id="archive_1">
                    { "root_id": 1, "objects": { "1" : { "name": "object number 1" } } }
                </script><!-- END OF ARCHIVE -->
            */);
        String result = expand(archive + S(/*
            <!-- lackr:mustache:template name="template_name" -->
                NAME: {{object.name}}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="template_name" -->
                { "object": { "$$archive" : "archive_1", "$$id" : 1 } }
            <!-- /lackr:mustache:eval -->
        */));
        assertContains(result.trim(), "NAME: object number 1");
    }

}
