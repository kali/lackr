package com.fotonauts.lackr.interpolr;

public class TestMarkupDetection extends BaseTestSubstitution {

	@Override
	protected void setUp() throws Exception {
	    // TODO Auto-generated method stub
	    super.setUp();
		inter = new Interpolr();
		inter.addRule(new MarkupDetectingRule("{*}") {

			@Override
			public Chunk substitute(byte[] buffer, int start, int stop, Object blah) {
				return new ConstantChunk(("'" + new String(buffer, start, stop-start) + "'").getBytes());
			}
		});
}
	
	public void testNoMatch() throws Exception {
		Document r = parse("foobar");
		assertEquals("(foobar)", r.toDebugString());
		assertEquals("foobar", expand(r));
	}

	public void testFull() throws Exception {
		Document r = parse("{titi}");
		assertEquals("<'titi'>", r.toDebugString());
		assertEquals("'titi'", expand(r));
	}

	public void testBegin() throws Exception {
		Document r = parse("tat{a}");
		assertEquals("(tat)<'a'>", r.toDebugString());
		assertEquals("tat'a'", expand(r));
	}

	public void testEnd() throws Exception {
		Document r = parse("{t}ata");
		assertEquals("<'t'>(ata)", r.toDebugString());
		assertEquals("'t'ata", expand(r));
	}

	public void testMiddle() throws Exception {
		Document r = parse("tata{titi}tata");
		assertEquals("(tata)<'titi'>(tata)", r.toDebugString());
		assertEquals("tata'titi'tata", expand(r));
	}

	public void testNotClose() throws Exception {
		Document r = parse("tata{tititata");
		assertEquals("(tata{tititata)", r.toDebugString());
		assertEquals("tata{tititata", expand(r));
	}
}
