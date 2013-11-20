package com.fotonauts.lackr.interpolr;

import java.util.Arrays;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.jetty.util.component.AbstractLifeCycle;

import com.fotonauts.lackr.MimeType;

public class Interpolr extends AbstractLifeCycle {

    private ObjectMapper jacksonObjectMapper;

    private List<Plugin> plugins;

    @Override
    protected void doStart() throws Exception {
        jacksonObjectMapper = new ObjectMapper();
    }

    public void processResult(InterpolrScope scope) {
        String mimeType = scope.getResultMimeType();
        if (MimeType.isML(mimeType) || MimeType.isJS(mimeType))
            scope.setParsedDocument(parse(scope.getBodyBytes(), scope));
        else
            scope.setParsedDocument(new Document(new DataChunk(scope.getBodyBytes())));
    }

    public Document parse(byte[] buffer, int start, int stop, InterpolrScope scope) {
        Document chunks = new Document();
        chunks.add(new DataChunk(buffer, start, stop));
        for (Plugin plugin : getPlugins())
            for (Rule rule : plugin.getRules()) {
                chunks = parse(rule, chunks, scope);
            }
        return chunks;
    }

    public List<Plugin> getPlugins() {
        return plugins;
    }

    public Document parse(byte[] data, InterpolrScope scope) {
        return parse(data, 0, data.length, scope);
    }

    public Document parse(Rule rule, Document input, InterpolrScope scope) {
        Document result = new Document();
        for (Chunk chunk : input.getChunks()) {
            if (chunk instanceof DataChunk) {
                List<Chunk> replacements = rule.parse((DataChunk) chunk, scope);
                result.addAll(replacements);
            } else
                result.add(chunk);
        }
        return result;
    }

    public ObjectMapper getJacksonObjectMapper() {
        return jacksonObjectMapper;
    }

    public void preflightCheck(InterpolrContext context) {
        context.getMustacheContext().checkAndCompileAll();
        if (context.getRootScope().getParsedDocument() != null) {
            context.getRootScope().getParsedDocument().check();
        }
    }

    public void setPlugins(Plugin[] plugins) {
        this.plugins = Arrays.asList(plugins);
    }
}