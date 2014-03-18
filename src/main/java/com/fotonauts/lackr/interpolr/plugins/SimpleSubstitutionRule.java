package com.fotonauts.lackr.interpolr.plugins;

import java.io.UnsupportedEncodingException;
import java.util.List;

import com.fotonauts.lackr.interpolr.InterpolrScope;
import com.fotonauts.lackr.interpolr.rope.Chunk;
import com.fotonauts.lackr.interpolr.rope.ConstantChunk;
import com.fotonauts.lackr.interpolr.rope.DataChunk;

public class SimpleSubstitutionRule extends SimpleTriggerRule {

    private Chunk replacement;

    public SimpleSubstitutionRule(String placeholder, String replacement) {
        setTrigger(placeholder);
        setReplacement(replacement);
    }

    public void setPlaceholder(String placeholder) {
        setTrigger(placeholder);
    }

    public void setReplacement(String replacement) {
        try {
            this.replacement = new ConstantChunk(replacement.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // no way
        }
    }

    @Override
    protected int onFound(List<Chunk> result, DataChunk chunk, int startFound, InterpolrScope scope) {
        result.add(replacement);
        return trigger.length();
    }

}
