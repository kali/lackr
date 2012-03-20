package com.fotonauts.lackr;

public abstract class TextSubstitutionEngine implements SubstitutionEngine {

	public TextSubstitutionEngine() {
		super();
	}

	protected boolean parseable(LackrFrontendRequest lackrRequest) {
		String mimeType = lackrRequest.getRootRequest().getExchange().getResponseHeaderValue("Content-Type");
		return mimeType != null && (MimeType.isML(mimeType) || MimeType.isJS(mimeType));
	}
}