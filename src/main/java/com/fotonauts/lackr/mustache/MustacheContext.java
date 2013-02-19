package com.fotonauts.lackr.mustache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.interpolr.Document;
import com.fotonauts.lackr.mustache.helpers.ReverseEachHelper;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.HandlebarsException;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.TemplateLoader;

public class MustacheContext {

    private Handlebars handlebars;
    private Map<String, Document> registeredTemplatesDocument;
    private Map<String, Template> compiledTemplates;

    public MustacheContext() {
        registeredTemplatesDocument = Collections.synchronizedMap(new HashMap<String, Document>());
        compiledTemplates = Collections.synchronizedMap(new HashMap<String, Template>());
        TemplateLoader loader = new TemplateLoader() {

            @Override
            public String resolve(String uri) {
                return uri;
            }
            
            @Override
            protected Reader read(String location) throws IOException {
                return new StringReader(getExpandedTemplate(location));
            }
        };
        handlebars = new Handlebars(loader);
        handlebars.registerHelper(ReverseEachHelper.NAME, ReverseEachHelper.INSTANCE);
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

            /*
             * } catch (MustacheParseException e) { StringBuilder builder = new
             * StringBuilder(); builder.append("MustacheParseException\n");
             * builder.append(e.getMessage() + "\n");
             * builder.append("template name: " + registered.getKey() + "\n");
             * String lines[] = expanded.split("\n"); for (int i = 0; i <
             * lines.length; i++) builder.append(String.format("% 3d %s\n", i +
             * 1, lines[i])); builder.append("\n"); backendExceptions.add(new
             * LackrPresentableError(builder.toString())); }
             */
        }
    }

    /*
     * private TemplateLoader getLoader() { return new TemplateLoader() {
     * 
     * @Override public Reader getTemplate(String name) throws Exception {
     * return new StringReader(getExpandedTemplate(name)); } }; }
     */
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
}
