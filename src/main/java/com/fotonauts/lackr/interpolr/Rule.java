package com.fotonauts.lackr.interpolr;

import java.util.List;

public interface Rule {
	
	List<Chunk> parse(DataChunk chunk);
	
}
