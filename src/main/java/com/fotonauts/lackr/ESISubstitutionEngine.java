package com.fotonauts.lackr;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ESISubstitutionEngine extends TextSubstitutionEngine implements SubstitutionEngine {

	private Pattern[] patterns;
	{
		patterns = new Pattern[2];
		patterns[0] = Pattern.compile("<!--# include virtual=\"(.*?)\" -->");
		patterns[1] = Pattern.compile("\"ssi:include:virtual:(.*)\"");
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
			for (Pattern pattern : patterns) {
				Matcher matcher = pattern.matcher(content);
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
				for(Pattern pattern : patterns) {
					Matcher matcher = pattern.matcher(content);
					if (matcher.find()) {
						String replacement = "";
						LackrContentExchange exchange = rootRequest.fragmentsMap.get(matcher.group(1));
						if (exchange.getResponseContentBytes() != null) {
							replacement = new String(exchange.getResponseContentBytes(), "UTF-8");
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
