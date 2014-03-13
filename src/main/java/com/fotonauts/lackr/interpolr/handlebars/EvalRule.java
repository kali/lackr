package com.fotonauts.lackr.interpolr.handlebars;

import java.io.UnsupportedEncodingException;

import com.fotonauts.lackr.interpolr.Chunk;
import com.fotonauts.lackr.interpolr.InterpolrScope;
import com.fotonauts.lackr.interpolr.MarkupDetectingRule;
import com.fotonauts.lackr.interpolr.Plugin;

public class EvalRule extends MarkupDetectingRule {

    private Plugin plugin;

    public EvalRule(HandlebarsPlugin plugin) {
        super("<!-- " + plugin.getPrefix() + ":eval name=\"*\" -->*<!-- /" + plugin.getPrefix() + ":eval -->");
        this.plugin = plugin;
    }

    @Override
    public Chunk substitute(byte[] buffer, int start, int[] boundPairs, int stop, InterpolrScope scope) {
        String name;
        try {
            name = new String(buffer, boundPairs[0], boundPairs[1] - boundPairs[0], "UTF-8");
            return new HandlebarsEvalChunk(plugin, name, buffer, boundPairs[2], boundPairs[3], scope);
        } catch (UnsupportedEncodingException e) {
            // fu*k off
            return null;
        }
    }
}
