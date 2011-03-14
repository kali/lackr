package com.fotonauts.lackr;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserAgent {

	public enum BrowserName {
		IE, FIREFOX, OPERA, KONQUEROR, WEBKIT, UNKNOWN
	};

	public enum DeviceName {
		IPAD, IPHONE, ANDROID, DESKTOP
	};

	private static Pattern IE_VERSION_PATTERN = Pattern.compile("(?i)msie (\\d+)\\.(\\d+)");
	private static Pattern FIREFOX_VERSION_PATTERN = Pattern.compile("(?i)firefox/(\\d+)\\.(\\d+)");

	private BrowserName browserName;
	private DeviceName deviceName;
	private int majorVersion;
	private int minorVersion;

	private String normalized;

	public UserAgent(String header) {
		String downcase = header.toLowerCase();
		browserName = BrowserName.UNKNOWN;
		if (downcase.indexOf("msie") >= 0 && downcase.indexOf("opera") == -1 && downcase.indexOf("webtv") == -1)
			browserName = BrowserName.IE;
		else if (downcase.indexOf("firefox/") >= 0)
			browserName = BrowserName.FIREFOX;
		else if (downcase.indexOf("opera") >= 0)
			browserName = BrowserName.OPERA;
		else if (downcase.indexOf("konqueror") >= 0)
			browserName = BrowserName.KONQUEROR;
		else if (downcase.indexOf("applewebkit/") >= 0)
			browserName = BrowserName.WEBKIT;

		majorVersion = 0;
		minorVersion = 0;
		Pattern versionPattern = browserName == BrowserName.IE ? IE_VERSION_PATTERN
		        : browserName == BrowserName.FIREFOX ? FIREFOX_VERSION_PATTERN : null;
		if (versionPattern != null) {
			Matcher matcher = versionPattern.matcher(downcase);
			if (matcher.find()) {
				try {
					majorVersion = Integer.parseInt(matcher.group(1));
					minorVersion = Integer.parseInt(matcher.group(2));
				} catch (NumberFormatException exception) {
					/* do nothing */
				}
			}
		}

		deviceName = DeviceName.DESKTOP;
		if (downcase.indexOf("ipad") >= 0)
			deviceName = DeviceName.IPAD;
		else if (downcase.indexOf("iphone") >= 0 || downcase.indexOf("ipod") >= 0)
			deviceName = DeviceName.IPHONE;
		else if (downcase.indexOf("android") >= 0)
			deviceName = DeviceName.ANDROID;

		normalized = (browserName + " " + majorVersion + " " + minorVersion + " " + deviceName).toLowerCase();
	}

	@Override
	public String toString() {
		return normalized;
	}

}
