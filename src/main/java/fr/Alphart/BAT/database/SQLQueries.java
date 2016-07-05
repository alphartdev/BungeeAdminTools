package fr.Alphart.BAT.database;

/**
 * This class contains almost all sql queries used by the plugin. Each subclass
 * contains queries handled by a module. Each subclass has another subclass
 * called "SQLite" which provides compatibility with SQLite.
 */
public class SQLQueries {
	public static class Kick {
		public final static String table = "BAT_kick";
		public final static String createTable = "CREATE TABLE IF NOT EXISTS `" + table + "` ("
				+ "`kick_id` INTEGER PRIMARY KEY AUTO_INCREMENT,"
				+ "`UUID` varchar(100) NOT NULL,`kick_staff` varchar(30) NOT NULL,"
				+ "`kick_reason` varchar(100) NULL, `kick_server` varchar(30) NOT NULL,"
				+ "`kick_date` timestamp NOT NULL,"

				+ "INDEX(UUID)" + ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
		public static final String getKick = "SELECT kick_server, kick_reason, kick_staff, kick_date FROM `" + table
				+ "`" + " WHERE UUID = ? ORDER BY kick_date DESC;";
		public static final String getManagedKick = "SELECT kick_server, kick_reason, UUID, kick_date FROM `" + table
				+ "`" + " WHERE kick_staff = ? ORDER BY kick_date DESC;";
		public final static String kickPlayer = "INSERT INTO `" + table
				+ "`(UUID, kick_staff, kick_reason, kick_server, kick_date) VALUES (?, ?, ?, ?, NOW());";

		public static class SQLite {
			public final static String[] createTable = {
				"CREATE TABLE IF NOT EXISTS `" + table + "` (" + "`kick_id` INTEGER PRIMARY KEY AUTOINCREMENT,"
						+ "`UUID` varchar(100) NOT NULL," + "`kick_staff` varchar(30) NOT NULL,"
						+ "`kick_reason` varchar(100) NULL," + "`kick_server` varchar(30) NOT NULL,"
						+ "`kick_date` timestamp NOT NULL" + ");",
						"CREATE INDEX IF NOT EXISTS `kick.uuid_index` ON " + table + " (`UUID`);" };
			public final static String kickPlayer = "INSERT INTO `" + table
					+ "`(UUID, kick_staff, kick_reason, kick_server, kick_date) VALUES (?, ?, ?, ?, date());";
			public static final String getKick = "SELECT kick_server, kick_reason, kick_staff, strftime('%s',kick_date) FROM `" + table
					+ "`" + " WHERE UUID = ? ORDER BY kick_date;";
			public static final String getManagedKick = "SELECT kick_server, kick_reason, UUID, strftime('%s',kick_date) FROM `" + table
					+ "`" + " WHERE kick_staff = ? ORDER BY kick_date;";
		}
	}

	public static class Ban {
		public final static String table = "BAT_ban";
		public final static String createTable = "CREATE TABLE IF NOT EXISTS `" + table + "` ("
				+ "`ban_id` INTEGER PRIMARY KEY AUTO_INCREMENT," + "`UUID` varchar(100) NULL,"
				+ "`ban_ip` varchar(50) NULL,"

				+ "`ban_staff` varchar(30) NOT NULL," + "`ban_reason` varchar(100) NULL,"
				+ "`ban_server` varchar(30) NOT NULL," + "`ban_begin` timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,"
				+ "`ban_end` timestamp NULL," + "`ban_state` bool NOT NULL default 1,"

				+ "`ban_unbandate` timestamp NULL," + "`ban_unbanstaff` varchar(30) NULL,"
				+ "`ban_unbanreason` varchar(100) NULL,"

				+ "INDEX(UUID)," + "INDEX(ban_ip)" + ") ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1;";

