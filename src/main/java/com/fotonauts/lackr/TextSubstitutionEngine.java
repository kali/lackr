package com.fotonauts.lackr;

public abstract class TextSubstitutionEngine implements SubstitutionEngine {

	public TextSubstitutionEngine() {
		super();
	}

	protected boolean parseable(LackrRequest lackrRequest) {
		String mimeType = lackrRequest.rootExchange.getResponseFields().getStringField("Content-Type");
		return mimeType != null && (MimeType.isML(mimeType) || MimeType.isJS(mimeType));
	}
}