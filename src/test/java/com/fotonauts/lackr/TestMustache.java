package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.util.StringUtils;

//@formatter:off
public class TestMustache extends BaseTestSubstitution {

    public TestMustache() throws Exception {
    }

    @Test
    public void testMustache() throws Exception {
        String result = expand(S(/*
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
        String result = expand(S(/*
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
        String result = expand(S(/*
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
        String result = expand(S(/*
            <!-- lackr:mustache:template name="template_name" -->
                <!--# include virtual="/esi.must" -->
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="template_name" -->
                { "name": "the name", "value": "the value", "esi":"ssi:include:virtual:/esi.json" }
            <!-- /lackr:mustache:eval -->*/));
        assertNearlyEquals("some text from the template name:the name value:the value some:json crap", result);
    }

    @Test
    @Ignore
    public void testMustacheLenientParsing() throws Exception {
        String result = expand(S(/*
            <!-- lackr:mustache:template name="template_name" -->
                some text from the template name:{{name} value:{{value}}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="template_name" -->
                { "name": "the name", "value": "the value" }
            <!-- /lackr:mustache:eval -->*/), false);
        assertNearlyEquals("some text from the template name:{{name} value:the value", result);
    }

    @Test
    public void testMustacheJsonParseException() throws Exception {
        String result = expand(S(/*
            <!-- lackr:mustache:template name="template_name" -->
                some text from the template name:{{name}} value:{{value}}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="template_name" -->
                { "name": "the name "value": "the value" }
            <!-- /lackr:mustache:eval -->*/), true);
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
        String result = expand(S(/*
            <!-- lackr:mustache:template name="template_name" -->
                some text from the template name:{{name}} value:{{value}} blah:{{esi.blih}}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="template_name" -->
                { "name": "the name", "value": "the value", "esi":"ssi:include:virtual:/esi.json" }
            <!-- /lackr:mustache:eval -->*/), true);
        assertNotNull("result is an error", result);
        assertTrue(result.contains("MustacheException"));
    }

    @Test
    public void testMustacheAbsentKeyInHybridKeys() throws Exception {
        String result = expand(S(/*
            <!-- lackr:mustache:template name="template_name" -->
                {{ absent.stuff }}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="template_name" --> 
                { }
            <!-- /lackr:mustache:eval -->*/));
        assertNearlyEquals("", result);
    }

    @Test
    public void testMustacheNoTemplates() throws Exception {
        String result = expand(S(/*
            <!-- lackr:mustache:eval name="bogus_template_name" -->
                { "name": "the name", "value": "the value", "esi":"ssi:include:virtual:/esi.json" }
            <!-- /lackr:mustache:eval -->*/), true);
        assertNotNull("result is an error", result);
        assertTrue(result.contains("Mustache template not found"));
    }

    @Test
    public void testMustacheTemplateNotFound() throws Exception {
        String result = expand(S(/*
            <!-- lackr:mustache:template name="template_name" -->
                some text from the template name:{{name}} value:{{value}} blah:{{esi.blih}}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="bogus_template_name" -->
                { "name": "the name", "value": "the value", "esi":"ssi:include:virtual:/esi.json" }
            <!-- /lackr:mustache:eval -->*/), true);
        assertNotNull("result is an error", result);
        assertTrue(result.contains("Mustache template not found"));
    }

    @Test
    public void testInlineImageFlagSupport() throws Exception {
        String result = expand(S(/*
            <!-- lackr:mustache:template name="template_name" -->
                some text from the template name:{{name}} value:{{value}} inline:{{_ftn_inline_images}}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="template_name" -->
                { "name": "the name", "value": "the value" }
            <!-- /lackr:mustache:eval -->*/));
        assertNearlyEquals("some text from the template name:the name value:the value inline:false", result);
    }

