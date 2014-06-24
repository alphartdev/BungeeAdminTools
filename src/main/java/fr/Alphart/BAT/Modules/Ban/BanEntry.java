package fr.Alphart.BAT.Modules.Ban;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
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
}