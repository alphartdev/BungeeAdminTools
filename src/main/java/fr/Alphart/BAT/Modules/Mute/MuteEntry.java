package fr.Alphart.BAT.Modules.Mute;

import java.sql.Timestamp;

public class MuteEntry {
	private final String entity;
	private final String server;
	private final String reason;
	private final String staff;
	private final Timestamp beginDate;
	private final Timestamp endDate;
	private final Timestamp unmuteDate;
	private final String unmuteReason;
	private final String unmuteStaff;
	private final boolean active;

	public MuteEntry(String entity, String server, String reason, String staff, Timestamp beginDate, Timestamp endDate, Timestamp unmuteDate, String unmuteReason, String unmuteStaff, boolean active) {
		this.entity = entity;
		this.server = server;
		this.reason = reason;
		this.staff = staff;
		this.beginDate = beginDate;
		this.endDate = endDate;
		this.unmuteDate = unmuteDate;
		this.unmuteReason = unmuteReason;
		this.unmuteStaff = unmuteStaff;
		this.active = active;
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

	public Timestamp getBeginDate() {
		return this.beginDate;
	}

	public Timestamp getEndDate() {
		return this.endDate;
	}

	public Timestamp getUnmuteDate() {
		return this.unmuteDate;
	}

	public String getUnmuteReason() {
		return this.unmuteReason;
	}

	public String getUnmuteStaff() {
		return this.unmuteStaff;
	}

	public boolean getActive() {
		return this.active;
	}

	public boolean isActive() {
		return this.active;
	}

}