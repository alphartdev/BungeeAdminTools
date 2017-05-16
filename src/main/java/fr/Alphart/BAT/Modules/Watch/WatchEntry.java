package fr.Alphart.BAT.Modules.Watch;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WatchEntry {
	private final String entity;
	private final String server;
	private final String reason;
	private final String staff;
	private final Timestamp beginDate;
	private final Timestamp endDate;
	private final Timestamp unwatchDate;
	private final String unwatchReason;
	private final String unwatchStaff;
	private final boolean active;
}
