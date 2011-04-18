package com.fotonauts.lackr.esi;


public class JSEscapedMLESIRule extends AbstractJSMLRule {

	public JSEscapedMLESIRule() {
		super("\\u003C!--# include virtual=\\\"*\\\" --\\u003E");
	}


}
