    package com.fotonauts.lackr.mustache;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.backend.LackrBackendRequest;
import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.ConstantChunk;
import com.github.jknack.handlebars.Template;

public class MustacheEvalChunk extends ParsedJsonChunk implements Chunk {

    private static Chunk EMPTY = new ConstantChunk(new byte[0]);

    Chunk result = EMPTY;
    String name;

    public MustacheEvalChunk(String name, byte[] buffer, int start, int stop, LackrBackendRequest request) {
        super(buffer, start, stop, request);
        this.name = name;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void check() {
        inner.check();
        Map<String, Object> data = null;
        data = parse(inner, request.getFrontendRequest(), name);
        MustacheContext context = request.getFrontendRequest().getMustacheContext();
        inlineWrapperJsonEvaluation(data);

        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("root", data);
        resolveArchiveReferences(wrapper, context);
        data = (Map<String, Object>) wrapper.get("root");

        Template template = context.get(name);
        if (template == null) {
            StringBuilder builder = new StringBuilder();
            builder.append("Mustache template not found\n");
            builder.append("url: " + request.getQuery() + "\n");
            builder.append("template name: " + name + "\n");
            builder.append("\nexpanded data:\n");
            builder.append(contentAsDebugString(inner, -1, -1));
            builder.append("\n");
            builder.append("known templates: ");
            for (String name : context.getAllNames()) {
                builder.append(name);
                builder.append(" ");
            }
            builder.append("\n");
            request.getFrontendRequest().addBackendExceptions(new LackrPresentableError(builder.toString()));
        } else
            try {
                result = new ConstantChunk(context.eval(template, data).getBytes("UTF-8"));
            } catch (Throwable e) {
                StringBuilder builder = new StringBuilder();
                builder.append("MustacheException\n");
                builder.append("url: " + request.getQuery() + "\n");
                builder.append(e.getMessage() + "\n");
                if (e.getCause() != null && e.getCause() != e)
                    builder.append("caused by: " + e.getCause().getMessage() + "\n");
                builder.append("template name: " + name + "\n");
                String[] lines;
                try {
                    lines = request.getFrontendRequest().getMustacheContext().getExpandedTemplate(name).split("\n");
                    for (int i = 0; i < lines.length; i++) {
                        builder.append(String.format("% 3d %s\n", i + 1, lines[i]));
                    }
                } catch (Exception e2) {
                    /* very depressing to get here */
                }
                builder.append("\nexpanded data:\n");
                builder.append(contentAsDebugString(inner, -1, -1));
                request.getFrontendRequest().addBackendExceptions(new LackrPresentableError(builder.toString()));
            }
    }

    @SuppressWarnings("unchecked")
    private static void resolveArchiveReferences(Object data, final MustacheContext context) {
        new ReferenceResolverWalker() {
            @Override
            public Map<String, Object> resolve(Object datum) {
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
