package com.fotonauts.lackr;

public class BackendRequest {

	private final byte[] body;

	public static enum Target {
		PICOR, FEMTOR
	};
	
	private final Target target;
	private final String method;
	private final String parent;
	private final int parentId;
	private final String query;
	private final LackrFrontendRequest frontendRequest;

	private final String syntax;

	public BackendRequest(Target target, LackrFrontendRequest frontendRequest, String method,
			String query, String parent, int parentId, String syntax,
			byte[] body) {
		super();
		this.target = target;
		this.frontendRequest = frontendRequest;
		this.method = method;
		this.query = query;
		this.parent = parent;
		this.parentId = parentId;
		this.syntax = syntax;
		this.body = body;
	}

	public byte[] getBody() {
		return body;
	}

	public String getMethod() {
		return method;
	}

	public String getParent() {
		return parent;
	}

	public int getParentId() {
		return parentId;
	}

	public String getQuery() {
		return query;
	}

	public LackrFrontendRequest getFrontendRequest() {
		return frontendRequest;
	}

	public String getSyntax() {
		return syntax;
	}

	public String getPath() {
		return query.indexOf('?') == -1 ? query : query.substring(0, query.indexOf('?'));
	}

	public String getParams() {
		return query.indexOf('?') == -1 ? null : query.substring(query.indexOf('?') + 1);
	}

	public Object getTarget() {
	    return target;
    }
}
