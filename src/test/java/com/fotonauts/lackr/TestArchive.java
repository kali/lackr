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
        System.err.println(page);
        String result = expand(page);
        System.err.println(result);

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

}
