package com.fotonauts.lackr.interpolr.esi;


public class JSEscapedMLESIRule extends AbstractJSMLRule {

	public JSEscapedMLESIRule() {
		super("\\u003C!--# include virtual=\\\"*\\\" --\\u003E");
	}


}
