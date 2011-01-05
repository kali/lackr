package com.fotonauts.lackr;

import java.io.UnsupportedEncodingException;

public class SimpleSubstitutionEngine extends TextSubstitutionEngine {

	public byte[] generateContent(LackrRequest rootRequest, byte[] byteContent) {
		if (!parseable(rootRequest))
			return byteContent;

		try {
			return new String(byteContent, "UTF-8")
					.replaceAll(
							"http://_A_S_S_E_T_S___P_A_T_H_",
							"")
					.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			/* very unlikely */		
			throw new RuntimeException(e);
		}

	}

	public String[] lookForSubqueries(LackrContentExchange exchange) {
		return new String[0];
	}

}
