package com.fotonauts.lackr.interpolr;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.fotonauts.lackr.picorassets.AssetPrefixRule;
import com.fotonauts.lackr.picorassets.AssetResolver;

public class TestSimpleSubstitution extends BaseTestSubstitution {

    @Test
    public void testNoop() throws Exception {
        Interpolr inter = new Interpolr();
        Document r = parse(inter, "foobar");
        assertEquals("(foobar)", r.toDebugString());
        assertEquals("foobar", expand(r));
    }

    @Test
    public void testNoMatch() throws Exception {
        Interpolr inter = new Interpolr();
        inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
        Document r = parse(inter, "foobar");
        assertEquals("(foobar)", r.toDebugString());
        assertEquals("foobar", expand(r));
    }

    @Test
    public void testFull() throws Exception {
        Interpolr inter = new Interpolr();
        inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
        Document r = parse(inter, "titi");
        assertEquals("<toto>", r.toDebugString());
        assertEquals("toto", expand(r));
    }

    @Test
    public void testBegin() throws Exception {
        Interpolr inter = new Interpolr();
        inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
        Document r = parse(inter, "tatatiti");
        assertEquals("(tata)<toto>", r.toDebugString());
        assertEquals("tatatoto", expand(r));
    }

    @Test
    public void testEnd() throws Exception {
        Interpolr inter = new Interpolr();
        inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
        Document r = parse(inter, "tititata");
        assertEquals("<toto>(tata)", r.toDebugString());
        assertEquals("tototata", expand(r));
    }

    @Test
    public void testMiddle() throws Exception {
        Interpolr inter = new Interpolr();
        inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
        Document r = parse(inter, "tatatititata");
        assertEquals("(tata)<toto>(tata)", r.toDebugString());
        assertEquals("tatatototata", expand(r));
    }

    @Test
    public void testSeveral() throws Exception {
        Interpolr inter = new Interpolr();
        inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
        Document r = parse(inter, "tatatititututititete");
        assertEquals("(tata)<toto>(tutu)<toto>(tete)", r.toDebugString());
        assertEquals("tatatototututototete", expand(r));
    }

    @Test
    public void testMultiple() throws Exception {
        Interpolr inter = new Interpolr();
        inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
        inter.addRule(new SimpleSubstitutionRule("lili", "lolo"));
        Document r = parse(inter, "tatalilitatatititata");
        assertEquals("(tata)<lolo>(tata)<toto>(tata)", r.toDebugString());
        assertEquals("tatalolotatatototata", expand(r));
    }

    @Test
    public void testAssetBasic() throws Exception {
        Interpolr inter = new Interpolr();
        inter.addRule(new AssetPrefixRule(new AssetResolver() {

            @Override
            public String resolve(String asset) {
                return "`" + asset + "'";
            }

            @Override
            public String getMagicPrefix() {
                return "/lackr.prefix.for.assets/";
            }

        }));
        assertEquals("<`/lackr.prefix.for.assets/some/asset.jpg'>", parse(inter, "/lackr.prefix.for.assets/some/asset.jpg")
                .toDebugString());
        assertEquals("(blah )<`/lackr.prefix.for.assets/some/asset.jpg'>( blah)",
                parse(inter, "blah /lackr.prefix.for.assets/some/asset.jpg blah").toDebugString());
    }
}
