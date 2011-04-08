package com.fotonauts.lackr.interpolr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import junit.framework.TestCase;

public class BaseTestSubstitution extends TestCase {
	protected Interpolr inter;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	protected Document parse(String data) {
		try {
			return inter.parse(data.getBytes("UTF-8"), null);
		} catch (UnsupportedEncodingException e) {
			// no way
		}
		return null;
	}

	protected String expand(Document chunks) {
		try {
			int length = chunks.length();
			ByteArrayOutputStream baos = new ByteArrayOutputStream(length);
			chunks.writeTo(baos);
			byte[] bytes = baos.toByteArray();
			assertEquals("result length computation is fine", length, bytes.length);
			return new String(bytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// no way
		} catch (IOException e) {
			// now way here
		}
		return null;
	}

}
