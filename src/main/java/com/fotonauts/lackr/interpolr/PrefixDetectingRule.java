package com.fotonauts.lackr.interpolr;

import java.util.List;

public abstract class PrefixDetectingRule extends SimpleTriggerRule {

    public PrefixDetectingRule() {
    }

    public PrefixDetectingRule(String prefix) {
        setTrigger(prefix);
    }

    public abstract int lookaheadForEnd(byte[] buffer, int start, int stop);

    public abstract Chunk substitute(byte[] buffer, int start, int stop, InterpolrScope scope);

    @Override
    protected int onFound(List<Chunk> result, DataChunk chunk, int startFound, InterpolrScope scope) {
        int lookahead = lookaheadForEnd(chunk.getBuffer(), startFound, chunk.getStop());
        result.add(substitute(chunk.getBuffer(), startFound, lookahead, scope));
        return lookahead - startFound;
    }
}
