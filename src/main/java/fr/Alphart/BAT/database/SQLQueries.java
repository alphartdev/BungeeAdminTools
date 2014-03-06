package fr.Alphart.BAT.database;

/**
 * This class contains almost all sql queries used by the plugin.
 * Each subclass contains queries handled by a module.
 * Each subclass has another subclass called "SQLite" which provides compatibility with SQLite.
 */
public class SQLQueries {

	// TODO: Convert Kick module to UUID
	public static class Kick{
		public final static String table = "BAT_kick";
		public final static String createTable = 
				"CREATE TABLE IF NOT EXISTS `" + table +"` ("
						+"`kick_id` INTEGER PRIMARY KEY AUTO_INCREMENT,"
						+"`UUID` varchar(100) NOT NULL,"
						+"`kick_ip` varchar(50) NOT NULL,"
						+"`kick_staff` varchar(30) NOT NULL,"
						+"`kick_reason` varchar(100) NULL,"
						+"`kick_server` varchar(30) NOT NULL,"
						+"`kick_date` timestamp NOT NULL,"

		+"INDEX(UUID)"
		+") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
		public static final String getKick = "SELECT kick_server, kick_reason, kick_staff, kick_date FROM `" + table + "`"
				+ " WHERE UUID = ?;";
		public final static String kickPlayer = "INSERT INTO `" + table
				+ "`(UUID, kick_ip, kick_staff, kick_reason, kick_server, kick_date) VALUES (?, ?, ?, ?, ?, NOW());";

		public static class SQLite{
			public final static String[] createTable = {
				"CREATE TABLE IF NOT EXISTS `" + table +"` ("
						+"`kick_id` INTEGER PRIMARY KEY AUTOINCREMENT,"
						+"`UUID` varchar(100) NOT NULL,"
						+"`kick_ip` varchar(50) NOT NULL,"
						+"`kick_staff` varchar(30) NOT NULL,"
						+"`kick_reason` varchar(100) NULL,"
						+"`kick_server` varchar(30) NOT NULL,"
						+"`kick_date` timestamp NOT NULL"
						+");",
						"CREATE INDEX IF NOT EXISTS `kick.uuid_index` ON " + table + " (`UUID`);"
			};
			public final static String kickPlayer = "INSERT INTO `" + table
					+ "`(UUID, kick_ip, kick_staff, kick_reason, kick_server, kick_date) VALUES (?, ?, ?, ?, ?, date());";
		}
	}

	public static class Ban{
		public final static String table = "BAT_ban";
		public final static String createTable = 
				"CREATE TABLE IF NOT EXISTS `" + table + "` (" 
						+ "`ban_id` INTEGER PRIMARY KEY AUTO_INCREMENT,"
						+ "`UUID` varchar(100) NULL," 
						+ "`ban_ip` varchar(50) NULL,"

			+ "`ban_staff` varchar(30) NOT NULL,"
			+ "`ban_reason` varchar(100) NULL,"
			+ "`ban_server` varchar(30) NOT NULL,"
			+ "`ban_begin` timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,"
			+ "`ban_end` timestamp NULL,"
			+ "`ban_state` bool NOT NULL default 1,"

			+ "`ban_unbandate` timestamp NULL,"
			+ "`ban_unbanstaff` varchar(30) NULL,"
			+ "`ban_unbanreason` varchar(100) NULL,"

			+ "INDEX(UUID)," 
			+ "INDEX(ban_ip)"
			+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1;";

		// Use to check if a player is ban on a ban_server
		// Parameter : player, player's ban_ip, (ban_server)
		public static final String isBan = "SELECT ban_id FROM `" + table + "` WHERE ban_state = 1 AND (UUID = ? OR "
				+ "? IN (SELECT ban_ip FROM `" + table + "` WHERE ban_state = 1 AND UUID IS NULL));";
		public static final String isBanServer = "SELECT ban_id FROM `" + table + "` WHERE ban_state = 1 AND (UUID = ? OR "
				+ "? IN (SELECT ban_ip FROM `" + table + "` WHERE ban_state = 1 AND UUID IS NULL)) AND"
				+ " ban_server = ?;";

		public static final String isBanIP = "SELECT ban_id FROM `" + table + "` WHERE ban_state = 1 AND ban_ip = ?;";
		public static final String isBanServerIP = "SELECT ban_id FROM `" + table + "` WHERE ban_state = 1 AND ban_ip = ? AND ban_server = ?;";

