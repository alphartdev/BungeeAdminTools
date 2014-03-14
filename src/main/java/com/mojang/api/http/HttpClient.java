package com.mojang.api.http;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.util.List;

public interface HttpClient {
	public String post(final URL url, final HttpBody body, final List<HttpHeader> headers) throws IOException;
	public String post(final URL url, final Proxy proxy, final HttpBody body, final List<HttpHeader> headers) throws IOException;
}
