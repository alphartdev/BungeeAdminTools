package fr.Alphart.BAT.Modules.Ban;

import java.sql.Timestamp;

public class BanEntry {
	private final String entity;
	private final String server;
	private final String reason;
	private final String staff;
	private final Timestamp beginDate;
	private final Timestamp endDate;
	private final Timestamp unbanDate;
	private final String unbanReason;
	private final String unbanStaff;
	private final boolean active;

	public BanEntry(String entity, String server, String reason, String staff, Timestamp beginDate, Timestamp endDate, Timestamp unbanDate, String unbanReason, String unbanStaff, boolean active) {
		this.entity = entity;
		this.server = server;
		this.reason = reason;
		this.staff = staff;
		this.beginDate = beginDate;
		this.endDate = endDate;
		this.unbanDate = unbanDate;
		this.unbanReason = unbanReason;
		this.unbanStaff = unbanStaff;
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

	public Timestamp getUnbanDate() {
		return this.unbanDate;
	}

	public String getUnbanReason() {
		return this.unbanReason;
	}

	public String getUnbanStaff() {
		return this.unbanStaff;
	}

	public boolean getActive() {
		return this.active;
	}

	public boolean isActive() {
		return this.active;
	}

}