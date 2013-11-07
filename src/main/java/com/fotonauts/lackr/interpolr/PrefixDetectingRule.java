package com.fotonauts.lackr.interpolr;

import java.util.List;

public abstract class PrefixDetectingRule extends SimpleTriggerRule {

    public PrefixDetectingRule() {
    }
    
    public PrefixDetectingRule(String prefix) {
        setTrigger(prefix);
    }

    public abstract int lookaheadForEnd(Chunk buffer, int start, int stop);

    public abstract Chunk substitute(Chunk buffer, int start, int stop, Object context);

    @Override
    protected int onFound(List<Chunk> result, Chunk chunk, int startFound, Object context) {
        int lookahead = lookaheadForEnd(chunk, startFound, chunk.length());
        result.add(substitute(chunk, startFound, lookahead, context));
        return lookahead - startFound;
    }
}
