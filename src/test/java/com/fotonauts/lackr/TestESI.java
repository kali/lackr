package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.junit.Test;

public class TestESI extends BaseTestSubstitution {

    public TestESI() throws Exception {
        super();
    }

    public String quoteJson(String text) {
        return new String(org.codehaus.jackson.io.JsonStringEncoder.getInstance().quoteAsUTF8(text)).replaceAll("\\/", "\\\\/");
    }
    
    @Test
    public void testHtmlInHtml() throws Exception {
        String result = expand("before\n<!--# include virtual=\"/esi.html\" -->\nafter\n");
        assertEquals("before\n" + ESI_HTML + "\nafter\n", result);
    }

    @Test
    public void testJsInHtmlShouldCrash() throws Exception {
        System.err.println(expand("<!--# include virtual=\"/esi.json\" -->"));
        assertNull(expand("<!--# include virtual=\"/esi.json\" -->"));
    }

    @Test
    public void testHtmlInJs() throws Exception {
        String result = expand("before\n\"ssi:include:virtual:/esi.html\"\nafter\n");
        assertEquals("before\n\"" + quoteJson(ESI_HTML) + "\"\nafter\n", result);
    }

    @Test
    public void testJsInJs() throws Exception {
        String result = expand("before\n\"ssi:include:virtual:/esi.json\"\nafter\n");
        assertEquals("before\n" + ESI_JSON + "\nafter\n", result);
    }

    @Test
    public void testHtmlInMlJs() throws Exception {
        String result = expand("before\n<!--# include virtual=\\\"/esi.html\\\" -->\nafter\n");
        String json = quoteJson(ESI_HTML);
        assertEquals("before\n" + json + "\nafter\n", result);
    }

    @Test
    public void testJInMlJsShouldCrash() throws Exception {
        assertNull(expand("before\n<!--# include virtual=\\\"/esi.json\\\" -->\nafter\n"));
    }

    @Test
    public void testEscapedHtmlInMlJs() throws Exception {
        String result = expand("before\n\\u003C!--# include virtual=\\\"/esi.html\\\" --\\u003E\nafter\n");
        System.err.println(ESI_HTML);
        String json = quoteJson(ESI_HTML);
        System.err.println(json);
        System.err.println(result);
        assertEquals("before\n" + json + "\nafter\n", result);
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
        assertEquals("{ something_empty: \"" + ESI_TEXT.replace("\"", "\\\"").replace("/", "\\/").replace("\n", "\\n") + "\" }",
                result);
    }

