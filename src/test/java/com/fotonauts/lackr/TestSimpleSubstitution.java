package com.fotonauts.lackr;


public class TestSimpleSubstitution extends BaseTestSubstitution {
	
	public void testAssetPaths() throws Exception {
	    String result = expand("before\nhttp://_A_S_S_E_T_S___P_A_T_H_\nafter");
	    assertEquals("before\n\nafter", result);
    }
	
}
