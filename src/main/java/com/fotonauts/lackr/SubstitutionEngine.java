package com.fotonauts.lackr;

import java.io.IOException;

public interface SubstitutionEngine {

	public byte[] generateContent(LackrRequest rootRequest, byte[] byteContent);

	public void scheduleSubQueries(LackrContentExchange lackrContentExchange, LackrRequest lackrRequest) throws IOException;
}