    @Test
    public void testInlineLocaleSupport() throws Exception {
        String result = expand(S(/*
            <!-- lackr:mustache:template name="template_name" -->
                inline:{{_ftn_locale}}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="template_name" -->
                { }
            <!-- /lackr:mustache:eval -->*/), false, "es.localhost");
        assertNearlyEquals(result, "inline:es");
        result = expand(S(/*
            <!-- lackr:mustache:template name="template_name" -->
                inline:{{_ftn_locale}}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="template_name" -->
                { }
            <!-- /lackr:mustache:eval -->*/), false, "fr.localhost");
        assertNearlyEquals(result, "inline:fr");
        result = expand(S(/*
            <!-- lackr:mustache:template name="template_name" -->
                inline:{{_ftn_locale}}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="template_name" -->
                { }
            <!-- /lackr:mustache:eval -->*/));
        assertContains(result, "inline:en");
    }

    @Test
    public void testPartial() throws Exception {
        String result = expand(S(/*
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

    @Test
    public void testEach() throws Exception {
        String result = expand(S(/*
            <!-- lackr:mustache:template name="t" -->
                {{#each ints}}{{a}}{{/each}}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="t" -->
                { "ints": [ { "a": 1}, { "a": 2}, {"a": 3 } ] }
            <!-- /lackr:mustache:eval -->*/));
        assertEquals("123", result.trim());
    }

    @Test
    public void testReverseEach() throws Exception {
        String result = expand(S(/*
                <!-- lackr:mustache:template name="t" -->
                    {{#reverse_each ints}}{{a}}{{/reverse_each}}
                <!-- /lackr:mustache:template -->
                <!-- lackr:mustache:eval name="t" -->
                    { "ints": [ ] }
                <!-- /lackr:mustache:eval -->*/));
        assertEquals("", result.trim());
        result = expand(S(/*
            <!-- lackr:mustache:template name="t" -->
                {{#reverse_each ints}}{{a}}{{/reverse_each}}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="t" -->
                { "ints": [ { "a": 1}, { "a": 2}, {"a": 3 } ] }
            <!-- /lackr:mustache:eval -->*/));
        assertEquals("321", result.trim());
    }

    @Test
    public void testAbsoluteDateTime() throws Exception {
        String template = S(/*
                <!-- lackr:mustache:template name="t" -->
                {{absolute_datetime nowhere format="date_time" type="short"}}
                {{absolute_datetime landed_at format="date_time" type="short"}}
                {{absolute_datetime landed_at format="date_time" type="medium"}}
                {{absolute_datetime landed_at format="date_time" type="long"}}
                {{absolute_datetime landed_at format="date_time" type="full"}}
                {{absolute_datetime landed_at format="date" type="short"}}
                {{absolute_datetime landed_at format="date" type="medium"}}
                {{absolute_datetime landed_at format="date" type="long"}}
                {{absolute_datetime landed_at format="date" type="full"}}
                {{absolute_datetime landed_at format="time" type="short"}}
                {{absolute_datetime landed_at format="time" type="medium"}}
                {{absolute_datetime landed_at format="time" type="long"}}
                {{absolute_datetime landed_at format="time" type="full"}}
            <!-- /lackr:mustache:template -->*/);

        String dateAsInt = S(/*
                <!-- lackr:mustache:eval name="t" -->
                    { "landed_at": -14186520 }
                <!-- /lackr:mustache:eval -->
        */);

        String dateAsHash = S(/*
            <!-- lackr:mustache:eval name="t" -->
                { "landed_at": { "$DATE" : -14186520000 } }
            <!-- /lackr:mustache:eval -->
        */);

        String[] expected = new String[] { "7/20/69, 7:18 PM", "Jul 20, 1969, 7:18:00 PM", "July 20, 1969 at 7:18:00 PM GMT",
                "Sunday, July 20, 1969 at 7:18:00 PM GMT", "7/20/69", "Jul 20, 1969", "July 20, 1969", "Sunday, July 20, 1969",
                "7:18 PM", "7:18:00 PM", "7:18:00 PM GMT", "7:18:00 PM GMT" };
        String en = expand(template + dateAsInt, false, "localhost");
        String[] got = en.trim().replaceAll(" *\n[ \n]*", "\n").split("\n");
        Assert.assertArrayEquals(expected, got);

        en = expand(template + dateAsHash, false, "localhost");
        got = en.trim().replaceAll(" *\n[ \n]*", "\n").split("\n");
        Assert.assertArrayEquals(expected, got);

        String fr = expand(template + dateAsInt, false, "fr.localhost");
        String[] obtenu = fr.trim().replaceAll(" *\n[ \n]*", "\n").split("\n");
        String[] attendu = new String[] { "20/07/1969 19:18", "20 juil. 1969 19:18:00", "20 juillet 1969 19:18:00 UTC",
                "dimanche 20 juillet 1969 19:18:00 UTC", "20/07/1969", "20 juil. 1969", "20 juillet 1969",
                "dimanche 20 juillet 1969", "19:18", "19:18:00", "19:18:00 UTC", "19:18:00 UTC" };
        Assert.assertArrayEquals(attendu, obtenu);
    }

