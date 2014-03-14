package com.mojang.api.http;

public class HttpBody {

	private final String bodyString;

	public HttpBody(final String bodyString) {
		this.bodyString = bodyString;
	}

	public byte[] getBytes() {
		return bodyString != null ? bodyString.getBytes() : new byte[0];
	}

}
