package com.fotonauts.lackr.mustache;

import java.io.UnsupportedEncodingException;

import com.fotonauts.lackr.backend.LackrBackendRequest;
import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.ChunkUtils;
import com.fotonauts.lackr.interpolr.Document;
import com.fotonauts.lackr.interpolr.MarkupDetectingRule;
import com.fotonauts.lackr.interpolr.ViewChunk;

public class ArchiveRule extends MarkupDetectingRule {

    public ArchiveRule() {
        super("<script type=\"vnd.fotonauts/picordata\" id=\"*\">*</script><!-- END OF ARCHIVE -->");
    }

    @Override
    public Chunk substitute(Chunk buffer, int start, int[] boundPairs, int stop, Object context) {
        LackrBackendRequest request = (LackrBackendRequest) context;
        try {
            String archiveId = new String(ChunkUtils.extractBytes(buffer, boundPairs[0], boundPairs[1]), "UTF-8");
            Chunk inner = request.getFrontendRequest().getService().getInterpolr()
                    .parse(new ViewChunk(buffer, boundPairs[2], boundPairs[3]), request);

            request.getFrontendRequest().getMustacheContext().registerArchive(archiveId, inner);

            return new Document(new Chunk[] { new ViewChunk(buffer, start, boundPairs[2]), inner,
                    new ViewChunk(buffer, boundPairs[3], stop) });
        } catch (UnsupportedEncodingException e) {
            /* nope */
            throw new RuntimeException(e);
        }
    }
}