    @Test
    public void testRelativeDateTime() throws Exception {

        String template = S(/*
            <!-- lackr:mustache:template name="t" -->
                {{relative_datetime at}}{{relative_datetime nowhere}}
            <!-- /lackr:mustache:template -->
        */);
        String dateAsInt = "<!-- lackr:mustache:eval name=\"t\" --> { \"at\": " 
                + (System.currentTimeMillis() / 1000 + 86410) 
                + "} <!-- /lackr:mustache:eval -->";
        String dateAsHash = "<!-- lackr:mustache:eval name=\"t\" --> { \"at\": { \"$DATE\" : "
                + (System.currentTimeMillis() + 86410 * 1000) + "} } <!-- /lackr:mustache:eval -->";

        String en = expand(template + dateAsInt, false, "localhost");
        String[] got = en.trim().split("\n");
        String[] expected = new String[] { "1 day from now" };
        Assert.assertArrayEquals(expected, got);

        en = expand(template + dateAsHash, false, "localhost");
        got = en.trim().split("\n");
        expected = new String[] { "1 day from now" };
        Assert.assertArrayEquals(expected, got);

        String fr = expand(template + dateAsInt, false, "fr.localhost");
        String[] obtenu = fr.trim().split("\n");
        String[] attendu = new String[] { "dans 1 jour" };
        Assert.assertArrayEquals(attendu, obtenu);

        Assert.assertNotNull("icu4j it support", expand(template + dateAsInt, false, "it.localhost"));
        Assert.assertNotNull("icu4j es support", expand(template + dateAsInt, false, "es.localhost"));
        Assert.assertNotNull("icu4j zh support", expand(template + dateAsInt, false, "zh.localhost"));
        Assert.assertNotNull("icu4j ja support", expand(template + dateAsInt, false, "ja.localhost"));
        Assert.assertNotNull("icu4j pt support", expand(template + dateAsInt, false, "pt.localhost"));
        Assert.assertNotNull("icu4j de support", expand(template + dateAsInt, false, "de.localhost"));
        Assert.assertNotNull("icu4j ko support", expand(template + dateAsInt, false, "ko.localhost"));
        Assert.assertNotNull("icu4j ru support", expand(template + dateAsInt, false, "ru.localhost"));
    }

    protected void assertNearlyEquals(String expected, String got) {
        assertNotNull(got);
        if(!expected.trim().replaceAll("[ \n]+"," ").equals(got.trim().replaceAll("[ \n]+"," "))) {
            assertEquals(expected, got);
        }
    }
    
    @Test
    public void testDerivatives() throws Exception {
        String page = S(/*
            <!-- lackr:mustache:template name="t" -->
                {{#items}}{{derivative item kind="image"}} {{/items}}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="t" -->
                { "items": [
                    { "item": { "_id" : "kali-hNvjiyDiSOA" } },
                    { "item": { "_id" : "kali-hNvjiyDiSOA", "media_base_id": "kali-12" } },
                    { "item": { "_id" : "kali-hNvjiyDiSOA", "format": "PNG" } },
                    { "item": { "_id" : "kali-hNvjiyDiSOA", "upload_grid": "testing" } },
                    { "item": { "_id" : "kali-hNvjiyDiSOA",
                                     "img_derivatives" : { "image" : { "url" : "http://picor_url/" } } } }
                ] }
            <!-- /lackr:mustache:eval -->*/);
        String result = expand(page).trim();
        assertNearlyEquals(S(/*
                http://images.cdn.fotopedia.com/kali-hNvjiyDiSOA-image.jpg
                http://images.cdn.fotopedia.com/kali-12-image.jpg
                http://images.cdn.fotopedia.com/kali-hNvjiyDiSOA-image.png
                http://images.cdn.testing.ftnz.net/kali-hNvjiyDiSOA-image.jpg
                http://picor_url/
        */), result);
    }

