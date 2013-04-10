package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class TestMustache extends BaseTestSubstitution {

    public TestMustache() throws Exception {
    }

    @Test
    public void testMustache() throws Exception {
        String result = expand("<!-- lackr:mustache:template name=\"template_name\" -->\n"
                + "some text from the template name:{{name}} value:{{value}}\n" + "<!-- /lackr:mustache:template -->\n"
                + "<!-- lackr:mustache:eval name=\"template_name\" -->\n"
                + "{ \"name\": \"the name\", \"value\": \"the value\" }\n" + "<!-- /lackr:mustache:eval -->\n");
        assertEquals("\n\nsome text from the template name:the name value:the value\n\n", result);
    }

    @Test
    public void testMustacheWithEsi() throws Exception {
        String result = expand("<!-- lackr:mustache:template name=\"template_name\" -->\n"
                + "some text from the template name:{{name}} value:{{value}} some:{{esi.some}}\n"
                + "<!-- /lackr:mustache:template -->\n" + "<!-- lackr:mustache:eval name=\"template_name\" -->\n"
                + "{ \"name\": \"the name\", \"value\": \"the value\", \"esi\":\"ssi:include:virtual:/esi.json\" }\n"
                + "<!-- /lackr:mustache:eval -->\n");
        assertEquals("\n\nsome text from the template name:the name value:the value some:json crap\n\n", result);
    }

    @Test
    public void testMustacheTemplateWithEsi() throws Exception {
        String result = expand("<!-- lackr:mustache:template name=\"template_name\" -->\n"
                + "<!--# include virtual=\"/esi.must\" -->\n" + "<!-- /lackr:mustache:template -->\n"
                + "<!-- lackr:mustache:eval name=\"template_name\" -->\n"
                + "{ \"name\": \"the name\", \"value\": \"the value\", \"esi\":\"ssi:include:virtual:/esi.json\" }\n"
                + "<!-- /lackr:mustache:eval -->\n");
        assertEquals("\n\nsome text from the template name:the name value:the value some:json crap\n\n", result);
    }

    @Test
    @Ignore
    public void testMustacheLenientParsing() throws Exception {
        String result = expand("<!-- lackr:mustache:template name=\"template_name\" -->\n"
                + "some text from the template name:{{name} value:{{value}}\n" + "<!-- /lackr:mustache:template -->\n"
                + "<!-- lackr:mustache:eval name=\"template_name\" -->\n"
                + "{ \"name\": \"the name\", \"value\": \"the value\" }\n" + "<!-- /lackr:mustache:eval -->\n", false);
        assertEquals("\n\nsome text from the template name:{{name} value:the value\n\n", result);
    }

    @Test
    public void testMustacheJsonParseException() throws Exception {
        String result = expand("<!-- lackr:mustache:template name=\"template_name\" -->\n"
                + "some text from the template name:{{name}} value:{{value}}\n" + "<!-- /lackr:mustache:template -->\n"
                + "<!-- lackr:mustache:eval name=\"template_name\" -->\n" + "{ \"name\": \"the name \"value\": \"the value\" }\n"
                + "<!-- /lackr:mustache:eval -->\n", true);
        assertNotNull("result is an error", result);
        assertTrue(result.contains("JsonParseException"));
    }

    // This test is now irrelevant (and broken) as we use a default value in
    // case a value is missing or null,
    // and I'm at a loss trying
    // to find another possible error case.
    @Ignore
    @Test
    public void testMustacheException() throws Exception {
        String result = expand("<!-- lackr:mustache:template name=\"template_name\" -->\n"
                + "some text from the template name:{{name}} value:{{value}} blah:{{esi.blih}}\n"
                + "<!-- /lackr:mustache:template -->\n" + "<!-- lackr:mustache:eval name=\"template_name\" -->\n"
                + "{ \"name\": \"the name\", \"value\": \"the value\", \"esi\":\"ssi:include:virtual:/esi.json\" }\n"
                + "<!-- /lackr:mustache:eval -->\n", true);
        assertNotNull("result is an error", result);
        assertTrue(result.contains("MustacheException"));
    }

    @Test
    public void testMustacheAbsentKeyInHybridKeys() throws Exception {
        String result = expand("<!-- lackr:mustache:template name=\"template_name\" -->\n" + "{{ absent.stuff }}\n"
                + "<!-- /lackr:mustache:template -->\n" + "<!-- lackr:mustache:eval name=\"template_name\" -->\n" + "{ }\n"
                + "<!-- /lackr:mustache:eval -->\n");
        assertEquals("\n\n\n\n", result);
    }

    @Test
    public void testMustacheNoTemplates() throws Exception {
        String result = expand("<!-- lackr:mustache:eval name=\"bogus_template_name\" -->\n"
                + "{ \"name\": \"the name\", \"value\": \"the value\", \"esi\":\"ssi:include:virtual:/esi.json\" }\n"
                + "<!-- /lackr:mustache:eval -->\n", true);
        assertNotNull("result is an error", result);
        assertTrue(result.contains("Mustache template not found"));
    }

    @Test
    public void testMustacheTemplateNotFound() throws Exception {
        String result = expand("<!-- lackr:mustache:template name=\"template_name\" -->\n"
                + "some text from the template name:{{name}} value:{{value}} blah:{{esi.blih}}\n"
                + "<!-- /lackr:mustache:template -->\n" + "<!-- lackr:mustache:eval name=\"bogus_template_name\" -->\n"
                + "{ \"name\": \"the name\", \"value\": \"the value\", \"esi\":\"ssi:include:virtual:/esi.json\" }\n"
                + "<!-- /lackr:mustache:eval -->\n", true);
        assertNotNull("result is an error", result);
        assertTrue(result.contains("Mustache template not found"));
    }

    @Test
    public void testInlineImageFlagSupport() throws Exception {
        String result = expand("<!-- lackr:mustache:template name=\"template_name\" -->\n"
                + "some text from the template name:{{name}} value:{{value}} inline:{{_ftn_inline_images}}\n"
                + "<!-- /lackr:mustache:template -->\n" + "<!-- lackr:mustache:eval name=\"template_name\" -->\n"
                + "{ \"name\": \"the name\", \"value\": \"the value\" }\n" + "<!-- /lackr:mustache:eval -->\n");
        assertEquals("\n\nsome text from the template name:the name value:the value inline:false\n\n", result);
    }

    @Test
    public void testInlineLocaleSupport() throws Exception {
        String result = expand("<!-- lackr:mustache:template name=\"template_name\" -->\n" + " inline:{{_ftn_locale}}\n"
                + "<!-- /lackr:mustache:template -->\n" + "<!-- lackr:mustache:eval name=\"template_name\" -->\n" + "{ }\n"
                + "<!-- /lackr:mustache:eval -->\n", false, "es.localhost");
        assertEquals("\n\n inline:es\n\n", result);
        result = expand("<!-- lackr:mustache:template name=\"template_name\" -->\n" + " inline:{{_ftn_locale}}\n"
                + "<!-- /lackr:mustache:template -->\n" + "<!-- lackr:mustache:eval name=\"template_name\" -->\n" + "{ }\n"
                + "<!-- /lackr:mustache:eval -->\n", false, "fr.localhost");
        assertEquals("\n\n inline:fr\n\n", result);
        result = expand("<!-- lackr:mustache:template name=\"template_name\" -->\n" + " inline:{{_ftn_locale}}\n"
                + "<!-- /lackr:mustache:template -->\n" + "<!-- lackr:mustache:eval name=\"template_name\" -->\n" + "{ }\n"
                + "<!-- /lackr:mustache:eval -->\n");
        assertTrue(result.indexOf("inline:en") >= 0);
    }

    @Test
    public void testPartial() throws Exception {
        String result = expand("<!-- lackr:mustache:template name=\"partial\" -->\n"
                + "some text from the template name:{{name}} value:{{value}}\n"
                + "<!-- /lackr:mustache:template -->\n"
                + "<!-- lackr:mustache:template name=\"main\" -->main opens {{>partial}} and closes<!-- /lackr:mustache:template -->\n"
                + "<!-- lackr:mustache:eval name=\"main\" -->\n" + "{ \"name\": \"the name\", \"value\": \"the value\" }\n"
                + "<!-- /lackr:mustache:eval -->\n");
        assertEquals("\n\nmain opens \nsome text from the template name:the name value:the value\n and closes\n", result);
    }

    @Test
    public void testEach() throws Exception {
        String result = expand("<!-- lackr:mustache:template name=\"t\" -->\n" + "{{#each ints}}{{a}}{{/each}}"
                + "<!-- /lackr:mustache:template -->\n" + "<!-- lackr:mustache:eval name=\"t\" -->\n"
                + "{ \"ints\": [ { \"a\": 1}, { \"a\": 2}, {\"a\": 3 } ] }\n" + "<!-- /lackr:mustache:eval -->\n");
        assertEquals("\n\n123\n", result);
    }

    @Test
    public void testReverseEach() throws Exception {
        String result = expand("<!-- lackr:mustache:template name=\"t\" -->\n" + "{{#reverse_each ints}}{{a}}{{/reverse_each}}"
                + "<!-- /lackr:mustache:template -->\n" + "<!-- lackr:mustache:eval name=\"t\" -->\n" + "{ \"ints\": [ ] }\n"
                + "<!-- /lackr:mustache:eval -->\n");
        assertEquals("\n\n\n", result);
        result = expand("<!-- lackr:mustache:template name=\"t\" -->\n" + "{{#reverse_each ints}}{{a}}{{/reverse_each}}"
                + "<!-- /lackr:mustache:template -->\n" + "<!-- lackr:mustache:eval name=\"t\" -->\n"
                + "{ \"ints\": [ { \"a\": 1}, { \"a\": 2}, {\"a\": 3 } ] }\n" + "<!-- /lackr:mustache:eval -->\n");
        assertEquals("\n\n321\n", result);
    }

    @Test
    public void testAbsoluteDateTime() throws Exception {
        String page = "<!-- lackr:mustache:template name=\"t\" -->\n"
                + "{{absolute_datetime landed_at format=\"date_time\" type=\"short\"}}\n"
                + "{{absolute_datetime landed_at format=\"date_time\" type=\"medium\"}}\n"
                + "{{absolute_datetime landed_at format=\"date_time\" type=\"long\"}}\n"
                + "{{absolute_datetime landed_at format=\"date_time\" type=\"full\"}}\n"
                + "{{absolute_datetime landed_at format=\"date\" type=\"short\"}}\n"
                + "{{absolute_datetime landed_at format=\"date\" type=\"medium\"}}\n"
                + "{{absolute_datetime landed_at format=\"date\" type=\"long\"}}\n"
                + "{{absolute_datetime landed_at format=\"date\" type=\"full\"}}\n"
                + "{{absolute_datetime landed_at format=\"time\" type=\"short\"}}\n"
                + "{{absolute_datetime landed_at format=\"time\" type=\"medium\"}}\n"
                + "{{absolute_datetime landed_at format=\"time\" type=\"long\"}}\n"
                + "{{absolute_datetime landed_at format=\"time\" type=\"full\"}}\n" + "<!-- /lackr:mustache:template -->\n"
                + "<!-- lackr:mustache:eval name=\"t\" -->\n" + "{ \"landed_at\": -14186520 }\n"
                + "<!-- /lackr:mustache:eval -->\n";
        String en = expand(page, false, "localhost");
        String[] got = en.trim().split("\\n");
        String[] expected = new String[] { "7/20/69, 7:18 PM", "Jul 20, 1969, 7:18:00 PM", "July 20, 1969 at 7:18:00 PM GMT",
                "Sunday, July 20, 1969 at 7:18:00 PM GMT", "7/20/69", "Jul 20, 1969", "July 20, 1969", "Sunday, July 20, 1969",
                "7:18 PM", "7:18:00 PM", "7:18:00 PM GMT", "7:18:00 PM GMT" };
        Assert.assertArrayEquals(expected, got);

        String fr = expand(page, false, "fr.localhost");
        String[] obtenu = fr.trim().split("\\n");
        String[] attendu = new String[] { "20/07/1969 19:18", "20 juil. 1969 19:18:00", "20 juillet 1969 19:18:00 UTC",
                "dimanche 20 juillet 1969 19:18:00 UTC", "20/07/1969", "20 juil. 1969", "20 juillet 1969",
                "dimanche 20 juillet 1969", "19:18", "19:18:00", "19:18:00 UTC", "19:18:00 UTC" };
        Assert.assertArrayEquals(attendu, obtenu);
    }

    @Test
    public void testRelativeDateTime() throws Exception {
        String page = "<!-- lackr:mustache:template name=\"t\" -->\n" + "{{relative_datetime at}}\n"
                + "<!-- /lackr:mustache:template -->\n" + "<!-- lackr:mustache:eval name=\"t\" -->\n" + "{ \"at\": "
                + (System.currentTimeMillis() / 1000 + 86410) + " }\n" + "<!-- /lackr:mustache:eval -->\n";
        String en = expand(page, false, "localhost");
        String[] got = en.trim().split("\\n");
        String[] expected = new String[] { "1 day from now" };
        Assert.assertArrayEquals(expected, got);

        String fr = expand(page, false, "fr.localhost");
        String[] obtenu = fr.trim().split("\\n");
        String[] attendu = new String[] { "dans 1 jour" };
        Assert.assertArrayEquals(attendu, obtenu);
    }

    @Test
    public void testDerivatives() throws Exception {
        String page = "<!-- lackr:mustache:template name=\"t\" -->{{#items}}{{derivative item kind=\"image\"}}\n{{/items}}<!-- /lackr:mustache:template -->"
                + "<!-- lackr:mustache:eval name=\"t\" -->\n"
                + "{ \"items\": ["
                + "{ \"item\": { \"_id\" : \"kali-hNvjiyDiSOA\" } },\n"
                + "{ \"item\": { \"_id\" : \"kali-hNvjiyDiSOA\", \"media_base_id\": \"kali-12\" } },\n"
                + "{ \"item\": { \"_id\" : \"kali-hNvjiyDiSOA\", \"format\": \"PNG\" } },\n"
                + "{ \"item\": { \"_id\" : \"kali-hNvjiyDiSOA\", \"upload_grid\": \"testing\" } }\n"
                + "] }<!-- /lackr:mustache:eval -->\n";
        String result = expand(page).trim();
        assertEquals("http://images.cdn.fotopedia.com/kali-hNvjiyDiSOA-image.jpg\n"
                + "http://images.cdn.fotopedia.com/kali-12-image.jpg\n"
                + "http://images.cdn.fotopedia.com/kali-hNvjiyDiSOA-image.png\n"
                + "http://images.cdn.testing.ftnz.net/kali-hNvjiyDiSOA-image.jpg", result);
    }
}
