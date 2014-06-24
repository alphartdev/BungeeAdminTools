package fr.Alphart.BAT.Modules.Mute;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
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
}