    @Test
    public void testDerivativesEmptyContext() throws Exception {
        String page = S(/*
            <!-- lackr:mustache:template name="t" -->
                {{#item}}{{derivative cover kind="image"}} {{/item}}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="t" -->
                { "item": {} }
            <!-- /lackr:mustache:eval -->*/);
        String result = expand(page);
        assertNearlyEquals("", result);
    }

    @Test
    public void testInlineWrapperSubstitution() throws Exception {
        // https://github.com/fotonauts/picor/commit/4efa85aadd81ed2371f9866d214cad60066139bb
        String page = S(/*
            <!-- lackr:mustache:template name="t" -->
                {{blu}} {{glou}} {{glap}}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="t" -->
                { "blu": "bla", "blo":{ "$$inline_wrapper" : { "glou" : 42, "glap" : "haha" } } }
            <!-- /lackr:mustache:eval -->*/);
        String result = expand(page);
        assertNearlyEquals("bla 42 haha", result);
    }

    @Test
    public void testHumanizeInteger() throws Exception {
        // https://github.com/fotonauts/picor/commit/4efa85aadd81ed2371f9866d214cad60066139bb
        List<String> sb = new ArrayList<>();
        for (int i : new Integer[] { 12, 9999, 10000, 10001, 9999999, 10000000, 10000001 })
            sb.add("{ \"i\" : " + i + "}");
        String page = S(/*
            <!-- lackr:mustache:template name="t" -->
                {{#ints}}{{humanize_integer i}} {{/ints}}
            <!-- /lackr:mustache:template -->
            <!-- lackr:mustache:eval name="t" -->
                { "ints" : [ { "i": 12}, { "i": 9999 }, { "i": 10000}, {"i": 10001},
                             {"i": 9999999}, {"i": 10000000}, {"i": 10000001 } ] }
            <!-- /lackr:mustache:eval -->*/);
        String result = expand(page);
        assertNearlyEquals("12 9999 10k 10k 9999k 10M 10M", result);
    }

    @Test
    public void testLocalize() throws Exception {
        String page = S(/*
            <script type="vnd.fotonauts/picordata" id="leave-me-alone">
                { "root_id": 0, "objects": { "0" : [] } }
            </script><!-- END OF ARCHIVE -->
            <script type="vnd.fotonauts/picordata" id="translations/foo">
                { "root_id": 0, "objects": { "0" : { "Tap to set slide title" : "Tappe donc pour editer" } } }
            </script><!-- END OF ARCHIVE -->
            <script type="vnd.fotonauts/picordata" id="translations/bar">
                { "root_id": 12, "objects": { "12" : { "or go play in the mixer" : "ou vas jouer dans le mixer" } } }
            </script><!-- END OF ARCHIVE -->
            <!-- lackr:mustache:template name="tmpl" -->
                {{localize "Tap to set slide title"}} {{localize "or go play in the mixer"}}
            <!-- /lackr:mustache:template -->
            <script type="vnd.fotonauts/picordata" id="translations/illformed">
                { "root_id": 0, "objects": { "0" : [] } }
            </script><!-- END OF ARCHIVE -->
            <!-- lackr:mustache:eval name="tmpl" -->
                {}
            <!-- /lackr:mustache:eval -->
            */);
        String result = expand(page);
        assertContains(result.trim(), "Tappe donc pour editer ou vas jouer dans le mixer");
    }

    @Test
    public void testSubviewWithInlineTemplate() throws Exception {
        String result = expand(S(/*
                <!-- lackr:mustache:template name="tmpl" -->
                    <div class="toplevel">
                        {{tag_subview top.subviews_by_id.sv12}}
                    </div>
                <!-- /lackr:mustache:template -->

                <!-- lackr:mustache:eval name="tmpl" -->
                    { "top" : {
                        "subviews_by_id" : {
                            "sv12" : {
                                "wrapped_mustache_template" : "name:{{{view_model.name}}}",
                                "view_model" : { "name" : "yopla" }
                            }
                        }
                    } }
                <!-- /lackr:mustache:eval -->
        */));
        assertContains(result.trim(), "name:yopla");
    }