		// Use to check if a player is ban on a ban_server
		// Parameter : player, player's ban_ip, (ban_server)
		public static final String isBan = "SELECT ban_id FROM `" + table + "` WHERE ban_state = 1 AND UUID = ?;";
		public static final String isBanServer = "SELECT ban_id FROM `" + table + "` WHERE ban_state = 1 AND UUID = ? "
				+ "AND ban_server = ?;";

		public static final String isBanIP = "SELECT ban_id FROM `" + table
				+ "` WHERE ban_state = 1 AND ban_ip = ? AND UUID IS NULL;";
		public static final String isBanServerIP = "SELECT ban_id FROM `" + table
				+ "` WHERE ban_state = 1 AND ban_ip = ? AND ban_server = ? AND UUID IS NULL;";

		public static final String createBan = "INSERT INTO `" + table
				+ "`(UUID, ban_staff, ban_server, ban_end, ban_reason) VALUES (?, ?, ?, ?, ?);";

		public static final String createBanIP = "INSERT INTO `" + table
				+ "`(ban_ip, ban_staff, ban_server, ban_end, ban_reason) VALUES (?, ?, ?, ?, ?);";

		public static final String unBan = "UPDATE `" + table
				+ "` SET ban_state = 0, ban_unbanreason = ?, ban_unbanstaff = ?, ban_unbandate = NOW() "
				+ "WHERE UUID = ? AND ban_state = 1;";
		public static final String unBanServer = "UPDATE `" + table
				+ "` SET ban_state = 0, ban_unbanreason = ?, ban_unbanstaff = ?, ban_unbandate = NOW() "
				+ "WHERE UUID = ? AND ban_server = ? AND ban_state = 1;";

		public static final String unBanIP = "UPDATE `" + table
				+ "` SET ban_state = 0, ban_unbanreason = ?, ban_unbanstaff = ?, ban_unbandate = NOW()  "
				+ "WHERE ban_ip = ? AND UUID IS NULL;";
		public static final String unBanIPServer = "UPDATE `" + table
				+ "` SET ban_state = 0, ban_unbanreason = ?, ban_unbanstaff = ?, ban_unbandate = NOW()  "
				+ "WHERE ban_ip = ? AND ban_server = ? AND UUID IS NULL;";

		public static final String getBan = "SELECT * FROM `"
				+ table + "`" + " WHERE UUID = ? ORDER BY ban_state DESC, ban_end DESC;";
		public static final String getBanIP = "SELECT * FROM `"
				+ table + "`" + " WHERE ban_ip = ? AND UUID IS NULL ORDER BY ban_state DESC, ban_end DESC;";

		public static final String getManagedBan = "SELECT * FROM `"
				+ table + "`" + " WHERE ban_staff = ? OR ban_unbanstaff = ? ORDER BY ban_state DESC, ban_end DESC;";
		
		public static final String getBanMessage = "SELECT ban_reason, ban_end, ban_staff, ban_begin FROM `" 
				+ table + "` WHERE (UUID = ? OR ban_ip = ?) AND ban_state = 1 AND ban_server = ?;";
		
		public static final String updateExpiredBan = "UPDATE `" + table + "` SET ban_state = 0 "
				+ "WHERE ban_state = 1 AND (ban_end != 0 AND ban_end < NOW());";

		public static class SQLite {
			// Ban related
			public final static String[] createTable = {
				"CREATE TABLE IF NOT EXISTS `" + table + "` (" + "`ban_id` INTEGER PRIMARY KEY AUTOINCREMENT,"
						+ "`UUID` varchar(100) NULL," + "`ban_ip` varchar(50) NULL,"

							+ "`ban_staff` varchar(30) NOT NULL," + "`ban_reason` varchar(100) NULL,"
							+ "`ban_server` varchar(30) NOT NULL,"
							+ "`ban_begin` timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL," + "`ban_end` timestamp NULL,"
							+ "`ban_state` bool NOT NULL default 1,"

							+ "`ban_unbandate` timestamp NULL," + "`ban_unbanstaff` varchar(30) NULL,"
							+ "`ban_unbanreason` varchar(100) NULL" + ");",
							"CREATE INDEX IF NOT EXISTS `ban.uuid_index` ON " + table + " (`UUID`);",
							"CREATE INDEX IF NOT EXISTS `ban.ip_index` ON " + table + " (`ban_ip`);" };
			
