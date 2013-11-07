package com.fotonauts.lackr.interpolr;

import java.util.List;

public abstract class MarkupDetectingRule extends SimpleTriggerRule {

    final private BoyerMooreScanner[] patterns;

    public MarkupDetectingRule(String markup) {
        String[] parts = markup.split("\\*");
        patterns = new BoyerMooreScanner[parts.length];
        for (int i = 0; i < parts.length; i++)
            patterns[i] = new BoyerMooreScanner(parts[i].getBytes());
        setTrigger(patterns[0]);
    }

    public abstract Chunk substitute(Chunk chunk, int start, int[] boundPairs, int stop, Object context);

    @Override
    protected int onFound(List<Chunk> result, Chunk chunk, int startFound, Object context) {
        int boundPairs[] = new int[2 * (patterns.length - 1)];
        boolean broken = false;
        int lookahead = startFound + patterns[0].length();
        for (int i = 1; !broken && i < patterns.length; i++) {
            boundPairs[2 * (i - 1)] = lookahead;
            lookahead = patterns[i].searchNext(chunk, lookahead, chunk.length());
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
            result.add(new ViewChunk(chunk, startFound, chunk.length()));
            return chunk.length() - startFound;
        } else {
            result.add(substitute(chunk, startFound, boundPairs, lookahead, context));
            return lookahead - startFound;
        }
    }
}
