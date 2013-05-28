package com.fotonauts.lackr.mustache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.fotonauts.lackr.BackendRequest;
import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.ConstantChunk;
import com.fotonauts.lackr.interpolr.Document;
import com.github.jknack.handlebars.Template;

public class MustacheEvalChunk implements Chunk {

    private static Chunk EMPTY = new ConstantChunk(new byte[0]);

    private Document inner;
    private BackendRequest request;
    private Chunk result = EMPTY;
    private String name;

    public MustacheEvalChunk(String name, byte[] buffer, int start, int stop, BackendRequest request) {
        this.request = request;
        this.name = name;
        inner = request.getFrontendRequest().getService().getInterpolr().parse(buffer, start, stop, request);
    }

    public static void inlineWrapperJsonEvaluation(Object data) {
        if (data instanceof List<?>) {
            List<Serializable> dataAsList = (List<Serializable>) data;
            for (Serializable s : dataAsList)
                inlineWrapperJsonEvaluation(s);

        } else if (data instanceof Map<?, ?>) {
            Map<String, Serializable> dataAsMap = (Map<String, Serializable>) data;
            List<String> keysToRemove = new LinkedList<>();
            Map<String, Serializable> stuffToInline = new HashMap<>();
            for (Entry<String, Serializable> pair : dataAsMap.entrySet()) {
                if (pair.getValue() instanceof Map<?, ?>) {
                    Map<String, Serializable> valueAsMap = (Map<String, Serializable>) pair.getValue();
                    if (valueAsMap.size() == 1 && valueAsMap.containsKey("$$inline_wrapper")) {
                        if (valueAsMap.get("$$inline_wrapper") instanceof Map<?, ?>) {
                            stuffToInline
                                    .putAll((Map<? extends String, ? extends Serializable>) valueAsMap.get("$$inline_wrapper"));
                            keysToRemove.add(pair.getKey());
                        }
                    }
                }
            }
            for (String k : keysToRemove)
                dataAsMap.remove(k);
            dataAsMap.putAll(stuffToInline);
            for (Serializable value : dataAsMap.values())
                inlineWrapperJsonEvaluation(value);
        }
    }

    @Override
    public void check() {
        inner.check();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectMapper mapper = request.getFrontendRequest().getService().getJacksonObjectMapper();
        @SuppressWarnings("rawtypes")
        Map data = null;
        try {
            inner.writeTo(baos);
            data = mapper.readValue(baos.toByteArray(), Map.class);
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
                String[] lines = baos.toString().split("\n");
                for (int i = 0; i < lines.length; i++) {
                    builder.append(String.format("% 3d %s\n", i + 1, lines[i]));
                }
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
        } catch (JsonParseException e) {
            StringBuilder builder = new StringBuilder();
            builder.append("JsonParseException\n");
            builder.append("url: " + request.getQuery() + "\n");
            builder.append(e.getMessage() + "\n");
            builder.append("template name: " + name + "\n");
            String lines[] = baos.toString().split("\n");
            for (int i = 0; i < lines.length; i++) {
                builder.append(String.format("% 3d %s\n", i + 1, lines[i]));
                if (i + 1 == e.getLocation().getLineNr()) {
                    builder.append("    ");
                    for (int j = 0; j < e.getLocation().getColumnNr() - 2; j++)
                        builder.append("-");
                    builder.append("^\n");
                }
            }
            builder.append("\n");
            request.getFrontendRequest().addBackendExceptions(new LackrPresentableError(builder.toString()));
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
            lines = baos.toString().split("\n");
            for (int i = 0; i < lines.length; i++) {
                builder.append(String.format("% 3d %s\n", i + 1, lines[i]));
            }
            if (data != null) {
                builder.append("\nparsed data:\n");
                try {
                    mapper.defaultPrettyPrintingWriter().writeValue(baos, data);
                } catch (JsonGenerationException e1) {
                    e1.printStackTrace();
                } catch (JsonMappingException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
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
