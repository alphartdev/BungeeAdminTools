package fr.Alphart.BAT.Modules.Kick;

public class KickEntry {
	private final String entity;
	private final String server;
	private final String reason;
	private final String staff;
	private final int date;

	public KickEntry(final String entity, final String server, final String reason, final String staff, final int date) {
		this.entity = entity;
		this.server = server;
		this.reason = reason;
		this.staff = staff;
		this.date = date;
	}

	public String getPlayerName() {
		return entity;
	}

	public String getServer() {
		return server;
	}

	public String getReason() {
		return reason;
	}

	public String getStaff() {
		return staff;
	}

	public int getdate() {
		return date;
	}
}
