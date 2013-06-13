package com.fotonauts.lackr.mustache;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fotonauts.lackr.BackendRequest;
import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.ConstantChunk;
import com.github.jknack.handlebars.Template;

public class MustacheEvalChunk extends ParsedJsonChunk implements Chunk {

    private static Chunk EMPTY = new ConstantChunk(new byte[0]);

    private Chunk result = EMPTY;
    private String name;

    public MustacheEvalChunk(String name, byte[] buffer, int start, int stop, BackendRequest request) {
        super(buffer, start, stop, request);
        this.name = name;
    }

    @Override
    public void check() {
        inner.check();
        Map<String, Object> data = null;
        try {
            data = parse();
            MustacheContext context = request.getFrontendRequest().getMustacheContext();
            inlineWrapperJsonEvaluation(data);
            
            Map<String,Object> wrapper = new HashMap<>();
            wrapper.put("root", data);
            resolveArchiveReferences(wrapper, context.getArchives());
            data = (Map<String, Object>) wrapper.get("root");
            
            data.put("_ftn_inline_images", request.getFrontendRequest().getUserAgent().supportsInlineImages());
            data.put("_ftn_locale", request.getFrontendRequest().getPreferredLocale());
            Template template = context.get(name);
            if (template == null) {
                StringBuilder builder = new StringBuilder();
                builder.append("Mustache template not found\n");
                builder.append("url: " + request.getQuery() + "\n");
                builder.append("template name: " + name + "\n");
                builder.append("\nexpanded data:\n");
                builder.append(contentAsDebugString(-1, -1));
                builder.append("\n");
                builder.append("known templates: ");
                for (String name : context.getAllNames()) {
                    builder.append(name);
                    builder.append(" ");
                }
                builder.append("\n");
                request.getFrontendRequest().addBackendExceptions(new LackrPresentableError(builder.toString()));
            } else
                result = new ConstantChunk(template.apply(data).getBytes("UTF-8"));
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
            builder.append(contentAsDebugString(-1, -1));
            if (data != null) {
                builder.append("\nparsed data:\n");
                builder.append(prettyPrint(data));
            }
            builder.append("\n");
            request.getFrontendRequest().addBackendExceptions(new LackrPresentableError(builder.toString()));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asAReference(Object data, Map<String, Archive> archives) {
        if (data instanceof Map<?, ?>) {
            Map<String, Object> dataAsMap = (Map<String, Object>) data;
            if (dataAsMap.containsKey("$$archive") && dataAsMap.containsKey("$$id")) {
                Archive arch = (Archive) archives.get(dataAsMap.get("$$archive"));
                return arch.getObject((Integer) dataAsMap.get("$$id"));
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static void resolveArchiveReferences(Object data, Map<String, Archive> archives) {
        if (data instanceof List<?>) {
            List<Object> dataAsList = (List<Object>) data;
            boolean shouldChange = false;
            for (Object s : dataAsList) {
                resolveArchiveReferences(s, archives);
                shouldChange = shouldChange || asAReference(s, archives) != null;
            }
            if (shouldChange) {
                for (int i = 0; i < dataAsList.size(); i++) {
                    Object resolved = asAReference(dataAsList.get(i), archives);
                    if (resolved != null)
                        dataAsList.set(i, resolved);
                }
            }

        } else if (data instanceof Map<?, ?>) {
            Map<String, Object> dataAsMap = (Map<String, Object>) data;
            Map<String, Object> changes = new HashMap<>();
            for (Entry<String, Object> e : dataAsMap.entrySet()) {
                resolveArchiveReferences(e.getValue(), archives);
                Object resolved = asAReference(e.getValue(), archives);
                if (resolved != null)
                    changes.put(e.getKey(), resolved);
            }
            dataAsMap.putAll(changes);
        }
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
