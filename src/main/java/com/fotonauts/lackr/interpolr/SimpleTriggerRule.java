package com.fotonauts.lackr.interpolr;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public abstract class SimpleTriggerRule implements Rule {

    protected BoyerMooreScanner trigger;

    public SimpleTriggerRule() {
        super();
    }

    public void setTrigger(String trigger) {
    	try {
    	    if(StringUtils.isNotBlank(trigger))
    	        setTrigger(new BoyerMooreScanner(trigger.getBytes("UTF-8")));
    	    else
    	        trigger = null;
        } catch (UnsupportedEncodingException e) {
            // unlikely
        }
    }

    public void setTrigger(BoyerMooreScanner boyerMooreScanner) {
        trigger = boyerMooreScanner;
    }


    @Override
    public Chunk parse(Chunk chunk, Object context) {
        List<Chunk> result = new ArrayList<Chunk>();
        if(trigger == null) {
            return chunk;
        }
        int current = 0;
        while (current < chunk.length()) {
            // int found = trigger.searchNext(chunk.getBuffer(), current, chunk.getStop());
            int found = trigger.searchNext(new ViewChunk(chunk, current, chunk.length()));
            if (found == -1) {
                result.add(new ViewChunk(chunk, current, chunk.length()));
                current = chunk.length();
            } else {
                if (found > 0) {
                    result.add(new ViewChunk(chunk, current, found));
                }
                current = found + onFound(result, chunk, found, context);
            }
        }
        if(result.size() == 1)
            return chunk;
        else
            return new Document(result);
    }

    abstract protected int onFound(List<Chunk> result, Chunk chunk, int index, Object context);
}