			public static final String unBan = "UPDATE `" + table
					+ "` SET ban_state = 0, ban_unbanreason = ?, ban_unbanstaff = ?, ban_unbandate = datetime() "
					+ "WHERE UUID = ? AND ban_state = 1;";
			public static final String unBanIP = "UPDATE `" + table
					+ "` SET ban_state = 0, ban_unbanreason = ?, ban_unbanstaff = ?, ban_unbandate = datetime()  "
					+ "WHERE ban_ip = ? AND UUID IS NULL;";
			public static final String unBanIPServer = "UPDATE `" + table
					+ "` SET ban_state = 0, ban_unbanreason = ?, ban_unbanstaff = ?, ban_unbandate = datetime()  "
					+ "WHERE ban_ip = ? AND ban_server = ? AND UUID IS NULL;";
			public static final String unBanServer = "UPDATE `" + table
					+ "` SET ban_state = 0, ban_unbanreason = ?, ban_unbanstaff = ?, ban_unbandate = datetime() "
					+ "WHERE UUID = ? AND ban_server = ? AND ban_state = 1;";
			
			public static final String getBan = "SELECT *, "
					+ "strftime('%s',ban_begin), strftime('%s',ban_end), strftime('%s',ban_unbandate) "
					+ "FROM `" + table + "`" + " WHERE UUID = ? ORDER BY ban_state DESC, ban_end DESC;";
			public static final String getBanIP = "SELECT *, "
					+ "strftime('%s',ban_begin), strftime('%s',ban_end), strftime('%s',ban_unbandate) "
					+ "FROM `" + table + "`" + " WHERE ban_ip = ? AND UUID IS NULL ORDER BY ban_state DESC, ban_end DESC;";
			
			public static final String getBanMessage = "SELECT ban_reason, ban_staff, ban_end, strftime('%s',ban_begin) FROM `" 
					+ table + "` WHERE (UUID = ? OR ban_ip = ?) AND ban_state = 1 AND ban_server = ?;";
			
			public static final String getManagedBan = "SELECT *, "
					+ "strftime('%s',ban_begin), strftime('%s',ban_end), strftime('%s',ban_unbandate) "
					+ "FROM `" + table + "`" + " WHERE ban_staff = ? OR ban_unbanstaff = ? ORDER BY ban_state DESC, ban_end DESC;";
			
			public static final String updateExpiredBan = "UPDATE `" + table + "` SET ban_state = 0 "
					+ "WHERE ban_state = 1 AND (ban_end != 0 AND (ban_end / 1000) < CAST(strftime('%s', 'now') as integer));";
		}
	}

	public static class Mute {
		public final static String table = "BAT_mute";
		public final static String createTable = "CREATE TABLE IF NOT EXISTS `" + table + "` ("
				+ "`mute_id` INTEGER PRIMARY KEY AUTO_INCREMENT," + "`UUID` varchar(100) NULL,"
				+ "`mute_ip` varchar(50) NULL,"

				+ "`mute_staff` varchar(30) NOT NULL," + "`mute_reason` varchar(100) NULL,"
				+ "`mute_server` varchar(30) NOT NULL," + "`mute_begin` timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,"
				+ "`mute_end` timestamp NULL," + "`mute_state` bool NOT NULL default 1,"

				+ "`mute_unmutedate` timestamp NULL," + "`mute_unmutestaff` varchar(30) NULL,"
				+ "`mute_unmutereason` varchar(100) NULL,"

				+ "INDEX(UUID)," + "INDEX(mute_ip)" + ") ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1;";