    @Test
    public void testSubviewRecursive() throws Exception {
        String result = expand(S(/*
                <!-- lackr:mustache:template name="tmpl" -->
                    <div class="toplevel">
                        {{tag_subview top.subviews_by_id.sv12}}
                    </div>
                <!-- /lackr:mustache:template -->

                <!-- lackr:mustache:eval name="tmpl" -->
                    { "top" : {
                        "subviews_by_id" : {
                            "sv12" : {
                                "wrapped_mustache_template" : "name:{{{view_model.name}}} {{tag_subview sv42}}",
                                "view_model" : { "name" : "yopla" },
                                "sv42": {
                                    "wrapped_mustache_template" : "name2:{{{view_model.value}}}",
                                    "view_model" : { "value" : "yop yop yop" }
                                }
                            }
                        }
                    } }
                <!-- /lackr:mustache:eval -->
        */));
        assertContains(result.trim(), "name:yopla name2:yop yop yop");
    }

    @Test
    public void testSubviewRecursiveWithPartial() throws Exception {
        String result = expand(S(/*
                <!-- lackr:mustache:template name="tmpl" -->
                    <div class="toplevel">
                        {{tag_subview top.subviews_by_id.sv12}}
                    </div>
                <!-- /lackr:mustache:template -->

                <!-- lackr:mustache:eval name="tmpl" -->
                    { "top" : {
                        "subviews_by_id" : {
                            "sv12" : {
                                "wrapped_mustache_template" : "name:{{{view_model.name}}} {{tag_subview sv42}}",
                                "view_model" : { "name" : "yopla" },
                                "sv42": {
                                    "view_model" : { "value" : "yop yop yop" },
                                    "wrapped_mustache_template" : "name2:{{>pp1}}",
                                    "mustache_partials": {
                                          "pp1": {
                                              "mustache": "PP1 partial: {{{view_model.value}}}"
                                          }
                                    }
                                }
                            }
                        }
                    } }
                <!-- /lackr:mustache:eval -->
        */));
        assertContains(result.trim(), "name:yopla name2:PP1 partial: yop yop yop");
    }

    @Test
    public void testIdTopLevelSubviewWithDash() throws Exception {
        String result = expand(S(/*
                <!-- lackr:mustache:template name="tmpl" -->
                    <div class="toplevel">
                        {{tag_subview top.subviews_by_id.sv12-}}
                    </div>
                <!-- /lackr:mustache:template -->

                <!-- lackr:mustache:eval name="tmpl" -->
                    { "top" : {
                        "subviews_by_id" : {
                            "sv12-" : {
                                "wrapped_mustache_template" : "name:{{{view_model.name}}}",
                                "view_model" : { "name" : "yopla" }
                            }
                        }
                    } }
                <!-- /lackr:mustache:eval -->
        */), true);
        assertContains(result.trim(), "Exception");
    }
    
    @Test
    public void testIdNestedSubviewWithDash() throws Exception {
        String result = expand(S(/*
                <!-- lackr:mustache:template name="tmpl" -->
                    <div class="toplevel">
                        {{tag_subview top.subviews_by_id.sv12}}
                    </div>
                <!-- /lackr:mustache:template -->

                <!-- lackr:mustache:eval name="tmpl" -->
                    { "top" : {
                        "subviews_by_id" : {
                            "sv12" : {
                                "wrapped_mustache_template" : "name:{{{view_model.name}}} {{tag_subview sv42-}}",
                                "view_model" : { "name" : "yopla" },
                                "sv42-": {
                                    "wrapped_mustache_template" : "name2:{{{view_model.value}}}",
                                    "view_model" : { "value" : "yop yop yop" }
                                }
                            }
                        }
                    } }
                <!-- /lackr:mustache:eval -->
        */), true);
        assertContains(result.trim(), "found: '-'");
    }