    @Test
    public void testFemtorJSESI() throws Exception {
        String result = expand("{ something: \"ssi:include:femtor:/femtor\" }");
        assertEquals("{ something: \"Hi from dummy femtor\\n\" }", result);
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

    @Test
    public void testMethodsMainRequest() throws Exception {
        for (HttpMethod method : new HttpMethod[] { HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT }) {
            Request r = client.newRequest("http://localhost:" + lackrPort + "/method");
            r.method(method);
            ContentResponse e = r.send();
            assertEquals(method.asString(), e.getContentAsString());
        }
    }

    @Test
    public void testMethodSubRequest() throws Exception {
        for (HttpMethod method : new HttpMethod[] { HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT }) {
            Request r = client.newRequest("http://localhost:" + lackrPort + "/page.html");
            r.method(method);
            page.setLength(0);
            page.append("Main request does:" + method + "\n" + "ESI does:<!--# include virtual=\\\"/method\\\" -->");
            ContentResponse e = r.send();
            System.err.println(e.getContentAsString());
            assertContains(e.getContentAsString(), "ESI does:GET");
        }
    }

    @Test(timeout = 5000)
    public void testInvalidUrl() throws Exception {
        String result = expand(TextUtils.S(/*
{"status":"success","user":"{\n  \"root_id\": 1,\n  \"objects\": {\n    \"1\": {\n      \"$JSClass\": \"Picor.PCUser\",\n      \"_id\": \"octplane\",\n      \"_klass\": \"MUser\",\n      \"rb_model_presenter_name\": \"ModelPresenter::UserPresenter\",\n      \"url\": \"http://www.virtual.ftnz.net:2002/users/octplane\",\n      \"fullname\": \"Pierre Baillet\",\n      \"reputation\": {\n        \"points\": \"1 244\"\n      },\n      \"avatar\": {\n        \"$$id\": 2\n      },\n      \"has_avatar\": true,\n      \"location\": \"\",\n      \"reporter_drafts_count\": 2,\n      \"reporter_shared_drafts_count\": 0,\n      \"reporter_stories_count\": 2,\n      \"reporter_following_count\": 9,\n      \"reporter_followers_count\": 12,\n      \"reporter_reading_lists_count\": 3,\n      \"reporter_following_reading_list_count\": 0,\n      \"reporter_achievements_count\": 7,\n      \"reporter_unread_news_count\": 0,\n      \"reporter_news_web_notif_level\": 3,\n      \"reporter_have_unread_important_news\": false,\n      \"meFollowState\": \"ssi:include:virtual:/ios/users/octplane/meFollowState.json\",\n      \"info_hash\": {\n        \"logged_in\": true,\n        \"avatar_path\": \"http://www.fotopedia.com/items/octplane-rFjzTex2zTg/signed_media_url_for_infrabox/kind/fasquare/format/JPEG\",\n        \"reputation_str\": \"1 244\",\n        \"display_name\": \"Pierre Baillet\",\n        \"is_adminpedia\": false,\n        \"is_superuser\": true,\n        \"remaining_nominates\": 50,\n        \"remaining_score\": 200,\n        \"nominate_vote_quota\": 50,\n        \"score_vote_quota\": 200,\n        \"nb_votes_per_photo\": 1,\n        \"default_license\": {\n          \"url\": null,\n          \"long_text\": \"All rights reserved\"\n        },\n        \"flickr_info\": {\n          \"logged_in\": false,\n          \"auth_url\": \"http://www.virtual.ftnz.net:2002/linkflickraccount\"\n        },\n        \"can_post_comment_on_project\": true,\n        \"can_vote_photo\": true,\n        \"can_vote_photo_down\": true,\n        \"unread_notifications_nb\": 0\n      },\n      \"saved_conflict_information\": null,\n      \"born_on\": {\n        \"$DATE\": 347155200000\n      },\n      \"email\": \"pierre@baillet.name\",\n      \"firstname\": \"Pierre\",\n      \"lastname\": \"Baillet\",\n      \"website\": \"http://oct.zoy.org\",\n      \"logged_in\": true,\n      \"avatar_path\": \"http://www.fotopedia.com/items/octplane-rFjzTex2zTg/signed_media_url_for_infrabox/kind/fasquare/format/JPEG\",\n      \"reputation_str\": \"1 244\",\n      \"display_name\": \"Pierre Baillet\",\n      \"is_adminpedia\": false,\n      \"is_superuser\": true,\n      \"remaining_nominates\": 50,\n      \"remaining_score\": 200,\n      \"nominate_vote_quota\": 50,\n      \"score_vote_quota\": 200,\n      \"nb_votes_per_photo\": 1,\n      \"default_license\": {\n        \"url\": null,\n        \"long_text\": \"All rights reserved\"\n      },\n      \"flickr_info\": {\n        \"logged_in\": false,\n        \"auth_url\": \"http://www.virtual.ftnz.net:2002/linkflickraccount\"\n      },\n      \"can_post_comment_on_project\": true,\n      \"can_vote_photo\": true,\n      \"can_vote_photo_down\": true,\n      \"unread_notifications_nb\": 0\n    },\n    \"2\": {\n      \"$JSClass\": \"Picor.Picture\",\n      \"_id\": \"octplane-rFjzTex2zTg\",\n      \"_klass\": \"Picture\",\n      \"rb_model_presenter_name\": \"ModelPresenter::ItemPresenter\",\n      \"object_type\": \"Item\",\n      \"name\": \"DSCF5693\",\n      \"internal_author_id\": \"octplane\",\n      \"width\": 2136,\n      \"height\": 2848,\n      \"derivatives_ready\": true,\n      \"author_caption\": \"by <a href=\\\"http://www.virtual.ftnz.net:2002/users/octplane\\\" about=\\\"http://www.fotopedia.com/items/octplane-rFjzTex2zTg/signed_media_url_for_infrabox/kind/original/format/JPEG\\\" class=\\\"internal_author\\\" property=\\\"cc:attributionName\\\" rel=\\\"cc:attributionURL\\\">Pierre Baillet</a>\",\n      \"author_html\": \"<a href=\\\"http://www.virtual.ftnz.net:2002/users/octplane\\\" about=\\\"http://www.fotopedia.com/items/octplane-rFjzTex2zTg/signed_media_url_for_infrabox/kind/original/format/JPEG\\\" class=\\\"internal_author\\\" property=\\\"cc:attributionName\\\" rel=\\\"cc:attributionURL\\\">Pierre Baillet</a><span class='user_reputation'>1 244</span>\",\n      \"base_title\": \"\",\n      \"license\": {\n        \"url\": null,\n        \"text\": \"All rights reserved\",\n        \"long_text\": \"All rights reserved\",\n        \"icon_letters\": \"\",\n        \"downloadable\": false\n      },\n      \"ios_applications\": [\n\n      ],\n      \"references\": {\n        \"fotopedia\": [\n\n        ]\n      },\n      \"img_derivatives\": {\n        \"crop_24x24\": {\n          \"url\": \"http://www.fotopedia.com/items/octplane-rFjzTex2zTg/signed_media_url_for_infrabox/kind/crop_24x24/format/JPEG\",\n          \"width\": 24,\n          \"height\": 24\n        },\n        \"fasquare\": {\n          \"url\": \"http://www.fotopedia.com/items/octplane-rFjzTex2zTg/signed_media_url_for_infrabox/kind/fasquare/format/JPEG\",\n          \"width\": 50,\n          \"height\": 50\n        },\n        \"square\": {\n          \"url\": \"http://www.fotopedia.com/items/octplane-rFjzTex2zTg/signed_media_url_for_infrabox/kind/square/format/JPEG\",\n          \"width\": 60,\n          \"height\": 60\n        },\n        \"crop_125x125\": {\n          \"url\": \"http://www.fotopedia.com/items/octplane-rFjzTex2zTg/signed_media_url_for_infrabox/kind/crop_125x125/format/JPEG\",\n          \"width\": 125,\n          \"height\": 125\n        },\n        \"small\": {\n          \"url\": \"http://www.fotopedia.com/items/octplane-rFjzTex2zTg/signed_media_url_for_infrabox/kind/small/format/JPEG\",\n          \"width\": 128,\n          \"height\": 171\n        },\n        \"max_480\": {\n          \"url\": \"http://www.fotopedia.com/items/octplane-rFjzTex2zTg/signed_media_url_for_infrabox/kind/max_480/format/JPEG\",\n          \"width\": 360,\n          \"height\": 480\n        },\n        \"image\": {\n          \"url\": \"http://www.fotopedia.com/items/octplane-rFjzTex2zTg/signed_media_url_for_infrabox/kind/image/format/JPEG\",\n          \"width\": 400,\n          \"height\": 533\n        },\n        \"max_1024\": {\n          \"url\": \"http://www.fotopedia.com/items/octplane-rFjzTex2zTg/signed_media_url_for_infrabox/kind/max_1024/format/JPEG\",\n          \"width\": 768,\n          \"height\": 1024\n        },\n        \"ifill_1024x768\": {\n          \"url\": \"http://www.fotopedia.com/items/octplane-rFjzTex2zTg/signed_media_url_for_infrabox/kind/ifill_1024x768/format/JPEG\",\n          \"width\": 768,\n          \"height\": 1024\n        },\n        \"hd\": {\n          \"url\": \"http://www.fotopedia.com/items/octplane-rFjzTex2zTg/signed_media_url_for_infrabox/kind/hd/format/JPEG\",\n          \"width\": 810,\n          \"height\": 1080\n        },\n        \"ifill_2048\": {\n          \"url\": \"http://www.fotopedia.com/items/octplane-rFjzTex2zTg/signed_media_url_for_infrabox/kind/ifill_2048/format/JPEG\",\n          \"width\": 1536,\n          \"height\": 2048\n        },\n        \"max_2560\": {\n          \"url\": \"http://www.fotopedia.com/items/octplane-rFjzTex2zTg/signed_media_url_for_infrabox/kind/max_2560/format/JPEG\",\n          \"width\": 1200,\n          \"height\": 1600\n        }\n      }\n    }\n  }\n}"}
                */));
        System.err.println(result);
    }
}