		public static final String isMute = "SELECT mute_id FROM `" + table + "` WHERE mute_state = 1 AND UUID = ?;";
		public static final String isMuteServer = "SELECT mute_id FROM `" + table
				+ "` WHERE mute_state = 1 AND UUID = ? " + "AND mute_server = ?;";

		public static final String isMuteIP = "SELECT mute_id FROM `" + table
				+ "` WHERE mute_state = 1 AND mute_ip = ?  AND UUID IS NULL;";
		public static final String isMuteServerIP = "SELECT mute_id FROM `" + table
				+ "` WHERE mute_state = 1 AND mute_ip = ? AND mute_server = ? AND UUID IS NULL;";

		public static final String createMute = "INSERT INTO `" + table
				+ "`(UUID, mute_staff, mute_server, mute_end, mute_reason) VALUES (?, ?, ?, ?, ?);";

		public static final String createMuteIP = "INSERT INTO `" + table
				+ "`(mute_ip, mute_staff, mute_server, mute_end, mute_reason) VALUES (?, ?, ?, ?, ?);";

		public static final String unMute = "UPDATE `" + table
				+ "` SET mute_state = 0, mute_unmutereason = ?, mute_unmutestaff = ?, mute_unmutedate = NOW() "
				+ "WHERE UUID = ? AND mute_state = 1;";
		public static final String unMuteServer = "UPDATE `" + table
				+ "` SET mute_state = 0, mute_unmutereason = ?, mute_unmutestaff = ?, mute_unmutedate = NOW() "
				+ "WHERE UUID = ? AND mute_server = ? AND mute_state = 1;";

		public static final String unMuteIP = "UPDATE `" + table
				+ "` SET mute_state = 0, mute_unmutereason = ?, mute_unmutestaff = ?, mute_unmutedate = NOW()  "
				+ "WHERE mute_ip = ? AND UUID IS NULL;";
		public static final String unMuteIPServer = "UPDATE `" + table
				+ "` SET mute_state = 0, mute_unmutereason = ?, mute_unmutestaff = ?, mute_unmutedate = NOW()  "
				+ "WHERE mute_ip = ? AND mute_server = ? AND UUID IS NULL;";

		public static final String getMute = "SELECT * FROM `"
				+ table + "`" + " WHERE UUID = ? ORDER BY mute_state DESC, mute_end DESC;";
		public static final String getMuteIP = "SELECT * FROM `"
				+ table + "`" + " WHERE mute_ip = ? AND UUID IS NULL ORDER BY mute_state DESC, mute_end DESC;";

		public static final String getManagedMute = "SELECT * FROM `"
				+ table + "`" + " WHERE mute_staff = ? OR mute_unmutestaff = ? ORDER BY mute_state DESC, mute_end DESC;";
		
		public static final String getMuteMessage = "SELECT mute_reason, mute_end, mute_staff, mute_begin FROM `" 
				+ table + "` WHERE (UUID = ? OR mute_ip = ?) AND mute_state = 1 AND mute_server = ?;";
		
		public static final String updateExpiredMute = "UPDATE `" + table + "` SET mute_state = 0 "
				+ "WHERE mute_state = 1 AND (mute_end != 0 AND mute_end < NOW());";

		public static class SQLite {
			public final static String[] createTable = {
				"CREATE TABLE IF NOT EXISTS `" + table + "` (" + "`mute_id` INTEGER PRIMARY KEY AUTOINCREMENT,"
						+ "`UUID` varchar(100) NULL," + "`mute_ip` varchar(50) NULL,"

							+ "`mute_staff` varchar(30) NOT NULL," + "`mute_reason` varchar(100) NULL,"
							+ "`mute_server` varchar(30) NOT NULL,"
							+ "`mute_begin` timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,"
							+ "`mute_end` timestamp NULL," + "`mute_state` bool NOT NULL default 1,"

							+ "`mute_unmutedate` timestamp NULL," + "`mute_unmutestaff` varchar(30) NULL,"
							+ "`mute_unmutereason` varchar(100) NULL" + ");",
							"CREATE INDEX IF NOT EXISTS `mute.uuid_index` ON " + table + " (`UUID`);",
							"CREATE INDEX IF NOT EXISTS `mute.ip_index` ON " + table + " (`mute_ip`);" };

