    package com.fotonauts.lackr.interpolr.handlebars;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.ConstantChunk;
import com.fotonauts.lackr.interpolr.Document;
import com.fotonauts.lackr.interpolr.InterpolrScope;
import com.fotonauts.lackr.interpolr.JsonParseUtils;
import com.fotonauts.lackr.interpolr.Plugin;
import com.github.jknack.handlebars.Template;

public class HandlebarsEvalChunk implements Chunk {

    static Logger log = LoggerFactory.getLogger(HandlebarsEvalChunk.class);

    private static Chunk EMPTY = new ConstantChunk(new byte[0]);

    private Document inner;
    private InterpolrScope scope;
    private Chunk result = EMPTY;
    private String name;

    private Plugin plugin;
    
    public HandlebarsEvalChunk(Plugin plugin, String name, byte[] buffer, int start, int stop, InterpolrScope scope) {
        this.scope = scope;
        this.plugin = plugin;
        inner = scope.getInterpolr().parse(buffer, start, stop, scope);
        this.name = name;
    }

    @Override
    public void check() {
        inner.check();
        
        Map<String, Object> data = null;
        data = JsonParseUtils.parse(inner, scope.getInterpolrContext(), name);
        HandlebarsContext context = (HandlebarsContext) scope.getInterpolrContext().getPluginData(plugin);
        Template template = context.get(name);
        if (template == null) {
            StringBuilder builder = new StringBuilder();
            builder.append("Mustache template not found\n");
            builder.append("url: " + scope.toString() + "\n");
            builder.append("template name: " + name + "\n");
            builder.append("\nexpanded data:\n");
            builder.append(JsonParseUtils.contentAsDebugString(inner, -1, -1));
            builder.append("\n");
            builder.append("known templates: ");
            for (String name : context.getAllNames()) {
                builder.append(name);
                builder.append(" ");
            }
            builder.append("\n");
            scope.getInterpolrContext().addError(new LackrPresentableError(builder.toString()));
        } else
            try {
                result = new ConstantChunk(context.eval(template, data).getBytes("UTF-8"));
            } catch (Throwable e) {
                e.printStackTrace(System.err);
                StringBuilder builder = new StringBuilder();
                builder.append("MustacheException\n");
                builder.append("url: " + scope + "\n");
                builder.append(e.getMessage() + "\n");
                if (e.getCause() != null && e.getCause() != e)
                    builder.append("caused by: " + e.getCause().getMessage() + "\n");
                builder.append("template name: " + name + "\n");
                String[] lines;
                try {
                    lines = context.getExpandedTemplate(name).split("\n");
                    for (int i = 0; i < lines.length; i++) {
                        builder.append(String.format("% 3d %s\n", i + 1, lines[i]));
                    }
                } catch (Exception e2) {
                    /* very depressing to get here */
                }
                builder.append("\nexpanded data:\n");
                builder.append(JsonParseUtils.contentAsDebugString(inner, -1, -1));
                scope.getInterpolrContext().addError(new LackrPresentableError(builder.toString()));
            }
    }

    /*
    @SuppressWarnings("unchecked")
    private static void resolveArchiveReferences(Object data, final HandlebarsContext context) {
        new JsonWalker() {
            @Override
            public Object resolve(Object datum) {
                if (datum instanceof Map<?, ?>) {
                    Map<String, Object> datumAsMap = (Map<String, Object>) datum;
                    if (datumAsMap.containsKey("$$archive") && datumAsMap.containsKey("$$id")) {
                        Archive arch = context.getArchive((String) datumAsMap.get("$$archive"));
                        if(arch != null)
                            return arch.getObject((Integer) datumAsMap.get("$$id"));
                    }
                }
                return null;
            }
        }.walk(data);
    }
    */

    @Override
    public int length() {
        return result.length();
    }

    @Override
    public String toDebugString() {
        return "{{//" + name + "//}}";
    }

    @Override
    public void writeTo(OutputStream stream) throws IOException {
        result.writeTo(stream);
    }
}
