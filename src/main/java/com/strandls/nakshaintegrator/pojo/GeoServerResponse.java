package com.strandls.nakshaintegrator.pojo;

import java.util.Map;

public class GeoServerResponse {

	private byte[] body;
	private Map<String, String> headers;

	public GeoServerResponse(byte[] body, Map<String, String> headers) {
		this.body = body;
		this.headers = headers;
	}

	public byte[] getBody() {
		return body;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

}
