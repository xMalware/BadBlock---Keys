package fr.badblock.badkeys;

import java.sql.ResultSet;

public class Request {

	private String request;
	private RequestType requestType;
	private String[] fields;
	private boolean doNotClose;

	public Request(String request, RequestType requestType, String... fields) {
		this.request = request;
		this.requestType = requestType;
		this.fields = fields;
	}

	public void done(ResultSet resultSet) {

	}

	public String getRequest() {
		return this.request;
	}

	public RequestType getRequestType() {
		return this.requestType;
	}

	public String[] getFields() {
		return this.fields;
	}

	public boolean isDoNotClosed() {
		return this.doNotClose;
	}

	public void setDoNotClose(boolean bool) {
		this.doNotClose = bool;
	}

	public static enum RequestType {
		GETTER, SETTER;
	}

}
