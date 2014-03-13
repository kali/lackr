package com.fotonauts.lackr.interpolr.handlebars;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.interpolr.Document;
import com.fotonauts.lackr.interpolr.InterpolrContext;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.HandlebarsException;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.StringTemplateSource;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.github.jknack.handlebars.io.TemplateSource;

public class HandlebarsContext {

    static Logger log = LoggerFactory.getLogger(HandlebarsContext.class);

    private Handlebars handlebars;
    private Map<String, Document> registeredTemplatesDocument;
    private Map<String, Template> compiledTemplates;

    private InterpolrContext interpolrContext;

    private HandlebarsPlugin plugin;

    public InterpolrContext getInterpolrContext() {
        return interpolrContext;
    }

    public HandlebarsContext(HandlebarsPlugin plugin, InterpolrContext interpolrContext) {
        this.plugin = plugin;
        this.interpolrContext = interpolrContext;
        registeredTemplatesDocument = Collections.synchronizedMap(new HashMap<String, Document>());
        compiledTemplates = new HashMap<String, Template>();
        TemplateLoader loader = new TemplateLoader() {

            public TemplateSource sourceAt(final String uri) throws IOException {
                return new StringTemplateSource(uri, getExpandedTemplate(uri));
            }

            public String resolve(String uri) {
                return uri;
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
    }

    public void checkAndCompileAll() {
        log.debug("checkAndCompileAll");
        for (Entry<String, Document> registered : registeredTemplatesDocument.entrySet()) {
            registered.getValue().check();
            String expanded = getExpandedTemplate(registered.getKey());
            try {
                compiledTemplates.put(registered.getKey(), handlebars.compile(registered.getKey()));
            } catch (HandlebarsException e) {
                StringBuilder builder = new StringBuilder();
                builder.append("Handlebars: IOException\n");
                builder.append(e.getMessage() + "\n");
                builder.append("template name: " + registered.getKey() + "\n");
                String lines[] = expanded.split("\n");
                for (int i = 0; i < lines.length; i++)
                    builder.append(String.format("% 3d %s\n", i + 1, lines[i]));
                builder.append("\n");
                interpolrContext.addError(new LackrPresentableError(builder.toString()));
            } catch (IOException e) {
                StringBuilder builder = new StringBuilder();
                builder.append("Handlebars: IOException\n");
                builder.append(e.getMessage() + "\n");
                builder.append("template name: " + registered.getKey() + "\n");
                String lines[] = expanded.split("\n");
                for (int i = 0; i < lines.length; i++)
                    builder.append(String.format("% 3d %s\n", i + 1, lines[i]));
                builder.append("\n");
                interpolrContext.addError(new LackrPresentableError(builder.toString()));
            }

        }
    }

    public void registerTemplate(String name, Document template) {
        if (log.isDebugEnabled())
            log.debug("registerTemplate({}, {})", name, template.toDebugString());
        registeredTemplatesDocument.put(name, template);
    }

    public Template get(String templateName) {
        Template t = compiledTemplates.get(templateName);
        if (log.isDebugEnabled() && t != null) {
            String templateText = t.text().trim();
            if (templateText.length() > 30)
                t.text().trim().substring(0, 30);
            log.debug("get(\"{}\") = \"{}[...]\"", templateName, StringEscapeUtils.escapeJava(templateText));
        }
        if (t == null)
            log.warn("Template {} not found.", templateName);
        return t;
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
        Document doc = registeredTemplatesDocument.get(name);
        log.debug("getTemplate({}) = {}", name, doc);
        return doc;
    }

    public String[] getAllNames() {
        return (String[]) registeredTemplatesDocument.keySet().toArray(new String[0]);
    }

    public Handlebars getHandlebars() {
        return handlebars;
    }

    public String eval(Template template, Object data) throws IOException {
        Context context = plugin.makeHbsContext(this, data);
        String result = template.apply(context);
        return result;
    }

    public HandlebarsPlugin getPlugin() {
        return plugin;
    }

}
