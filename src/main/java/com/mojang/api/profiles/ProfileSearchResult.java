package com.mojang.api.profiles;

public class ProfileSearchResult {
	private Profile[] profiles;
	private int size;

	public Profile[] getProfiles() {
		return profiles;
	}

	public void setProfiles(final Profile[] profiles) {
		this.profiles = profiles;
	}

	public int getSize() {
		return size;
	}

	public void setSize(final int size) {
		this.size = size;
	}
}
