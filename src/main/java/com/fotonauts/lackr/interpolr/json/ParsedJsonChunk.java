package com.fotonauts.lackr.interpolr.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.Document;
import com.fotonauts.lackr.interpolr.InterpolrScope;

public abstract class ParsedJsonChunk  {

    protected Document inner;
    protected InterpolrScope scope;

    @SuppressWarnings("unchecked")
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

    public ParsedJsonChunk(byte[] buffer, int start, int stop, InterpolrScope scope) {
        super();
        this.scope = scope;
        inner = scope.getInterpolr().parse(buffer, start, stop, scope);
    }

    protected static String contentAsDebugString(Chunk inner, int lineNumber, int columnNumber) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            inner.writeTo(baos);
        } catch (IOException e) {
            /* ignore this, we're already debugging anyway */
        }
        StringBuilder builder = new StringBuilder();
        String lines[] = baos.toString().split("\n");
        for (int i = 0; i < lines.length; i++) {
            builder.append(String.format("% 3d %s\n", i + 1, lines[i]));
            if (i + 1 == lineNumber) {
                builder.append("    ");
                for (int j = 0; j < columnNumber - 2; j++)
                    builder.append("-");
                builder.append("^\n");
            }
        }
        return builder.toString();
    }

    /*
    public String prettyPrint(Map<String, Object> data) {
        return prettyPrint(data, scope);
    }
    public static String prettyPrint(Map<String, Object> data, InterpolrScope scope) {
        ObjectMapper mapper = null;
        if (scope != null)
            mapper = scope.getInterpolr().getJacksonObjectMapper();
        else
            mapper = new ObjectMapper();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            mapper.defaultPrettyPrintingWriter().writeValue(baos, data);
        } catch (JsonGenerationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JsonMappingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return baos.toString();
    }

    @SuppressWarnings("unchecked")
    protected static Map<String, Object> parse(Chunk inner, InterpolrContext context, String contextString) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectMapper mapper = context.getInterpolr().getJacksonObjectMapper();
        try {
            Map<String, Object> data = null;
            inner.writeTo(baos);
            boolean haveSomething = false;
            int i = 0;
            byte[] byteArray = baos.toByteArray();
            while(!haveSomething && ++i < byteArray.length) {
                haveSomething = byteArray[i] != ' ' && byteArray[i] != '\n' &&  byteArray[i] != '\t' && byteArray[i] != '\r';
            }
            if(haveSomething) {
                data = mapper.readValue(baos.toByteArray(), Map.class);
                return data;
            } else {
                return null;
            }
        } catch (JsonParseException e) {
            StringBuilder builder = new StringBuilder();
            builder.append("While parsing json for " + context + ": JsonParseException\n");
            builder.append(e.getMessage() + "\n");
            builder.append(contentAsDebugString(inner, e.getLocation().getLineNr(), e.getLocation().getColumnNr()));
            builder.append("\n");
            context.addError(new LackrPresentableError(builder.toString()));
        } catch (IOException e) {
            StringBuilder builder = new StringBuilder();
            builder.append("While parsing json for " + context + ": IOException\n");
            builder.append(e.getMessage() + "\n");
            context.addError(new LackrPresentableError(builder.toString()));
        }
        return null;
    }
    */
}