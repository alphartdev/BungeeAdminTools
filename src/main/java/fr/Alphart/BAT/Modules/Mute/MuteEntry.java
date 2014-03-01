package fr.Alphart.BAT.Modules.Mute;

public class MuteEntry {
	private final String entity;
	private final String server;
	private final String reason;
	private final String staff;
	private final int begin_date;
	private final int end_date;
	private final boolean active;

	public MuteEntry(final String entity, final String server, final String reason, final String staff, final int begin_date, final int end_date, final boolean isActive) {
		this.entity = entity;
		this.server = server;
		this.reason = reason;
		this.staff = staff;
		this.begin_date = begin_date;
		this.end_date = end_date;
		this.active = isActive;
	}

	public String getEntity() {
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

	public int getBegin_date() {
		return begin_date;
	}

	public int getEnd_date() {
		return end_date;
	}

	public boolean isActive(){
		return active;
	}

}
