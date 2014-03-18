package com.fotonauts.lackr.interpolr;

import static com.fotonauts.lackr.testutils.InterpolrTestUtils.expand;
import static com.fotonauts.lackr.testutils.InterpolrTestUtils.parse;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.fotonauts.lackr.interpolr.plugins.MarkupDetectingRule;
import com.fotonauts.lackr.interpolr.plugins.Plugin;
import com.fotonauts.lackr.interpolr.plugins.SingleRulePlugin;
import com.fotonauts.lackr.interpolr.rope.Chunk;
import com.fotonauts.lackr.interpolr.rope.ConstantChunk;
import com.fotonauts.lackr.interpolr.rope.Document;

public class TestMarkupDetection {

    protected Interpolr simpleIterpolr() throws Exception {
        Interpolr inter = new Interpolr();
        inter.setPlugins(new Plugin[] { new SingleRulePlugin(new MarkupDetectingRule("{*}") {

            @Override
            public Chunk substitute(byte[] buffer, int start, int[] boundPairs, int stop, InterpolrScope blah) {
                return new ConstantChunk(("'" + new String(buffer, boundPairs[0], boundPairs[1] - boundPairs[0]) + "'").getBytes());
            }
        }) });
        return inter;
    }

    protected Interpolr doubleIterpolr() throws Exception {
        Interpolr inter = new Interpolr();
        inter.setPlugins(new Plugin[] { new SingleRulePlugin(new MarkupDetectingRule("{*:*}") {

            @Override
            public Chunk substitute(byte[] buffer, int start, int[] boundPairs, int stop, InterpolrScope blah) {
                return new ConstantChunk(("'" + new String(buffer, boundPairs[0], boundPairs[1] - boundPairs[0]) + "->"
                        + new String(buffer, boundPairs[2], boundPairs[3] - boundPairs[2]) + "'").getBytes());
            }
        }) });
        return inter;
    }

    @Test
    public void testNoMatch() throws Exception {
        Document r = parse(simpleIterpolr(), "foobar");
        assertEquals("(foobar)", r.toDebugString());
        assertEquals("foobar", expand(r));
    }

    @Test
    public void testFull() throws Exception {
        Document r = parse(simpleIterpolr(), "{titi}");
        assertEquals("<'titi'>", r.toDebugString());
        assertEquals("'titi'", expand(r));
    }

    @Test
    public void testBegin() throws Exception {
        Document r = parse(simpleIterpolr(), "tat{a}");
        assertEquals("(tat)<'a'>", r.toDebugString());
        assertEquals("tat'a'", expand(r));
    }

    @Test
    public void testEnd() throws Exception {
        Document r = parse(simpleIterpolr(), "{t}ata");
        assertEquals("<'t'>(ata)", r.toDebugString());
        assertEquals("'t'ata", expand(r));
    }

    @Test
    public void testMiddle() throws Exception {
        Document r = parse(simpleIterpolr(), "tata{titi}tata");
        assertEquals("(tata)<'titi'>(tata)", r.toDebugString());
        assertEquals("tata'titi'tata", expand(r));
    }

    @Test
    public void testNotClose() throws Exception {
        Document r = parse(simpleIterpolr(), "tata{tititata");
        assertEquals("(tata)({tititata)", r.toDebugString());
        assertEquals("tata{tititata", expand(r));
    }

    @Test
    public void testDoubleNoMatch() throws Exception {
        Document r = parse(doubleIterpolr(), "foobar");
        assertEquals("(foobar)", r.toDebugString());
        assertEquals("foobar", expand(r));
    }

    @Test
    public void testDoubleFull() throws Exception {
        Document r = parse(doubleIterpolr(), "{titi:tata}");
        assertEquals("<'titi->tata'>", r.toDebugString());
        assertEquals("'titi->tata'", expand(r));
    }

    @Test
    public void testDoubleBegin() throws Exception {
        Document r = parse(doubleIterpolr(), "tat{a:i}");
        assertEquals("(tat)<'a->i'>", r.toDebugString());
        assertEquals("tat'a->i'", expand(r));
    }

    @Test
    public void testDoubleEnd() throws Exception {
        Document r = parse(doubleIterpolr(), "{t:s}ata");
        assertEquals("<'t->s'>(ata)", r.toDebugString());
        assertEquals("'t->s'ata", expand(r));
    }

    @Test
    public void testDoubleMiddle() throws Exception {
        Document r = parse(doubleIterpolr(), "tata{titi:toto}tata");
        assertEquals("(tata)<'titi->toto'>(tata)", r.toDebugString());
        assertEquals("tata'titi->toto'tata", expand(r));
    }

    @Test
    public void testDoubleNotClose() throws Exception {
        Document r = parse(doubleIterpolr(), "tata{tititata");
        assertEquals("(tata)({tititata)", r.toDebugString());
        assertEquals("tata{tititata", expand(r));
    }

    @Test
    public void testDoubleNotCloseSecond() throws Exception {
        Document r = parse(doubleIterpolr(), "tata{titi:tata");
        assertEquals("(tata)({titi:tata)", r.toDebugString());
        assertEquals("tata{titi:tata", expand(r));
    }
}
