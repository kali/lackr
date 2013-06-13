package com.fotonauts.lackr.mustache;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

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
            data.put("_ftn_inline_images", request.getFrontendRequest().getUserAgent().supportsInlineImages());
            data.put("_ftn_locale", request.getFrontendRequest().getPreferredLocale());
            inlineWrapperJsonEvaluation(data);

            MustacheContext context = request.getFrontendRequest().getMustacheContext();
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