			public static final String unMute = "UPDATE `"
					+ table
					+ "` SET mute_state = 0, mute_unmutereason = ?, mute_unmutestaff = ?, mute_unmutedate = datetime() "
					+ "WHERE UUID = ? AND mute_state = 1;";
			public static final String unMuteServer = "UPDATE `"
					+ table
					+ "` SET mute_state = 0, mute_unmutereason = ?, mute_unmutestaff = ?, mute_unmutedate = datetime() "
					+ "WHERE UUID = ? AND mute_server = ? AND mute_state = 1;";
			public static final String unMuteIP = "UPDATE `"
					+ table
					+ "` SET mute_state = 0, mute_unmutereason = ?, mute_unmutestaff = ?, mute_unmutedate = datetime()  "
					+ "WHERE mute_ip = ? AND UUID IS NULL;";
			public static final String unMuteIPServer = "UPDATE `"
					+ table
					+ "` SET mute_state = 0, mute_unmutereason = ?, mute_unmutestaff = ?, mute_unmutedate = datetime()  "
					+ "WHERE mute_ip = ? AND mute_server = ? AND UUID IS NULL;";

			public static final String getMute = "SELECT *, "
					+ "strftime('%s',mute_begin), strftime('%s',mute_end), strftime('%s',mute_unmutedate)"
					+ "FROM `" + table + "`" + " WHERE UUID = ? ORDER BY mute_state DESC, mute_end DESC;";
			public static final String getMuteIP = "SELECT *, "
					+ "strftime('%s',mute_begin), strftime('%s',mute_end), strftime('%s',mute_unmutedate)"
					+ "FROM `" + table + "`" + " WHERE mute_ip = ? AND UUID IS NULL ORDER BY mute_state DESC, mute_end DESC;";
			
			public static final String getManagedMute = "SELECT *, "
					+ "strftime('%s',mute_begin), strftime('%s',mute_end), strftime('%s',mute_unmutedate) "
					+ "FROM `" + table + "`" + " WHERE mute_staff = ? OR mute_unmutestaff = ? ORDER BY mute_state DESC, mute_end DESC;";
			
			public static final String getMuteMessage = "SELECT mute_reason, mute_staff, strftime('%s',mute_begin), mute_end FROM `" 
					+ table + "` WHERE (UUID = ? OR mute_ip = ?) AND mute_state = 1 AND mute_server = ?;";
			
			public static final String updateExpiredMute = "UPDATE `" + table + "` SET mute_state = 0 "
					+ "WHERE mute_state = 1 AND mute_end != 0 AND (mute_end / 1000) < CAST(strftime('%s', 'now') as integer);";
		}
	}

	public static class Comments{
		public static final String table = "bat_comments";
		
		public static final String createTable = "CREATE TABLE IF NOT EXISTS `" + table + "` ("
		+ "`id` int(11) NOT NULL AUTO_INCREMENT,"
		+ "`entity` varchar(100) NOT NULL,"
		+ "`note` varchar(255) NOT NULL,"
		+ "`type` varchar(7) NOT NULL,"
		+ "`staff` varchar(30) NOT NULL,"
		+ "`date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,"
		+ "PRIMARY KEY (`id`),"
		+ "INDEX(entity)"
		+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1;";
		
		public static final String insertEntry = "INSERT INTO `" + table + "` (entity, note, type, staff)"
				+ "VALUES (?, ?, ?, ?);";
		
		public static final String getEntries = "SELECT id, note, type, staff, date FROM `" + table + "` "
				+ "WHERE entity = ? ORDER BY date DESC;";
		public static final String getManagedEntries = "SELECT id, note, type, date, entity FROM `" + table + "` "
				+ "WHERE staff = ? ORDER BY date DESC;";
		
