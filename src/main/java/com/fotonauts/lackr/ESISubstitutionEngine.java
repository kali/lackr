package com.fotonauts.lackr;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jetty.http.HttpHeaders;
import org.json.JSONObject;

public class ESISubstitutionEngine extends TextSubstitutionEngine implements SubstitutionEngine {

	abstract private static class ESIIncludePattern {
		protected Pattern pattern;

		public abstract String translate(String content, String mimeType);

		public Pattern getPattern() {
			return pattern;
		}
	}

	private static class JSESIInclude extends ESIIncludePattern {

		public JSESIInclude() {
			pattern = Pattern.compile("\"ssi:include:virtual:(.*)\"");
		}

		@Override
		public String translate(String content, String mimeType) {
			if (MimeType.isJS(mimeType))
				return content;
			else if (MimeType.isML(mimeType))
				return JSONObject.quote(content);
			return "{}";
		}
	}

	private static class MLESIInclude extends ESIIncludePattern {

		public MLESIInclude() {
			pattern = Pattern.compile("<!--# include virtual=\"(.*?)\" -->");
		}

		@Override
		public String translate(String content, String mimeType) {
			if (MimeType.isML(mimeType))
				return content;
			throw new RuntimeException("unsupported ESI type (js* in *ML context)");
		}
	}

	private static class JSMLESIInclude extends ESIIncludePattern {

		public JSMLESIInclude() {
			pattern = Pattern.compile("\\\\u003C!--# include virtual=\\\\\"(.*?)\\\\\" --\\\\u003E");
		}

		@Override
		public String translate(String content, String mimeType) {
			if (MimeType.isML(mimeType)) {
				String json = JSONObject.quote(content);
				return json.substring(1, json.length() - 1);
			}
			throw new RuntimeException("unsupported ESI type (js* in js(*ML) context)");
		}
	}
	
	private static class HttpESIInclude extends ESIIncludePattern {
		public HttpESIInclude() {
			pattern = Pattern.compile("http://esi\\.include\\.virtual(/.*)#");
		}

		@Override
        public String translate(String content, String mimeType) {
			return content;
        }
		
	}

	static ESIIncludePattern[] patterns;
	static {
		patterns = new ESIIncludePattern[4];
		patterns[0] = new MLESIInclude();
		patterns[1] = new JSESIInclude();
		patterns[2] = new JSMLESIInclude();
		patterns[3] = new HttpESIInclude();
	}

	@Override
	public String[] lookForSubqueries(LackrContentExchange lackrContentExchange) {
		if (!parseable(lackrContentExchange.lackrRequest))
			return new String[0];

		List<String> subs = new ArrayList<String>();
		if (lackrContentExchange.getResponseContentBytes() != null
		        && lackrContentExchange.getResponseContentBytes().length > 0) {
			String content;
			try {
				content = new String(lackrContentExchange.getResponseContentBytes(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				/* very, very unlikely */
				throw new RuntimeException(e);
			}
			for (ESIIncludePattern pattern : patterns) {
				Matcher matcher = pattern.getPattern().matcher(content);
				while (matcher.find()) {
					subs.add(matcher.group(1));
				}
			}
		}
		return (String[]) subs.toArray(new String[subs.size()]);
	}

	@Override
	public byte[] generateContent(LackrRequest rootRequest, byte[] byteContent) {
		if (!parseable(rootRequest))
			return rootRequest.rootExchange.getResponseContentBytes();

		StringBuilder content;
		try {
			content = new StringBuilder(new String(byteContent, "UTF-8"));
			boolean replacedSome = false;
			do {
				replacedSome = false;
				for (ESIIncludePattern pattern : patterns) {
					Matcher matcher = pattern.getPattern().matcher(content);
					if (matcher.find()) {
						String replacement = "";
						LackrContentExchange exchange = rootRequest.fragmentsMap.get(matcher.group(1));
						if (exchange.getResponseContentBytes() != null) {
							replacement = pattern.translate(new String(exchange.getResponseContentBytes(), "UTF-8"),
							        exchange.getResponseFields().getStringField(HttpHeaders.CONTENT_TYPE));
						}
						content.replace(matcher.start(0), matcher.end(0), replacement);
						replacedSome = true;
					}
				}
			} while (replacedSome);
			return content.toString().getBytes("UTF-8");

		} catch (UnsupportedEncodingException e) {
			/* very, very unlikely */
			throw new RuntimeException(e);
		}
	}

}
