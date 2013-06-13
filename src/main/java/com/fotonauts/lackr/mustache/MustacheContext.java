package com.fotonauts.lackr.mustache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fotonauts.lackr.LackrFrontendRequest;
import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.interpolr.Document;
import com.fotonauts.lackr.mustache.helpers.DateTimeFormatterHelpers;
import com.fotonauts.lackr.mustache.helpers.MediaDerivativesUrlHelper;
import com.fotonauts.lackr.mustache.helpers.MiscelaneousHelpers;
import com.fotonauts.lackr.mustache.helpers.ReverseEachHelper;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.HandlebarsException;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.StringTemplateSource;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.github.jknack.handlebars.io.TemplateSource;

public class MustacheContext {

    private Handlebars handlebars;
    private Map<String, Document> registeredTemplatesDocument;
    private Map<String, Template> compiledTemplates;
    private Map<String, Map<String,Object>> archives;

    public MustacheContext(LackrFrontendRequest lackrFrontendRequest) {
        registeredTemplatesDocument = Collections.synchronizedMap(new HashMap<String, Document>());
        compiledTemplates = Collections.synchronizedMap(new HashMap<String, Template>());
        TemplateLoader loader = new TemplateLoader() {

            public TemplateSource sourceAt(final URI uri) throws IOException {
                return new StringTemplateSource(uri.toString(), getExpandedTemplate(uri.toString()));
            }

            public String resolve(URI uri) {
                return uri.toString();
            }

            @Override
            public String getPrefix() {
                return "";
            }

            @Override
            public String getSuffix() {
                return "";
            }
        };
        handlebars = new Handlebars(loader);
        handlebars.registerHelper(ReverseEachHelper.NAME, ReverseEachHelper.INSTANCE);
        handlebars.registerHelpers(new DateTimeFormatterHelpers());
        handlebars.registerHelpers(new MediaDerivativesUrlHelper());
        handlebars.registerHelpers(new MiscelaneousHelpers());
    }

    public void checkAndCompileAll(List<LackrPresentableError> backendExceptions) {
        for (Entry<String, Document> registered : registeredTemplatesDocument.entrySet()) {
            registered.getValue().check();
            String expanded = getExpandedTemplate(registered.getKey());
            try {
                compiledTemplates.put(registered.getKey(), handlebars.compile(expanded));
            } catch (HandlebarsException e) {
                StringBuilder builder = new StringBuilder();
                builder.append("Handlebars: IOException\n");
                builder.append(e.getMessage() + "\n");
                builder.append("template name: " + registered.getKey() + "\n");
                String lines[] = expanded.split("\n");
                for (int i = 0; i < lines.length; i++)
                    builder.append(String.format("% 3d %s\n", i + 1, lines[i]));
                builder.append("\n");
                backendExceptions.add(new LackrPresentableError(builder.toString()));
            } catch (IOException e) {
                StringBuilder builder = new StringBuilder();
                builder.append("Handlebars: IOException\n");
                builder.append(e.getMessage() + "\n");
                builder.append("template name: " + registered.getKey() + "\n");
                String lines[] = expanded.split("\n");
                for (int i = 0; i < lines.length; i++)
                    builder.append(String.format("% 3d %s\n", i + 1, lines[i]));
                builder.append("\n");
                backendExceptions.add(new LackrPresentableError(builder.toString()));
            }

        }
    }

    public void registerTemplate(String name, Document template) {
        registeredTemplatesDocument.put(name, template);
    }

    public Template get(String templateName) {
        return compiledTemplates.get(templateName);
    }

    public String getExpandedTemplate(String name) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            registeredTemplatesDocument.get(name).writeTo(baos);
            return baos.toString("UTF-8");
        } catch (IOException e) {
            return null;
        }
    }

    public Document getTemplate(String name) {
        return registeredTemplatesDocument.get(name);
    }

    public String[] getAllNames() {
        return (String[]) registeredTemplatesDocument.keySet().toArray(new String[0]);
    }

    public Map<String,Map<String,Object>> getArchives() {
        if(archives == null)
            archives = new HashMap<String, Map<String, Object>>();
        return archives;
    }
}
