package com.fotonauts.lackr.interpolr.json;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.ConstantChunk;
import com.fotonauts.lackr.interpolr.InterpolrScope;
import com.fotonauts.lackr.interpolr.JsonParseUtils;
import com.fotonauts.lackr.interpolr.MarkupDetectingRule;
import com.fotonauts.lackr.interpolr.Plugin;

public class DumpArchiveRule extends MarkupDetectingRule {

    public static class DumpArchiveChunk implements Chunk {

        private String name;
        private InterpolrScope scope;
        private Chunk result;
        private Plugin plugin;

        public DumpArchiveChunk(Plugin plugin, String archiveName, InterpolrScope scope) {
            this.name = archiveName;
            this.scope = scope;
            this.plugin = plugin;
        }

        @Override
        public int length() {
            return result.length();
        }

        @Override
        public void writeTo(OutputStream stream) throws IOException {
            result.writeTo(stream);
        }

        @Override
        public String toDebugString() {
            return "[DUMP " + name + " ]";
        }

        @Override
        public void check() {
            JsonContext ctx = (JsonContext) scope.getInterpolrContext().getPluginData(plugin);
            Archive archive = ctx.getArchive(name);
            String data = JsonParseUtils.prettyPrint(archive.getData());
            try {
                result = new ConstantChunk(data.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                /* ... */
            }
        }

    }

    private Plugin plugin;

    public DumpArchiveRule(Plugin plugin) {
        super("<!-- lackr:handlebars:dump archive=\"*\" -->");
        this.plugin = plugin;
    }

    @Override
    public Chunk substitute(byte[] buffer, int start, int[] boundPairs, int stop, InterpolrScope scope) {
        String archiveId = null;
        try {
            archiveId = new String(buffer, boundPairs[0], boundPairs[1] - boundPairs[0], "UTF-8");
        } catch (UnsupportedEncodingException e) {
            /* no thanks */
        }
        return new DumpArchiveChunk(plugin, archiveId, scope);
    }
}
