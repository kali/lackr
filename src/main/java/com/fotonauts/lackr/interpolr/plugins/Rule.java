package com.fotonauts.lackr.interpolr.plugins;

import java.util.List;

import com.fotonauts.lackr.interpolr.InterpolrScope;
import com.fotonauts.lackr.interpolr.rope.Chunk;
import com.fotonauts.lackr.interpolr.rope.DataChunk;

public interface Rule {

    List<Chunk> parse(DataChunk chunk, InterpolrScope scope);

}
