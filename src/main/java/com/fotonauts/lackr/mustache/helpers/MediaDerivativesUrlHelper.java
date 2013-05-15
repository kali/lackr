package com.fotonauts.lackr.mustache.helpers;

import java.io.IOException;
import java.util.Map;

import com.fotonauts.lackr.Service;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

public class MediaDerivativesUrlHelper implements Helper<Object> {

    private Service service;

    public MediaDerivativesUrlHelper(Service service) {
        this.service = service;
    }

    @Override
    public CharSequence apply(Object pictureAsObject, Options options) throws IOException {
        if(pictureAsObject == null)
            return "";
        @SuppressWarnings("unchecked")
        Map<String, Object> picture = (Map<String, Object>) pictureAsObject;
        String kind = (String) options.hash("kind");

        @SuppressWarnings("unchecked")
        Map<String, Object> derivatives = (Map<String, Object>) picture.get("img_derivatives");
        if(derivatives != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> derivative = (Map<String, Object>) derivatives.get(kind);
            if(derivative != null) {
                String url = (String) derivative.get("url");
                if(url != null)
                    return url;
            }
        }
        
        String grid = (String) picture.get("upload_grid");
        if (grid == null)
            grid = service.getGrid();
        if (grid == null || grid.equals(""))
            grid = "prod";

        String cdnHostname;
        switch (grid) {
        case "prod":
            cdnHostname = "images.cdn.fotopedia.com";
            break;
        case "staging":
            cdnHostname = "images.cdn.fotopedia.com";
            break;
        case "testing":
            cdnHostname = "images.cdn.testing.ftnz.net";
            break;
        case "infrabox":
            cdnHostname = "www.virtual.ftnz.net/data_upload";
            break;
        default:
            cdnHostname = "images.cdn.fotopedia.com";
            break;
        }

        String mediaBaseId = (String) picture.get("media_base_id");
        if (mediaBaseId == null)
            mediaBaseId = (String) picture.get("_id");

        String format = (String) picture.get("format");
        if (format == null)
            format = "JPEG";
        String ext;
        switch (format) {
        case "JPEG":
            ext = "jpg";
            break;
        case "PNG":
            ext = "png";
            break;
        case "GIF":
            ext = "gif";
            break;
        default:
            ext = "jpg";
            break;
        }

        return String.format("http://%s/%s-%s.%s", cdnHostname, mediaBaseId, kind, ext);
    }
}
