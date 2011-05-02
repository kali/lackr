package com.fotonauts.lackr.mustache;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

public class MustacheContext {

	private Map<String, String> registeredTemplates;
	private Map<String, Template> templates;

	public MustacheContext() {
		registeredTemplates = Collections.synchronizedMap(new HashMap<String, String>());
		templates = Collections.synchronizedMap(new HashMap<String, Template>());
	}

	public void registerTemplate(String name, String template) {
		registeredTemplates.put(name, template);
		templates.put(name, Mustache.compiler().compile(template));
	}
	
	public String eval(String templateName, Object data) {
		return templates.get(templateName).execute(data);
	}
}
