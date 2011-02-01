package com.fotonauts.lackr;

public interface SubstitutionEngine {

	public byte[] generateContent(LackrRequest rootRequest, byte[] byteContent);

	public String[] lookForSubqueries(LackrContentExchange exchange);
}