		public static final String createBan = "INSERT INTO `" + table
				+ "`(UUID, ban_ip, ban_staff, ban_server, ban_end, ban_reason) VALUES (?, ?, ?, ?, ?, ?);";

		public static final String createBanIP = "INSERT INTO `" + table
				+ "`(ban_ip, ban_staff, ban_server, ban_end, ban_reason) VALUES (?, ?, ?, ?, ?);";

		public static final String unBan = "UPDATE `" + table + "` SET ban_state = 0, ban_unbanreason = ?, ban_unbanstaff = ?, ban_unbandate = NOW() "
				+ "WHERE UUID = ? AND ban_state = 1;";
		public static final String unBanServer = "UPDATE `" + table + "` SET ban_state = 0, ban_unbanreason = ?, ban_unbanstaff = ?, ban_unbandate = NOW() "
				+ "WHERE UUID = ? AND ban_server = ? AND ban_state = 1;";

		public static final String unBanIP = "UPDATE `" + table + "` SET ban_state = 0, ban_unbanreason = ?, ban_unbanstaff = ?, ban_unbandate = NOW()  "
				+ "WHERE ban_ip = ? AND UUID IS NULL;";
		public static final String unBanIPServer = "UPDATE `" + table + "` SET ban_state = 0, ban_unbanreason = ?, ban_unbanstaff = ?, ban_unbandate = NOW()  "
				+ "WHERE ban_ip = ? AND ban_server = ? AND UUID IS NULL;";

		public static final String getBan = "SELECT ban_server, ban_reason, ban_staff, ban_begin, ban_end, ban_state FROM `" + table + "`"
				+ " WHERE UUID = ? OR ban_ip = ?;";
		public static final String getBanIP = "SELECT ban_server, ban_reason, ban_staff, ban_begin, ban_end, ban_state FROM `" + table + "`"
				+ " WHERE ban_ip = ?;";

		public static final String updateExpiredBan = "UPDATE `" + table + "` SET ban_state = 0 "
				+ "WHERE ban_state = 1 AND (ban_end != 0 AND ban_end < NOW());";

		public static class SQLite{
			// Ban related
			public final static String[] createTable = {
				"CREATE TABLE IF NOT EXISTS `" + table + "` (" 
						+ "`ban_id` INTEGER PRIMARY KEY AUTOINCREMENT,"
						+ "`UUID` varchar(100) NULL," 
						+ "`ban_ip` varchar(50) NULL,"

				+ "`ban_staff` varchar(30) NOT NULL,"
				+ "`ban_reason` varchar(100) NULL,"
				+ "`ban_server` varchar(30) NOT NULL,"
				+ "`ban_begin` timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,"
				+ "`ban_end` timestamp NULL,"
				+ "`ban_state` bool NOT NULL default 1,"

				+ "`ban_unbandate` timestamp NULL,"
				+ "`ban_unbanstaff` varchar(30) NULL,"
				+ "`ban_unbanreason` varchar(100) NULL"
				+ ");",
				"CREATE INDEX IF NOT EXISTS `ban.uuid_index` ON " + table + " (`UUID`);",
				"CREATE INDEX IF NOT EXISTS `ban.ip_index` ON " + table + " (`ban_ip`);"
			};
			public static final String unBan = "UPDATE `" + table + "` SET ban_state = 0, ban_unbanreason = ?, ban_unbanstaff = ?, ban_unbandate = datetime() "
					+ "WHERE UUID = ? AND ban_state = 1;";
			public static final String unBanIP = "UPDATE `" + table + "` SET ban_state = 0, ban_unbanreason = ?, ban_unbanstaff = ?, ban_unbandate = datetime()  "
					+ "WHERE ban_ip = ? AND UUID IS NULL;";
			public static final String unBanIPServer = "UPDATE `" + table + "` SET ban_state = 0, ban_unbanreason = ?, ban_unbanstaff = ?, ban_unbandate = datetime()  "
					+ "WHERE ban_ip = ? AND ban_server = ? AND UUID IS NULL;";
			public static final String unBanServer = "UPDATE `" + table + "` SET ban_state = 0, ban_unbanreason = ?, ban_unbanstaff = ?, ban_unbandate = datetime() "
					+ "WHERE UUID = ? AND ban_server = ? AND ban_state = 1;";
			public static final String updateExpiredBan = "UPDATE `" + table + "` SET ban_state = 0 "
					+ "WHERE ban_state = 1 AND (ban_end != 0 AND ban_end < datetime());";	
		}	
	}

