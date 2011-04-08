package com.fotonauts.lackr.interpolr;

public class TestSimpleSubstitution extends BaseTestSubstitution {

	public void testNoop() throws Exception {
		inter = new Interpolr();
		Document r = parse("foobar");
		assertEquals("(foobar)", r.toDebugString());
		assertEquals("foobar", expand(r));
	}

	public void testNoMatch() throws Exception {
		inter = new Interpolr();
		inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
		Document r = parse("foobar");
		assertEquals("(foobar)", r.toDebugString());
		assertEquals("foobar", expand(r));
	}

	public void testFull() throws Exception {
		inter = new Interpolr();
		inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
		Document r = parse("titi");
		assertEquals("<toto>", r.toDebugString());
		assertEquals("toto", expand(r));
	}

	public void testBegin() throws Exception {
		inter = new Interpolr();
		inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
		Document r = parse("tatatiti");
		assertEquals("(tata)<toto>", r.toDebugString());
		assertEquals("tatatoto", expand(r));
	}

	public void testEnd() throws Exception {
		inter = new Interpolr();
		inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
		Document r = parse("tititata");
		assertEquals("<toto>(tata)", r.toDebugString());
		assertEquals("tototata", expand(r));
	}

	public void testMiddle() throws Exception {
		inter = new Interpolr();
		inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
		Document r = parse("tatatititata");
		assertEquals("(tata)<toto>(tata)", r.toDebugString());
		assertEquals("tatatototata", expand(r));
	}

	public void testSeveral() throws Exception {
		inter = new Interpolr();
		inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
		Document r = parse("tatatititututititete");
		assertEquals("(tata)<toto>(tutu)<toto>(tete)", r.toDebugString());
		assertEquals("tatatototututototete", expand(r));
	}

	public void testMultiple() throws Exception {
		inter = new Interpolr();
		inter.addRule(new SimpleSubstitutionRule("titi", "toto"));
		inter.addRule(new SimpleSubstitutionRule("lili", "lolo"));
		Document r = parse("tatalilitatatititata");
		assertEquals("(tata)<lolo>(tata)<toto>(tata)", r.toDebugString());
		assertEquals("tatalolotatatototata", expand(r));
	}
}
