package com.fotonauts.lackr.interpolr.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.interpolr.InterpolrContext;
import com.fotonauts.lackr.interpolr.rope.Chunk;

public class JsonParseUtils {

    static Logger log = LoggerFactory.getLogger(JsonParseUtils.class);

    private static ObjectMapper jacksonObjectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parse(Chunk inner, InterpolrContext context, String contextString) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Map<String, Object> data = null;
            inner.writeTo(baos);
            boolean haveSomething = false;
            int i = 0;
            byte[] byteArray = baos.toByteArray();
            while (!haveSomething && ++i < byteArray.length) {
                haveSomething = byteArray[i] != ' ' && byteArray[i] != '\n' && byteArray[i] != '\t' && byteArray[i] != '\r';
            }
            if (haveSomething) {
                data = jacksonObjectMapper.readValue(baos.toByteArray(), Map.class);
                return data;
            } else {
                return null;
            }
        } catch (JsonParseException e) {
            if (log.isErrorEnabled()) {
                log.error("Parsing json: [[{}]] ", new String(baos.toByteArray()));
                log.error(e.getMessage(), e);
            }
            StringBuilder builder = new StringBuilder();
            builder.append("While parsing json for " + context + ": JsonParseException\n");
            builder.append(e.getMessage() + "\n");
            builder.append(contentAsDebugString(inner, e.getLocation().getLineNr(), e.getLocation().getColumnNr()));
            builder.append("\n");
            context.addError(new LackrPresentableError(builder.toString()));
        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error("Parsing json: [[{}]] ", new String(baos.toByteArray()));
                log.error(e.getMessage(), e);
            }
            StringBuilder builder = new StringBuilder();
            builder.append("While parsing json for " + context + ": IOException\n");
            builder.append(e.getMessage() + "\n");
            context.addError(new LackrPresentableError(builder.toString()));
        }
        return null;
    }

    public static String contentAsDebugString(Chunk inner, int lineNumber, int columnNumber) {
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

    public static String prettyPrint(Map<String, Object> data) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            jacksonObjectMapper.writer().withDefaultPrettyPrinter().writeValue(baos, data);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return baos.toString();
    }

}
