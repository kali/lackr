package com.fotonauts.lackr.mustache;

import java.io.UnsupportedEncodingException;

import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.DataChunk;
import com.fotonauts.lackr.interpolr.Document;
import com.fotonauts.lackr.interpolr.InterpolrScope;
import com.fotonauts.lackr.interpolr.MarkupDetectingRule;
import com.fotonauts.lackr.interpolr.Plugin;

public class ArchiveRule extends MarkupDetectingRule {

    private Plugin plugin;

    public ArchiveRule(Plugin plugin) {
        super("<script type=\"vnd.fotonauts/picordata\" id=\"*\">*</script><!-- END OF ARCHIVE -->");
        this.plugin = plugin;
    }

    @Override
    public Chunk substitute(byte[] buffer, int start, int[] boundPairs, int stop, InterpolrScope scope) {
        try {
            String archiveId = new String(buffer, boundPairs[0], boundPairs[1] - boundPairs[0], "UTF-8");
            Document inner = scope.getInterpolr().parse(buffer, boundPairs[2], boundPairs[3], scope);
            HandlebarsContext ctx = (HandlebarsContext) scope.getInterpolrContext().getPluginData(plugin);
            ctx.registerArchive(archiveId, inner);

            return new Document(new Chunk[] { new DataChunk(buffer, start, boundPairs[2]), inner,
                    new DataChunk(buffer, boundPairs[3], stop) });
        } catch (UnsupportedEncodingException e) {
            /* nope */
            throw new RuntimeException(e);
        }
    }
}
