package com.fotonauts.lackr.interpolr;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestMarkupDetection extends BaseTestSubstitution {

	protected Interpolr simpleIterpolr() throws Exception {
		Interpolr inter = new Interpolr();
		inter.addRule(new MarkupDetectingRule("{*:*}") {

			@Override
			public Chunk substitute(byte[] buffer, int start, int stop, Object blah) {
				return new ConstantChunk(("'" + new String(buffer, start, stop - start) + "'").getBytes());
			}
		});
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
		assertEquals("(tata{tititata)", r.toDebugString());
		assertEquals("tata{tititata", expand(r));
	}
}