	public static class Mute{
		public final static String table = "BAT_mute";
		public final static String createTable = 
				"CREATE TABLE IF NOT EXISTS `" + table +"` ("
						+"`mute_id` INTEGER PRIMARY KEY AUTO_INCREMENT,"
						+"`UUID` varchar(100) NULL,"
						+"`mute_ip` varchar(50) NULL,"

				+ "`mute_staff` varchar(30) NOT NULL,"
				+ "`mute_reason` varchar(100) NULL,"
				+ "`mute_server` varchar(30) NOT NULL,"
				+ "`mute_begin` timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,"
				+ "`mute_end` timestamp NULL,"
				+ "`mute_state` bool NOT NULL default 1,"

				+ "`mute_unmutedate` timestamp NULL,"
				+ "`mute_unmutestaff` varchar(30) NULL,"
				+ "`mute_unmutereason` varchar(100) NULL,"

				+"INDEX(UUID),"
				+"INDEX(mute_ip)"
				+") ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1;";

		public static final String isMute = "SELECT mute_id FROM `" + table + "` WHERE mute_state = 1 AND (UUID = ? OR "
				+ "? IN (SELECT mute_ip FROM `" + table + "` WHERE mute_state = 1 AND UUID IS NULL));";
		public static final String isMuteServer = "SELECT mute_id FROM `" + table + "` WHERE mute_state = 1 AND (UUID = ? OR "
				+ "? IN (SELECT mute_ip FROM `" + table + "` WHERE mute_state = 1 AND UUID IS NULL)) AND"
				+ " mute_server = ?;";

		public static final String isMuteIP = "SELECT mute_id FROM `" + table + "` WHERE mute_state = 1 AND mute_ip = ?;";
		public static final String isMuteServerIP = "SELECT mute_id FROM `" + table + "` WHERE mute_state = 1 AND mute_ip = ? AND mute_server = ?;";

		public static final String createMute = "INSERT INTO `" + table
				+ "`(UUID, mute_ip, mute_staff, mute_server, mute_end, mute_reason) VALUES (?, ?, ?, ?, ?, ?);";

		public static final String createMuteIP = "INSERT INTO `" + table
				+ "`(mute_ip, mute_staff, mute_server, mute_end, mute_reason) VALUES (?, ?, ?, ?, ?);";

		public static final String unMute = "UPDATE `" + table + "` SET mute_state = 0, mute_unmutereason = ?, mute_unmutestaff = ?, mute_unmutedate = NOW() "
				+ "WHERE UUID = ? AND mute_state = 1;";
		public static final String unMuteServer = "UPDATE `" + table + "` SET mute_state = 0, mute_unmutereason = ?, mute_unmutestaff = ?, mute_unmutedate = NOW() "
				+ "WHERE UUID = ? AND mute_server = ? AND mute_state = 1;";

		public static final String unMuteIP = "UPDATE `" + table + "` SET mute_state = 0, mute_unmutereason = ?, mute_unmutestaff = ?, mute_unmutedate = NOW()  "
				+ "WHERE mute_ip = ? AND UUID IS NULL;";
		public static final String unMuteIPServer = "UPDATE `" + table + "` SET mute_state = 0, mute_unmutereason = ?, mute_unmutestaff = ?, mute_unmutedate = NOW()  "
				+ "WHERE mute_ip = ? AND mute_server = ? AND UUID IS NULL;";

		public static final String getMute = "SELECT mute_server, mute_reason, mute_staff, mute_begin, mute_end, mute_state FROM `" + table
				+ "` WHERE UUID = ? OR mute_ip = ?;";
		public static final String getMuteIP = "SELECT mute_server, mute_reason, mute_staff, mute_begin, mute_end, mute_state FROM `" + table
				+ "` WHERE mute_ip = ?;";

		public static final String updateExpiredMute = "UPDATE `" + table + "` SET mute_state = 0 "
				+ "WHERE mute_state = 1 AND (mute_end != 0 AND mute_end < NOW());";

