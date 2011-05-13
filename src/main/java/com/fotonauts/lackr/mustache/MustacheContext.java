package com.fotonauts.lackr.mustache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fotonauts.lackr.LackrPresentableError;
import com.fotonauts.lackr.interpolr.Document;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.MustacheParseException;
import com.samskivert.mustache.Template;

public class MustacheContext {

	private Map<String, Document> registeredTemplatesDocument;
	private Map<String, Template> compiledTemplates;

	public MustacheContext() {
		registeredTemplatesDocument = Collections.synchronizedMap(new HashMap<String, Document>());
		compiledTemplates = Collections.synchronizedMap(new HashMap<String, Template>());
	}

	public void checkAndCompileAll(List<Throwable> exceptions) {
		for (Entry<String, Document> registered : registeredTemplatesDocument.entrySet()) {
			registered.getValue().check(exceptions);
			String expanded = getExpandedTemplate(registered.getKey());
			try {
				compiledTemplates.put(registered.getKey(), Mustache.compiler().compile(expanded));
			} catch (MustacheParseException e) {
				StringBuilder builder = new StringBuilder();
				builder.append("MustacheParseException\n");
				builder.append(e.getMessage() + "\n");
				builder.append("template name: " + registered.getKey() + "\n");
				String lines[] = expanded.split("\n");
				for (int i = 0; i < lines.length; i++)
					builder.append(String.format("% 3d %s\n", i + 1, lines[i]));
				builder.append("\n");
				exceptions.add(new LackrPresentableError(builder.toString()));
			}
		}
	}

	public void registerTemplate(String name, Document template) {
		registeredTemplatesDocument.put(name, template);
	}

	public Template get(String templateName) {
		return compiledTemplates.get(templateName);
	}

	public String getExpandedTemplate(String name) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			registeredTemplatesDocument.get(name).writeTo(baos);
			return baos.toString("UTF-8");
		} catch (IOException e) {
			return null;
		}
	}

	public Document getTemplate(String name) {
		return registeredTemplatesDocument.get(name);
	}

	public String[] getAllNames() {
		return (String[]) registeredTemplatesDocument.keySet().toArray(new String[0]);
	}
}
