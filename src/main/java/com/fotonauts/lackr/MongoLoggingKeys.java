/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.fotonauts.lackr;

/**
 * 
 * @author oct
 */
public enum MongoLoggingKeys {
	// Global information about the log
	OPERATION_ID("operation_id"), FACILITY("facility"), INSTANCE("instance"),
	// Current user information
	SSL("ssl"), HTTP_HOST("http_host"), REMOTE_ADDR("remote_addr"), USER_AGENT("user_agent"), USERNAME("username"), CLIENT_ID(
	        "client_id"), USER_ID("user_id"), SESSION_ID("session_id"), LOGIN_SESSION("login_session"),
	// Current Request
	METHOD("method"), PATH("path"), QUERY_PARMS("query_parms"), REFERER("referer"), PARENT("parent"),
	// Result about the request
	STATUS("status"), SIZE("size"), ELAPSED("elapsed"), DATE("date"), DATA("data");

	protected String prettyName;

	MongoLoggingKeys(String prettyName) {
		this.prettyName = prettyName;
	}

	/**
	 * @return the prettyName
	 */
	public String getPrettyName() {
		return prettyName;
	}

	/**
	 * @param prettyName
	 *            the prettyName to set
	 */
	public void setPrettyName(String prettyName) {
		this.prettyName = prettyName;
	}
}
