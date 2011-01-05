package com.fotonauts.lackr;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class SimpleSubstitutionEngine extends TextSubstitutionEngine {

	private Map<String, String> substitutions;
	
	public byte[] generateContent(LackrRequest rootRequest, byte[] byteContent) {
		if (!parseable(rootRequest))
			return byteContent;

		try {
			
			String content = new String(byteContent, "UTF-8");
			for(Map.Entry<String, String> entry: getSubstitutions().entrySet()) {
				content = content.replaceAll(
						entry.getKey(),entry.getValue());			
			}
			return content.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			/* very unlikely */		
			throw new RuntimeException(e);
		}

	}

	public String[] lookForSubqueries(LackrContentExchange exchange) {
		return new String[0];
	}

	public void setSubstitutions(Map<String, String> substitutions) {
		this.substitutions = substitutions;
	}

	public Map<String, String> getSubstitutions() {
		return substitutions;
	}

}
