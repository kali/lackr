package com.fotonauts.lackr.interpolr.plugins;

import java.util.List;

import com.fotonauts.lackr.interpolr.InterpolrScope;
import com.fotonauts.lackr.interpolr.rope.Chunk;
import com.fotonauts.lackr.interpolr.rope.DataChunk;
import com.fotonauts.lackr.interpolr.utils.BoyerMooreScanner;

public abstract class MarkupDetectingRule extends SimpleTriggerRule {

    final private BoyerMooreScanner[] patterns;

    public MarkupDetectingRule(String markup) {
        String[] parts = markup.split("\\*");
        patterns = new BoyerMooreScanner[parts.length];
        for (int i = 0; i < parts.length; i++)
            patterns[i] = new BoyerMooreScanner(parts[i].getBytes());
        setTrigger(patterns[0]);
    }

    public abstract Chunk substitute(byte[] buffer, int start, int[] boundPairs, int stop, InterpolrScope scope);

    @Override
    protected int onFound(List<Chunk> result, DataChunk chunk, int startFound, InterpolrScope scope) {
        int boundPairs[] = new int[2 * (patterns.length - 1)];
        boolean broken = false;
        int lookahead = startFound + patterns[0].length();
        for (int i = 1; !broken && i < patterns.length; i++) {
            boundPairs[2 * (i - 1)] = lookahead;
            lookahead = patterns[i].searchNext(chunk.getBuffer(), lookahead, chunk.getStop());
            if (lookahead == -1) {
                // unclosed tag, bail out
                broken = true;
            } else {
                boundPairs[2 * i - 1] = lookahead;
                lookahead += patterns[i].length();
            }
        }
        if (broken) {
            // unclosed tag, bail out
            result.add(new DataChunk(chunk.getBuffer(), startFound, chunk.getStop()));
            return chunk.getStop() - startFound;
        } else {
            result.add(substitute(chunk.getBuffer(), startFound, boundPairs, lookahead, scope));
            return lookahead - startFound;
        }
    }
}
