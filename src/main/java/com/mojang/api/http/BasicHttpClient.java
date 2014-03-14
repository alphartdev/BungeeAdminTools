package com.mojang.api.http;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.List;

/*
    TODO: refactor so unit tests can be written :)
 */
public class BasicHttpClient implements HttpClient {

	private static BasicHttpClient instance;

	private BasicHttpClient() {
	}

	public static BasicHttpClient getInstance() {
		if (instance == null) {
			instance = new BasicHttpClient();
		}
		return instance;
	}

	@Override
	public String post(final URL url, final HttpBody body, final List<HttpHeader> headers) throws IOException {
		return post(url, null, body, headers);
	}

	@Override
	public String post(final URL url, Proxy proxy, final HttpBody body, final List<HttpHeader> headers) throws IOException {
		if (proxy == null) {
			proxy = Proxy.NO_PROXY;
		}
		final HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
		connection.setRequestMethod("POST");

		for (final HttpHeader header : headers) {
			connection.setRequestProperty(header.getName(), header.getValue());
		}

		connection.setUseCaches(false);
		connection.setDoInput(true);
		connection.setDoOutput(true);

		final DataOutputStream writer = new DataOutputStream(connection.getOutputStream());
		writer.write(body.getBytes());
		writer.flush();
		writer.close();

		final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String line;
		final StringBuffer response = new StringBuffer();

		while ((line = reader.readLine()) != null) {
			response.append(line);
			response.append('\r');
		}

		reader.close();
		return response.toString();
	}
}
