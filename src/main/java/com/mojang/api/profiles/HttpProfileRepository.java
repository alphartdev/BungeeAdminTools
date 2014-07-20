package com.mojang.api.profiles;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;
import com.mojang.api.http.BasicHttpClient;
import com.mojang.api.http.HttpBody;
import com.mojang.api.http.HttpClient;
import com.mojang.api.http.HttpHeader;

/**
 * Little modification to avoid checking hundred of time the same profile.
 * @author Mojang
 */
public class HttpProfileRepository implements ProfileRepository {

	private static final int MAX_PAGES_TO_CHECK = 100;
	private static Gson gson = new Gson();
	private final HttpClient client;

	public HttpProfileRepository() {
		this(BasicHttpClient.getInstance());
	}

	public HttpProfileRepository(final HttpClient client) {
		this.client = client;
	}

	@Override
	public Profile[] findProfilesByCriteria(final ProfileCriteria... criteria) {
		try {
			final HttpBody body = new HttpBody(gson.toJson(criteria));
			final List<HttpHeader> headers = new ArrayList<HttpHeader>();
			headers.add(new HttpHeader("Content-Type", "application/json"));
			final List<Profile> profiles = new ArrayList<Profile>();
			for (int i = 1; i <= MAX_PAGES_TO_CHECK; i++) {
				final ProfileSearchResult result = post(new URL("https://api.mojang.com/profiles/page/" + i), body, headers);
				if (result.getSize() == 0) {
					break;
				}
				profiles.addAll(Arrays.asList(result.getProfiles()));
				// Begin: BAT modification
				if(profiles.size() >= criteria.length){
					break;
				}
				// End: BAT modification
			}
			return profiles.toArray(new Profile[profiles.size()]);
		} catch (final Exception e) {
			// TODO: logging and allowing consumer to react?
			return new Profile[0];
		}
	}

	private ProfileSearchResult post(final URL url, final HttpBody body, final List<HttpHeader> headers) throws IOException {
		final String response = client.post(url, body, headers);
		return gson.fromJson(response, ProfileSearchResult.class);
	}

}
