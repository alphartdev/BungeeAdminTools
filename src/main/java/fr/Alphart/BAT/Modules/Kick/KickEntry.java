package fr.Alphart.BAT.Modules.Kick;

import java.sql.Timestamp;

public class KickEntry {
	private final String entity;
	private final String server;
	private final String reason;
	private final String staff;
	private final Timestamp date;

	public KickEntry(String entity, String server, String reason, String staff, Timestamp date) {
		this.entity = entity;
		this.server = server;
		this.reason = reason;
		this.staff = staff;
		this.date = date;
	}

	public String getEntity() {
		return this.entity;
	}

	public String getServer() {
		return this.server;
	}

	public String getReason() {
		return this.reason;
	}

	public String getStaff() {
		return this.staff;
	}

	public Timestamp getDate() {
		return this.date;
	}
}