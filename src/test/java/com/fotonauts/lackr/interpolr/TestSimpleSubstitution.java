package com.fotonauts.lackr.interpolr;

import static org.junit.Assert.assertEquals;

public class TestSimpleSubstitution extends BaseTestSubstitution {

	public void testNoop() throws Exception {
		Interpolr inter = new Interpolr();
		Document r = parse(inter, "foobar");
		assertEquals("(foobar)", r.toDebugString());
		assertEquals("foobar", expand(r));
	}

	public void testNoMatch() throws Exception {
		Interpolr inter = new Interpolr();
		inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
		Document r = parse(inter, "foobar");
		assertEquals("(foobar)", r.toDebugString());
		assertEquals("foobar", expand(r));
	}

	public void testFull() throws Exception {
		Interpolr inter = new Interpolr();
		inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
		Document r = parse(inter, "titi");
		assertEquals("<toto>", r.toDebugString());
		assertEquals("toto", expand(r));
	}

	public void testBegin() throws Exception {
		Interpolr inter = new Interpolr();
		inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
		Document r = parse(inter, "tatatiti");
		assertEquals("(tata)<toto>", r.toDebugString());
		assertEquals("tatatoto", expand(r));
	}

	public void testEnd() throws Exception {
		Interpolr inter = new Interpolr();
		inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
		Document r = parse(inter, "tititata");
		assertEquals("<toto>(tata)", r.toDebugString());
		assertEquals("tototata", expand(r));
	}

	public void testMiddle() throws Exception {
		Interpolr inter = new Interpolr();
		inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
		Document r = parse(inter, "tatatititata");
		assertEquals("(tata)<toto>(tata)", r.toDebugString());
		assertEquals("tatatototata", expand(r));
	}

	public void testSeveral() throws Exception {
		Interpolr inter = new Interpolr();
		inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
		Document r = parse(inter, "tatatititututititete");
		assertEquals("(tata)<toto>(tutu)<toto>(tete)", r.toDebugString());
		assertEquals("tatatototututototete", expand(r));
	}

	public void testMultiple() throws Exception {
		Interpolr inter = new Interpolr();
		inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
		inter.addRule(new SimpleSubstitutionRule("lili", "lolo"));
		Document r = parse(inter, "tatalilitatatititata");
		assertEquals("(tata)<lolo>(tata)<toto>(tata)", r.toDebugString());
		assertEquals("tatalolotatatototata", expand(r));
	}
}
