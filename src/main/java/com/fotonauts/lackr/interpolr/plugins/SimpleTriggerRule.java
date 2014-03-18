package com.fotonauts.lackr.interpolr.plugins;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fotonauts.lackr.interpolr.InterpolrScope;
import com.fotonauts.lackr.interpolr.rope.Chunk;
import com.fotonauts.lackr.interpolr.rope.DataChunk;
import com.fotonauts.lackr.interpolr.utils.BoyerMooreScanner;

public abstract class SimpleTriggerRule implements Rule {

    protected BoyerMooreScanner trigger;

    public SimpleTriggerRule() {
        super();
    }

    public void setTrigger(String trigger) {
        try {
            if (StringUtils.isNotBlank(trigger))
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
    public List<Chunk> parse(DataChunk chunk, InterpolrScope scope) {
        List<Chunk> result = new ArrayList<Chunk>();
        if (trigger == null) {
            result.add(chunk);
            return result;
        }
        int current = chunk.getStart();
        while (current < chunk.getStop()) {
            int found = trigger.searchNext(chunk.getBuffer(), current, chunk.getStop());
            if (found == -1) {
                result.add(new DataChunk(chunk.getBuffer(), current, chunk.getStop()));
                current = chunk.getStop();
            } else {
                if (found > 0) {
                    result.add(new DataChunk(chunk.getBuffer(), current, found));
                }
                current = found + onFound(result, chunk, found, scope);
            }
        }
        return result;
    }

    abstract protected int onFound(List<Chunk> result, DataChunk chunk, int index, InterpolrScope scope);
}