package com.fotonauts.lackr;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.HttpMethods;
import org.json.JSONObject;

import com.fotonauts.lackr.SubstitutionEngine.IncludeException;

public class ESISubstitutionEngine extends TextSubstitutionEngine implements SubstitutionEngine {

	abstract public static class ESIIncludePattern {
		protected Pattern pattern;

		public abstract String translate(String content, String mimeType) throws IncludeException;

		public Pattern getPattern() {
			return pattern;
		}

		public abstract String getSyntaxIdentifier();
	}

	private static class JSESIInclude extends ESIIncludePattern {

		public JSESIInclude() {
			pattern = Pattern.compile("\"ssi:include:virtual:(.*?)\"");
		}

		@Override
		public String translate(String content, String mimeType) {
			if (MimeType.isJS(mimeType))
				return content;
			else if (MimeType.isML(mimeType)) {
				if (content == null || content.equals(""))
					return "null";
				else
					return JSONObject.quote(content);
			}
			return "{}";
		}

		@Override
		public String getSyntaxIdentifier() {
			return "JS";
		}
	}

	private static class MLESIInclude extends ESIIncludePattern {

		public MLESIInclude() {
			pattern = Pattern.compile("<!--# include virtual=\"(.*?)\" -->");
		}

		@Override
		public String translate(String content, String mimeType) throws IncludeException {
			if (MimeType.isML(mimeType))
				return content;
			throw new IncludeException("unsupported ESI type (js* in *ML context)");
		}

		@Override
		public String getSyntaxIdentifier() {
			return "ML";
		}
	}

	private static class JSMLESIInclude extends ESIIncludePattern {

		public JSMLESIInclude() {
			pattern = Pattern.compile("\\\\u003C!--# include virtual=\\\\\"(.*?)\\\\\" --\\\\u003E");
		}

		@Override
		public String translate(String content, String mimeType) throws IncludeException {
			if (MimeType.isML(mimeType)) {
				if (content == null || content.equals(""))
					return "null";
				String json = JSONObject.quote(content);
				return json.substring(1, json.length() - 1);
			}
			throw new IncludeException("unsupported ESI type (js* in js(*ML) context)");
		}

		@Override
		public String getSyntaxIdentifier() {
			return "JS(ML)";
		}
	}

	private static class HttpESIInclude extends ESIIncludePattern {
		public HttpESIInclude() {
			pattern = Pattern.compile("http://esi\\.include\\.virtual(/.*?)#");
		}

		@Override
		public String translate(String content, String mimeType) {
			return content;
		}

		@Override
		public String getSyntaxIdentifier() {
			return "URL";
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
	public void scheduleSubQueries(LackrContentExchange lackrContentExchange, LackrRequest lackrRequest)
	        throws IOException {
		if (!parseable(lackrContentExchange.lackrRequest))
			return;

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
					lackrRequest.scheduleUpstreamRequest(matcher.group(1), HttpMethods.GET, null, lackrContentExchange
					        .getURI(), pattern.getSyntaxIdentifier());
				}
			}
		}
	}

	@Override
	public byte[] generateContent(LackrRequest rootRequest, byte[] byteContent) throws IncludeException {
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
						String fragment = exchange.getResponseContentBytes() == null ? null : new String(exchange
						        .getResponseContentBytes(), "UTF-8");
						replacement = pattern.translate(fragment, exchange.getResponseFields().getStringField(
						        HttpHeaders.CONTENT_TYPE));
						if (replacement == null)
							replacement = "";
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
