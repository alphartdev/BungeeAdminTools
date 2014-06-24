package fr.Alphart.BAT.Modules.Kick;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class KickEntry {
	private final String entity;
	private final String server;
	private final String reason;
	private final String staff;
	private final Timestamp date;
}