package com.fotonauts.lackr.mustache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.fotonauts.lackr.BackendRequest;
import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.ConstantChunk;
import com.fotonauts.lackr.interpolr.MarkupDetectingRule;

public class DumpArchiveRule extends MarkupDetectingRule {

    public static class DumpArchiveChunk implements Chunk {

        private String name;
        private BackendRequest request;
        private Chunk result;
        
        public DumpArchiveChunk(String archiveName, BackendRequest request) {
            this.name = archiveName;
            this.request = request;
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
            Archive archive = request.getFrontendRequest().getMustacheContext().getArchive(name);
            ObjectMapper mapper = request.getFrontendRequest().getService().getJacksonObjectMapper();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                mapper.defaultPrettyPrintingWriter().writeValue(baos, archive.getData());
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
            result = new ConstantChunk(baos.toByteArray());
        }
        
    }
    
	public DumpArchiveRule() {
        super("<!-- lackr:mustache:dump archive=\"*\" -->");
	}

	@Override
	public Chunk substitute(byte[] buffer, int start, int[] boundPairs, int stop, Object context) {
        String archiveId = null;
        try {
            archiveId = new String(buffer, boundPairs[0], boundPairs[1] - boundPairs[0], "UTF-8");
        } catch (UnsupportedEncodingException e) {
            /* no thanks */
        }
	    return new DumpArchiveChunk(archiveId, (BackendRequest) context);
	}
}
