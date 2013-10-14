package com.fotonauts.lackr;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestSimpleSubstitution extends BaseTestSubstitution {

	public TestSimpleSubstitution() throws Exception {
		super();
	}

	@Test
	public void testAssetPaths() throws Exception {
		String result = expand("before\nhttp://_A_S_S_E_T_S___P_A_T_H_\nafter");
		assertEquals("before\nhttps://assets-virtual.ftnz.net\nafter", result);
	}

}