		public static class SQLite{
			public final static String[] createTable = {
				"CREATE TABLE IF NOT EXISTS `" + table +"` ("
						+"`mute_id` INTEGER PRIMARY KEY AUTOINCREMENT,"
						+"`UUID` varchar(100) NULL,"
						+"`mute_ip` varchar(50) NULL,"

				+ "`mute_staff` varchar(30) NOT NULL,"
				+ "`mute_reason` varchar(100) NULL,"
				+ "`mute_server` varchar(30) NOT NULL,"
				+ "`mute_begin` timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,"
				+ "`mute_end` timestamp NULL,"
				+ "`mute_state` bool NOT NULL default 1,"

				+ "`mute_unmutedate` timestamp NULL,"
				+ "`mute_unmutestaff` varchar(30) NULL,"
				+ "`mute_unmutereason` varchar(100) NULL"
				+");",
				"CREATE INDEX IF NOT EXISTS `mute.uuid_index` ON " + table + " (`UUID`);",
				"CREATE INDEX IF NOT EXISTS `mute.ip_index` ON " + table + " (`mute_ip`);"
			};

			public static final String unMute = "UPDATE `" + table + "` SET mute_state = 0, mute_unmutereason = ?, mute_unmutestaff = ?, mute_unmutedate = datetime() "
					+ "WHERE UUID = ? AND mute_state = 1;";
			public static final String unMuteServer = "UPDATE `" + table + "` SET mute_state = 0, mute_unmutereason = ?, mute_unmutestaff = ?, mute_unmutedate = datetime() "
					+ "WHERE UUID = ? AND mute_server = ? AND mute_state = 1;";
			public static final String unMuteIP = "UPDATE `" + table + "` SET mute_state = 0, mute_unmutereason = ?, mute_unmutestaff = ?, mute_unmutedate = datetime()  "
					+ "WHERE mute_ip = ? AND UUID IS NULL;";
			public static final String unMuteIPServer = "UPDATE `" + table + "` SET mute_state = 0, mute_unmutereason = ?, mute_unmutestaff = ?, mute_unmutedate = datetime()  "
					+ "WHERE mute_ip = ? AND mute_server = ? AND UUID IS NULL;";

			public static final String updateExpiredMute = "UPDATE `" + table + "` SET mute_state = 0 "
					+ "WHERE mute_state = 1 AND (mute_end != 0 AND mute_end < datetime());";
		}
	}

	public static class Core{
		public static final String table = "BAT_players";

		public static final String createTable = "CREATE TABLE IF NOT EXISTS `" + table +"` ("
				+"`BAT_player` varchar(30) NOT NULL,"
				+"`UUID` varchar(100) UNIQUE NOT NULL,"
				+"`lastip` varchar(50) NOT NULL,"
				+"`firstlogin` timestamp NULL,"
				+"`lastlogin` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
				+ "INDEX(BAT_player)"
				+") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
		public static final String updateIPUUID = "INSERT INTO `" + table +"` (BAT_player, lastip, firstlogin, UUID)"
				+ " VALUES (?, ?, NOW(), ?) ON DUPLICATE KEY UPDATE lastip = ?, lastlogin = null, BAT_player = ?;"; // Set lastlogin to null to update the date to the current

		public static final String getIP = "SELECT lastip FROM `" + table +"` WHERE UUID = ?;";

		public static final String getUUID = "SELECT UUID FROM `" + table +"` WHERE BAT_player = ?;";

		public static final String getPlayerData = "SELECT lastip, firstlogin, lastlogin FROM `" + table +"` WHERE UUID = ?;";

		public static final String getIpUsers = "SELECT BAT_player FROM `" + table +"` WHERE lastip = ?";

		public static class SQLite{
			public static final String createTable[] = {
				"CREATE TABLE IF NOT EXISTS `" + table +"` ("
						+"`BAT_player` varchar(30) NOT NULL,"
						+"`UUID` varchar(100) UNIQUE NOT NULL,"
						+"`lastip` varchar(50) NOT NULL,"
						+"`firstlogin` timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,"
						+"`lastlogin` timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL"
						+");",
						"CREATE INDEX IF NOT EXISTS `core.player_index` ON " + table + " (`BAT_player`);"};
			public static final String updateIPUUID = "INSERT OR REPLACE INTO `" + table +"` (BAT_player, lastip, firstlogin, lastlogin, UUID)"
					+ " VALUES (?, ?, (SELECT firstlogin FROM `" + table +"` WHERE UUID = ?), DATETIME(), ?);";
			public static final String getPlayerData = "SELECT strftime('%s',firstlogin), strftime('%s',lastlogin), lastip FROM `" + table +"` WHERE UUID = ?;";
		}
	}
}
