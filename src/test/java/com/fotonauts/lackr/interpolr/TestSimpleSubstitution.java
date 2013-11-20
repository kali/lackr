package com.fotonauts.lackr.interpolr;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestSimpleSubstitution {

    @Test
    public void testNoop() throws Exception {
        Interpolr inter = new Interpolr();
        inter.start();
        Document r = InterpolrTestUtils.parse(inter, "foobar");
        assertEquals("(foobar)", r.toDebugString());
        assertEquals("foobar", InterpolrTestUtils.expand(r));
        inter.stop();
    }

    @Test
    public void testNoMatch() throws Exception {
        Interpolr inter = new Interpolr();
        inter.setPlugins(new Plugin[] { new SingleRulePlugin(new SimpleSubstitutionRule("titi", "toto")) });
        Document r = InterpolrTestUtils.parse(inter, "foobar");
        assertEquals("(foobar)", r.toDebugString());
        assertEquals("foobar", InterpolrTestUtils.expand(r));
    }

    @Test
    public void testFull() throws Exception {
        Interpolr inter = new Interpolr();
        inter.setPlugins(new Plugin[] { new SingleRulePlugin(new SimpleSubstitutionRule("titi", "toto")) });
        Document r = InterpolrTestUtils.parse(inter, "titi");
        assertEquals("<toto>", r.toDebugString());
        assertEquals("toto", InterpolrTestUtils.expand(r));
    }

    @Test
    public void testBegin() throws Exception {
        Interpolr inter = new Interpolr();
        inter.setPlugins(new Plugin[] { new SingleRulePlugin(new SimpleSubstitutionRule("titi", "toto")) });
        Document r = InterpolrTestUtils.parse(inter, "tatatiti");
        assertEquals("(tata)<toto>", r.toDebugString());
        assertEquals("tatatoto", InterpolrTestUtils.expand(r));
    }

    @Test
    public void testEnd() throws Exception {
        Interpolr inter = new Interpolr();
        inter.setPlugins(new Plugin[] { new SingleRulePlugin(new SimpleSubstitutionRule("titi", "toto")) });
        Document r = InterpolrTestUtils.parse(inter, "tititata");
        assertEquals("<toto>(tata)", r.toDebugString());
        assertEquals("tototata", InterpolrTestUtils.expand(r));
    }

    @Test
    public void testMiddle() throws Exception {
        Interpolr inter = new Interpolr();
        inter.setPlugins(new Plugin[] { new SingleRulePlugin(new SimpleSubstitutionRule("titi", "toto")) });
        Document r = InterpolrTestUtils.parse(inter, "tatatititata");
        assertEquals("(tata)<toto>(tata)", r.toDebugString());
        assertEquals("tatatototata", InterpolrTestUtils.expand(r));
    }

    @Test
    public void testSeveral() throws Exception {
        Interpolr inter = new Interpolr();
        inter.setPlugins(new Plugin[] { new SingleRulePlugin(new SimpleSubstitutionRule("titi", "toto")) });
        Document r = InterpolrTestUtils.parse(inter, "tatatititututititete");
        assertEquals("(tata)<toto>(tutu)<toto>(tete)", r.toDebugString());
        assertEquals("tatatototututototete", InterpolrTestUtils.expand(r));
    }

    @Test
    public void testMultiple() throws Exception {
        Interpolr inter = new Interpolr();
        inter.setPlugins(new Plugin[] { new SingleRulePlugin(new SimpleSubstitutionRule("titi", "toto")),
                new SingleRulePlugin(new SimpleSubstitutionRule("lili", "lolo")) });
        Document r = InterpolrTestUtils.parse(inter, "tatalilitatatititata");
        assertEquals("(tata)<lolo>(tata)<toto>(tata)", r.toDebugString());
        assertEquals("tatalolotatatototata", InterpolrTestUtils.expand(r));
    }

}