		public static final String getMostRecentCommentDate = "SELECT date FROM `" + table + "` WHERE entity = ? ORDER BY date DESC";
		
		public static final String clearEntries = "DELETE FROM `" + table + "` WHERE entity = ?;";
		
		public static final String clearByID = "DELETE FROM `" + table + "` WHERE entity = ? AND id = ?;";
		
		public static final String simpleTriggerCheck = "SELECT COUNT(*) FROM `" + table + "` WHERE entity = ?;";
		public static final String patternTriggerCheck = "SELECT COUNT(*) FROM `" + table + "` WHERE entity = ? && note LIKE ?;";
		
		public static class SQLite{
			public static final String createTable[] = {
				"CREATE TABLE IF NOT EXISTS `" + table + "` ("
				+ "`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ "`entity` varchar(100) NOT NULL,"
				+ "`note` varchar(255) NOT NULL,"
				+ "`type` varchar(7) NOT NULL,"
				+ "`staff` varchar(30) NOT NULL,"
				+ "`date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP"
				+ ");",
				"CREATE INDEX IF NOT EXISTS `comments.entity_index` ON " + table + " (`entity`);" };
			
			public static final String getEntries = "SELECT id, note, type, staff, strftime('%s',date) FROM `" + table + "` "
					+ "WHERE entity = ? ORDER BY date DESC;";
			public static final String getManagedEntries = "SELECT id, note, type, strftime('%s',date), entity FROM `" + table + "` "
					+ "WHERE staff = ? ORDER BY date DESC;";
		}
	}
	
	public static class Core {
		public static final String table = "BAT_players";

		public static final String createTable = "CREATE TABLE IF NOT EXISTS `" + table + "` ("
				+ "`BAT_player` varchar(30) UNIQUE NOT NULL," + "`UUID` varchar(100) UNIQUE NOT NULL,"
				+ "`lastip` varchar(50) NOT NULL," + "`firstlogin` timestamp NULL,"
				+ "`lastlogin` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
				+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
		public static final String updateIPUUID = "INSERT INTO `" + table + "` (BAT_player, lastip, firstlogin, UUID)"
				+ " VALUES (?, ?, NOW(), ?) ON DUPLICATE KEY UPDATE lastip = ?, lastlogin = NOW(), BAT_player = ?;";

		public static final String getPlayerName = "SELECT BAT_player FROM `" + table + "` WHERE UUID = ?;";
		
		public static final String getIP = "SELECT lastip FROM `" + table + "` WHERE UUID = ?;";

		public static final String getUUID = "SELECT UUID FROM `" + table + "` WHERE BAT_player = ?;";

		public static final String getPlayerData = "SELECT lastip, firstlogin, lastlogin FROM `" + table
				+ "` WHERE UUID = ?;";

		public static final String getIpUsers = "SELECT BAT_player FROM `" + table + "` WHERE lastip = ?";

		public static class SQLite {
			public static final String createTable[] = {
				"CREATE TABLE IF NOT EXISTS `" + table + "` (" + "`BAT_player` varchar(30) NOT NULL,"
						+ "`UUID` varchar(100) UNIQUE NOT NULL," + "`lastip` varchar(50) NOT NULL,"
						+ "`firstlogin` timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,"
						+ "`lastlogin` timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL" + ");",
						"CREATE INDEX IF NOT EXISTS `core.player_index` ON " + table + " (`BAT_player`);" };
			public static final String updateIPUUID = "INSERT OR REPLACE INTO `" + table
					+ "` (BAT_player, lastip, firstlogin, lastlogin, UUID)"
					+ " VALUES (?, ?, (SELECT firstlogin FROM `" + table + "` WHERE UUID = ?), DATETIME(), ?);";
			public static final String getPlayerData = "SELECT strftime('%s',firstlogin), strftime('%s',lastlogin), lastip FROM `"
					+ table + "` WHERE UUID = ?;";
		}
	}
}