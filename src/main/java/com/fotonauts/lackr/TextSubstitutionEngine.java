package com.fotonauts.lackr;

public abstract class TextSubstitutionEngine implements SubstitutionEngine {

	public TextSubstitutionEngine() {
		super();
	}

	protected boolean parseable(LackrRequest lackrRequest) {
		String mimeType = lackrRequest.rootExchange.getResponseFields().getStringField("Content-Type");
		return mimeType != null && (mimeType.startsWith("text/html")
				|| mimeType.startsWith("application/xml")
				|| mimeType.startsWith("application/json")
				|| mimeType.startsWith("application/atom+xml")
				|| mimeType.startsWith("text/javascript")
				|| mimeType.startsWith("application/x-mmtml")
				|| mimeType.startsWith("application/x-mmtml+xml"));
	}

}