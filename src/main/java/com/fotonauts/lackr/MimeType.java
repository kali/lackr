package com.fotonauts.lackr;

public class MimeType {

	public static String APPLICATION_ATOM_XML = "application/atom";
	public static String APPLICATION_JAVASCRIPT = "application/javascript";
	public static String APPLICATION_JSON = "application/json";
	public static String APPLICATION_X_MMTML = "application/x-mmtml";
	public static String APPLICATION_XML = "application/xml";
	public static String TEXT_HTML = "text/html";
	public static String TEXT_JAVASCRIPT = "text/javascript";
	public static String TEXT_PLAIN = "text/plain";

	public static boolean isML(String mimeType) {
		if(isTextPlain(mimeType))
			return false;
		return mimeType.startsWith(TEXT_HTML) || mimeType.startsWith(APPLICATION_ATOM_XML)
		        || mimeType.startsWith(APPLICATION_X_MMTML) || mimeType.startsWith(APPLICATION_XML);
	}

	public static boolean isJS(String mimeType) {
		if(isTextPlain(mimeType))
			return false;
		return mimeType.startsWith(APPLICATION_JAVASCRIPT) || mimeType.startsWith(APPLICATION_JSON)
		        || mimeType.startsWith(TEXT_JAVASCRIPT);
	}

	public static boolean isTextPlain(String mimeType) {
		return mimeType == null || mimeType.equals("") || mimeType.startsWith(TEXT_PLAIN);
	}
}