    @Test
    public void testIdTopLevelSubviewNotFound() throws Exception {
        String result = expand(S(/*
                <!-- lackr:mustache:template name="tmpl" -->
                    <div class="toplevel">
                        {{tag_subview top.subviews_by_id.sv13}}
                    </div>
                <!-- /lackr:mustache:template -->

                <!-- lackr:mustache:eval name="tmpl" -->
                    { "top" : {
                        "subviews_by_id" : {
                            "sv12" : {
                                "wrapped_mustache_template" : "name:{{{view_model.name}}}",
                                "view_model" : { "name" : "yopla" }
                            }
                        }
                    } }
                <!-- /lackr:mustache:eval -->
        */));
        assertTrue(result.trim().contains("toplevel"));
        assertFalse(result.trim().contains("name:"));
    }
    
    @Test
    public void testSubviewRecursiveNotFound() throws Exception {
        String result = expand(S(/*
                <!-- lackr:mustache:template name="tmpl" -->
                    <div class="toplevel">
                        {{tag_subview top.subviews_by_id.sv12}}
                    </div>
                <!-- /lackr:mustache:template -->

                <!-- lackr:mustache:eval name="tmpl" -->
                    { "top" : {
                        "subviews_by_id" : {
                            "sv12" : {
                                "wrapped_mustache_template" : "name:{{{view_model.name}}} {{tag_subview sv43}}",
                                "view_model" : { "name" : "yopla" },
                                "sv42": {
                                    "wrapped_mustache_template" : "name2:{{{view_model.value}}}",
                                    "view_model" : { "value" : "yop yop yop" }
                                }
                            }
                        }
                    } }
                <!-- /lackr:mustache:eval -->
        */));
        assertTrue(result.trim().contains("toplevel"));
        assertTrue(result.trim().contains("name:"));
        assertFalse(result.trim().contains("name2:"));
    }

    // SYNC_WITH_PICOR
    @Test
    public void testDomCompatibleId() throws Exception {
        String page = S(/*
                <!-- lackr:mustache:template name="t" -->
                    {{#strings}}
                        {{{s}}} -> {{dom_compatible_id s}}
                    {{/strings}}
                <!-- /lackr:mustache:template -->"
                <!-- lackr:mustache:eval name="t" -->
                    { "strings": [
                        { "s": "blah" },
                        { "s": "12" },
                        { "s": " !:?" },
                        { "s": ".#()" },
                        { "s": "/'&-_" }
                    ] }
                <!-- /lackr:mustache:eval -->*/);
            String result = expand(page).trim();
            assertContains(result, "blah -> _blah");
            assertContains(result, "12 -> _12");
            assertContains(result, " !:? -> __spc__bang__colon__qmark_");
            assertContains(result, ".#() -> __dot__hash__lpar__rpar_");
            assertContains(result, "/'&-_ -> __slash__q__amp__dash__under_" );
    }

    @Test
    public void testIsHelper() throws Exception {
        String page = S(/*
                <!-- lackr:mustache:template name="t" -->
                string1 -> {{#is string1 "pouet"}}true{{/is}}
                string2 -> {{#is string2 "gla"}}true{{else}}false{{/is}}
                int1 -> {{#is int1 1}}true{{else}}false{{/is}}
                int2 -> {{#is int2 1}}true{{else}}false{{/is}}
                bogus param -> {{#is int1}}true{{else}}false{{/is}}
                bogus context -> {{#is}}true{{else}}false{{/is}}
            <!-- /lackr:mustache:template -->"
            <!-- lackr:mustache:eval name="t" -->
                { "string1": "pouet",
                  "string2": "glo",
                  "int1" : 1,
                  "int2" : 2
                }
            <!-- /lackr:mustache:eval -->*/);
        String result = expand(page).trim();
        assertContains(result, "string1 -> true");
        assertContains(result, "string2 -> false");
        assertContains(result, "int1 -> true");
        assertContains(result, "int2 -> false");
        assertContains(result, "int2 -> false");
        assertContains(result, "bogus param -> false");
        assertContains(result, "bogus context -> false");
    }
}
