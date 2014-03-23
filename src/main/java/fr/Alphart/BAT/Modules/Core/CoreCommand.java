package fr.Alphart.BAT.Modules.Core;

import static com.google.common.base.Preconditions.checkArgument;
import static fr.Alphart.BAT.I18n.I18n.__;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.mojang.api.profiles.HttpProfileRepository;
import com.mojang.api.profiles.Profile;
import com.mojang.api.profiles.ProfileCriteria;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.BATCommand;
import fr.Alphart.BAT.Modules.BATCommand.RunAsync;
import fr.Alphart.BAT.Modules.IModule;
import fr.Alphart.BAT.Modules.Ban.BanEntry;
import fr.Alphart.BAT.Modules.Mute.MuteEntry;
import fr.Alphart.BAT.Utils.FormatUtils;
import fr.Alphart.BAT.Utils.Utils;
import fr.Alphart.BAT.database.DataSourceHandler;
import fr.Alphart.BAT.database.SQLQueries;

public class CoreCommand {
	private final static BaseComponent[] CREDIT = TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes(
			'&', "&f||&9Bungee&fAdmin&cTools&f||&e - Developped by &aAlphart"));
	private final static BaseComponent[] HELP_MSG = TextComponent.fromLegacyText(ChatColor
			.translateAlternateColorCodes('&', "Type /bat help to get help"));

	public static class CommandHandler extends BATCommand {
		private final Map<String, BATCommand> subCmd;

		public CommandHandler() {
			super("bat", "", "", "");
			subCmd = new HashMap<String, BATCommand>();

			final LookupCmd lookup = new LookupCmd();
			subCmd.put(lookup.getName().split(" ")[1], lookup);
			final HelpCmd help = new HelpCmd();
			subCmd.put(help.getName().split(" ")[1], help);
			final ModulesCmd module = new ModulesCmd();
			subCmd.put(module.getName().split(" ")[1], module);
			final ConfirmCmd confirm = new ConfirmCmd();
			subCmd.put(confirm.getName().split(" ")[1], confirm);
			final ImportCmd importCmd = new ImportCmd();
			subCmd.put(importCmd.getName().split(" ")[1], importCmd);
			//			MigrateCmd migrate = new MigrateCmd();
			//			subCmd.put(confirm.getName().split(" ")[1], migrate);
		}

		public List<BATCommand> getSubCmd() {
			return new ArrayList<BATCommand>(subCmd.values());
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			if (args.length == 0) {
				sender.sendMessage(CREDIT);
				sender.sendMessage(HELP_MSG);
			} else {
				final BATCommand cmd = subCmd.get(args[0]);

				// Reorganize args (remove subcommand)
				final String[] cleanArgs = new String[args.length - 1];
				for (int i = 1; i < args.length; i++) {
					cleanArgs[i - 1] = args[i];
				}

				if (cmd != null) {
					if (cmd.getName().equals("bat confirm") || sender.hasPermission(cmd.getBATPermission()) || sender.hasPermission("bat.admin")) {
						cmd.execute(sender, cleanArgs);
					} else {
						sender.sendMessage(__("NO_PERM"));
					}
				} else {
					sender.sendMessage(__("INVALID_COMMAND"));
				}
			}
		}
	}

	public static class HelpCmd extends BATCommand {
		public HelpCmd() {
			super("bat help", "", "Displays help for core BAT commands.", "bat.help");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			final List<BATCommand> cmdsList = new ArrayList<BATCommand>();
			for (final BATCommand cmd : BAT.getInstance().getModules().getCore().getCommands()) {
				if (cmd instanceof CommandHandler) {
					cmdsList.addAll(((CommandHandler) cmd).getSubCmd());
				}
			}
			FormatUtils.showFormattedHelp(cmdsList, sender, "CORE");
		}
	}

	public static class ModulesCmd extends BATCommand {
		private final StringBuilder sb = new StringBuilder();

		public ModulesCmd() {
			super("bat modules", "", "Displays what modules are loaded and commands for those modules.", "bat.modules");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			sender.sendMessage(BAT.__("The loaded modules are :&a"));
			for (final IModule module : BAT.getInstance().getModules().getLoadedModules()) {
				if (module instanceof Core) {
					continue;
				}
				sb.setLength(0);
				sb.append("&f - &9");
				sb.append(module.getName());
				sb.append(" &f| &eMain command : &a/");
				sb.append(module.getMainCommand());
				sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',
						sb.toString())));
			}
			// It means that no module were loaded otherwise there would be
			// something remaining in the StringBuilder
			if (sb.length() == 0) {
				sender.sendMessage(BAT.__("&cThere aren't any loaded modules!"));
			} else {
				sb.setLength(0); // Clean the sb
			}
		}
	}

	@RunAsync
	public static class LookupCmd extends BATCommand {
		private final SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy 'at' HH:mm");
		private final Calendar localTime = Calendar.getInstance(TimeZone.getDefault());

		public LookupCmd() {
			super("bat lookup", "<player/ip>", "Display a player or an ip related information.", "bat.lookup");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			if (Utils.validIP(args[0])) {
				for (final BaseComponent[] msg : getFormatLookupIP(args[0])) {
					sender.sendMessage(msg);
				}
			} else {
				for (final BaseComponent[] msg : getFormatLookupPlayer(args[0])) {
					sender.sendMessage(msg);
				}
			}
		}

		public List<BaseComponent[]> getFormatLookupIP(final String ip) {
			final StringBuilder finalMsg = new StringBuilder();
			finalMsg.append("&f---- &BLookup &f- &a");
			finalMsg.append(ip);
			finalMsg.append(" &f----\n");

			final EntityEntry ipDetails = new EntityEntry(ip);

			if (!ipDetails.exist()) {
				final List<BaseComponent[]> returnedMsg = new ArrayList<BaseComponent[]>();
				returnedMsg.add(__("&eThe IP &a" + ip + "&e doesn't have any recording."));
				return returnedMsg;
			}

			boolean isBan = false;
			int bansNumber = 0;
			final List<String> banServers = new ArrayList<String>();
			boolean isMute = false;
			int mutesNumber = 0;
			final List<String> muteServers = new ArrayList<String>();

			if (!ipDetails.getBans().isEmpty()) {
				for (final BanEntry banEntry : ipDetails.getBans()) {
					if (banEntry.isActive()) {
						isBan = true;
						banServers.add(banEntry.getServer());
					}
				}
				bansNumber = ipDetails.getBans().size();
			}
			if (!ipDetails.getMutes().isEmpty()) {
				for (final MuteEntry muteEntry : ipDetails.getMutes()) {
					if (muteEntry.isActive()) {
						isMute = true;
						muteServers.add(muteEntry.getServer());
					}
				}
				mutesNumber = ipDetails.getMutes().size();
			}

			finalMsg.append("&eThis IP is used by the following players : \n&3 ");
			finalMsg.append(Joiner.on("&f, &3").join(ipDetails.getUsers()));

			if (isBan || isMute) {
				finalMsg.append("\n&eState : ");
				if (isBan) {
					finalMsg.append("\n&c&lBanned &efrom &3");
					finalMsg.append(Joiner.on("&f, &3").join(banServers));
				}
				if (isMute) {
					finalMsg.append("\n&c&lMute &efrom &3");
					finalMsg.append(Joiner.on("&f, &3").join(muteServers));
				}
			}

			if (bansNumber > 0 || mutesNumber > 0) {
				finalMsg.append("\n&eHistory : ");
				if (bansNumber > 0) {
					finalMsg.append("&B&l");
					finalMsg.append(bansNumber);
					finalMsg.append((bansNumber > 1) ? "&e bans" : "&e ban");
				}
				if (mutesNumber > 0) {
					finalMsg.append("\n&B&l            ");
					finalMsg.append(mutesNumber);
					finalMsg.append((mutesNumber > 1) ? "&e mutes" : "&e mute");
				}
			} else {
				finalMsg.append("\n&eNo sanctions ever imposed.");
			}

			finalMsg.append("\n&f-- &BLookup &f- &a");
			finalMsg.append(ip);
			finalMsg.append(" &f--");

			return FormatUtils.formatNewLine(ChatColor.translateAlternateColorCodes('&', finalMsg.toString()));
		}

		public List<BaseComponent[]> getFormatLookupPlayer(final String pName) {
			final StringBuilder finalMsg = new StringBuilder();
			finalMsg.append("&f---- &BLookup &f- &a");
			finalMsg.append(pName);
			finalMsg.append(" &f----\n");

			// Get players data related to each modules
			final EntityEntry pDetails = new EntityEntry(pName);

			if (!pDetails.exist()) {
				final List<BaseComponent[]> returnedMsg = new ArrayList<BaseComponent[]>();
				returnedMsg.add(__("playerNotFound"));
				return returnedMsg;
			}

			final EntityEntry ipDetails = new EntityEntry(Core.getPlayerIP(pName));

			boolean isBan = false;
			boolean isBanIP = false;
			int bansNumber = 0;
			final List<String> banServers = Lists.newArrayList();
			final List<String> banIPServers = Lists.newArrayList();
			boolean isMute = false;
			boolean isMuteIP = false;
			int mutesNumber = 0;
			final List<String> muteServers = Lists.newArrayList();
			final List<String> muteIPServers = Lists.newArrayList();
			int kicksNumber = 0;

			for (final BanEntry banEntry : pDetails.getBans()) {
				if (banEntry.isActive()) {
					isBan = true;
					banServers.add(banEntry.getServer());
				}
			}
			for (final BanEntry banEntry : ipDetails.getBans()) {
				if (banEntry.isActive()) {
					isBanIP = true;
					banIPServers.add(banEntry.getServer());
				}
			}
			for (final MuteEntry muteEntry : pDetails.getMutes()) {
				if (muteEntry.isActive()) {
					isMute = true;
					muteServers.add(muteEntry.getServer());
				}
			}
			for (final MuteEntry muteEntry : ipDetails.getMutes()) {
				if (muteEntry.isActive()) {
					isMuteIP = true;
					muteIPServers.add(muteEntry.getServer());
				}
			}
			bansNumber = pDetails.getBans().size() + ipDetails.getBans().size();
			mutesNumber = pDetails.getMutes().size() + ipDetails.getMutes().size();
			kicksNumber = pDetails.getKicks().size();

			// Construction of the message
			finalMsg.append((ProxyServer.getInstance().getPlayer(pName) != null) ? "&a&lConnected &r&eon the &3"
					+ ProxyServer.getInstance().getPlayer(pName).getServer().getInfo().getName() + " &eserver" : "&8&lOffline");

			if (isBan || isMute || isBanIP || isMuteIP) {
				finalMsg.append("\n&eState : ");
				if (isBan) {
					finalMsg.append("\n&c&lBanned &efrom &3");
					finalMsg.append(Joiner.on("&f, &3").join(banServers));
				}
				if (isBanIP) {
					finalMsg.append("\n&c&lBanned IP &efrom &3");
					finalMsg.append(Joiner.on("&f, &3").join(banIPServers));
				}
				if (isMute) {
					finalMsg.append("\n&c&lMute &efrom &3");
					finalMsg.append(Joiner.on("&f, &3").join(muteServers));
				}
				if (isMuteIP) {
					finalMsg.append("\n&c&lMute IP &efrom &3");
					finalMsg.append(Joiner.on("&f, &3").join(muteIPServers));
				}
			}

			localTime.setTimeInMillis(pDetails.getFirstLogin().getTime());
			finalMsg.append("\n&eFirst login : &a");
			finalMsg.append(format.format(localTime.getTime()));

			localTime.setTimeInMillis(pDetails.getLastLogin().getTime());
			finalMsg.append("\n&eLast login : &a");
			finalMsg.append(format.format(localTime.getTime()));

			finalMsg.append("\n&eLast IP : &a");
			finalMsg.append(pDetails.getLastIP());

			if (bansNumber > 0 || mutesNumber > 0 || kicksNumber > 0) {
				finalMsg.append("\n&eHistory : ");
				if (bansNumber > 0) {
					finalMsg.append("&B&l");
					finalMsg.append(bansNumber);
					finalMsg.append((bansNumber > 1) ? "&e bans" : "&e ban");
				}
				if (mutesNumber > 0) {
					finalMsg.append("\n&B&l            ");
					finalMsg.append(mutesNumber);
					finalMsg.append((mutesNumber > 1) ? "&e mutes" : "&e mute");
				}
				if (kicksNumber > 0) {
					finalMsg.append("\n&B&l            ");
					finalMsg.append(kicksNumber);
					finalMsg.append((kicksNumber > 1) ? "&e kicks" : "&e kick");
				}
			} else {
				finalMsg.append("\n&eNo sanctions ever imposed.");
			}

			finalMsg.append("\n&f-- &BLookup &f- &a");
			finalMsg.append(pName);
			finalMsg.append(" &f--");

			return FormatUtils.formatNewLine(ChatColor.translateAlternateColorCodes('&', finalMsg.toString()));
		}
	}

	public static class ConfirmCmd extends BATCommand {
		public ConfirmCmd() {
			super("bat confirm", "", "Confirm your queued command.", null);
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			if (!CommandQueue.executeQueueCommand(sender)) {
				sender.sendMessage(__("noQueuedCommand"));
			}
		}

	}

	@RunAsync
	public static class ImportCmd extends BATCommand{
		private final HttpProfileRepository profileRepository = Core.getProfileRepository();
		public ImportCmd() { super("bat import", "<bungeeSuiteBans/geSuitBans>", "Imports ban data from the specified source.", "bat.import");}

		public String getUUIDusingMojangAPI(final String pName){
			final Profile[] profiles = profileRepository.findProfilesByCriteria(new ProfileCriteria(pName, "minecraft"));

			if (profiles.length > 0) {
				return profiles[0].getId();
			} else {
				return null;
			}
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			String source = args[0];
			
			if("bungeeSuiteBans".equalsIgnoreCase(source)){
				importFromBungeeSuite(sender);
			}else if("geSuitBans".equalsIgnoreCase(source)){
				importFromGeSuit(sender);
			}else{
				throw new IllegalArgumentException("The specified source is incorrect. It may be either &abungeeSuiteBans&e or &ageSuitBans");
			}
		}
		
		public void importFromBungeeSuite(final CommandSender sender){
			ResultSet res = null;
			try (Connection conn = BAT.getConnection()) {
				// Check if the bungee suite tables are here
				final DatabaseMetaData dbm = conn.getMetaData();
				for(final String table : Arrays.asList("BungeeBans", "BungeePlayers")){
					final ResultSet tables = dbm.getTables(null, null, table, null);
					if (!tables.next()) {
						throw new IllegalArgumentException("The table " + table + " wasn't found. Import aborted ...");
					}
				}

				sender.sendMessage(BAT.__("BAT will be disabled during the import ..."));
				BAT.getInstance().getModules().unloadModules();

				int totalEntries = 0;
				int convertedEntries = 0;

				// Count the number of entries (use to show the progression)
				final ResultSet resCount = conn.prepareStatement("SELECT COUNT(*) FROM BungeeBans;").executeQuery();
				if(resCount.next()){
					totalEntries = resCount.getInt("COUNT(*)");
				}

				if(totalEntries == 0){
					sender.sendMessage(BAT.__("There is no entry to convert."));
					return;
				}

				final PreparedStatement insertBans = conn.prepareStatement("INSERT INTO `" + SQLQueries.Ban.table
						+ "`(UUID, ban_ip, ban_staff, ban_server, ban_begin, ban_end, ban_reason) VALUES (?, ?, ?, ?, ?, ?, ?);");

				final PreparedStatement getIP = conn.prepareStatement("SELECT ipaddress FROM BungeePlayers WHERE playername = ?;");

				res = conn.prepareStatement("SELECT * FROM BungeeBans;").executeQuery();

				// Contains all the player uuids already used
				final Map<String, String> pUUIDs = new HashMap<String, String>();

				while (res.next()) {
					final boolean ipBan = "ipban".equals(res.getString("type"));

					final String pName = res.getString("player");
					final String server = IModule.GLOBAL_SERVER;
					final String staff = res.getString("banned_by");
					final String reason = res.getString("reason");
					final Timestamp ban_begin = res.getTimestamp("banned_on");
					final Timestamp ban_end = res.getTimestamp("banned_until");

					// Get the ip
					String ip = null;
					getIP.setString(1, pName);	
					final ResultSet resIP = getIP.executeQuery();
					if(resIP.next()){
						ip = resIP.getString("ipaddress");
					}
					resIP.close();
					if(ipBan && ip == null){
						continue;
					}

					// Get UUID
					String UUID = pUUIDs.get(pName);
					if(UUID == null){
						UUID = getUUIDusingMojangAPI(pName);
						if(UUID == null){
							UUID = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + getName()).getBytes(Charsets.UTF_8)).toString();
						}
						pUUIDs.put(pName, UUID);
					}

					// Insert the ban
					insertBans.setString(1, (ipBan) ? null : Core.getUUID(pName));
					insertBans.setString(2, (ipBan) ? ip : null);
					insertBans.setString(3, staff);
					insertBans.setString(4, server);
					insertBans.setTimestamp(5, ban_begin);
					insertBans.setTimestamp(6, ban_end);
					insertBans.setString(7, reason);
					insertBans.execute();
					insertBans.clearParameters();
					getIP.clearParameters();
					convertedEntries++;

					// Every 100 entries converted, show the progess
					if(convertedEntries % 100 == 0){
						sender.sendMessage(BAT.__("&a" + (convertedEntries / totalEntries) +  "%&e entries converted !&a"
								+ (totalEntries - convertedEntries) + "&e remaining entries on a total of &6" + totalEntries));
					}
				}

				sender.sendMessage(BAT.__("Congratulations, the migration is finished. &a" + convertedEntries + " entries&e was converted successfully."));
				BAT.getInstance().getModules().loadModules();
			} catch (final SQLException e) {
				sender.sendMessage(BAT.__(DataSourceHandler.handleException(e)));
			} finally{
				DataSourceHandler.close(res);
			}
		}
	
		public void importFromGeSuit(final CommandSender sender){
			ResultSet res = null;
			try (Connection conn = BAT.getConnection()) {
				// Check if the bungee suite tables are here
				final DatabaseMetaData dbm = conn.getMetaData();
				for(final String table : Arrays.asList("bans", "players")){
					final ResultSet tables = dbm.getTables(null, null, table, null);
					if (!tables.next()) {
						throw new IllegalArgumentException("The table " + table + " wasn't found. Import aborted ...");
					}
				}

				sender.sendMessage(BAT.__("BAT will be disabled during the import ..."));
				BAT.getInstance().getModules().unloadModules();

				int totalEntries = 0;
				int convertedEntries = 0;

				// Count the number of entries (use to show the progression)
				final ResultSet resCount = conn.prepareStatement("SELECT COUNT(*) FROM bans;").executeQuery();
				if(resCount.next()){
					totalEntries = resCount.getInt("COUNT(*)");
				}

				if(totalEntries == 0){
					sender.sendMessage(BAT.__("There is no entry to convert."));
					return;
				}

				final PreparedStatement insertBans = conn.prepareStatement("INSERT INTO `" + SQLQueries.Ban.table
						+ "`(UUID, ban_ip, ban_staff, ban_server, ban_begin, ban_end, ban_reason) VALUES (?, ?, ?, ?, ?, ?, ?);");

				final PreparedStatement getIP = conn.prepareStatement("SELECT ipaddress FROM players WHERE playername = ?;");

				res = conn.prepareStatement("SELECT * FROM bans;").executeQuery();

				// Contains all the player uuids already used
				final Map<String, String> pUUIDs = new HashMap<String, String>();

				while (res.next()) {
					final boolean ipBan = "ipban".equals(res.getString("type"));

					final String pName = res.getString("display");
					final String server = IModule.GLOBAL_SERVER;
					final String staff = res.getString("banned_by");
					final String reason = res.getString("reason");
					final Timestamp ban_begin = res.getTimestamp("banned_on");
					final Timestamp ban_end = res.getTimestamp("banned_until");

					// Get the ip
					String ip = null;
					getIP.setString(1, pName);	
					final ResultSet resIP = getIP.executeQuery();
					if(resIP.next()){
						ip = resIP.getString("ipaddress");
					}
					resIP.close();
					if(ipBan && ip == null){
						continue;
					}

					// Get UUID
					String UUID = pUUIDs.get(pName);
					if(UUID == null){
						UUID = getUUIDusingMojangAPI(pName);
						if(UUID == null){
							UUID = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + getName()).getBytes(Charsets.UTF_8)).toString();
						}
						pUUIDs.put(pName, UUID);
					}

					// Insert the ban
					insertBans.setString(1, (ipBan) ? null : Core.getUUID(pName));
					insertBans.setString(2, (ipBan) ? ip : null);
					insertBans.setString(3, staff);
					insertBans.setString(4, server);
					insertBans.setTimestamp(5, ban_begin);
					insertBans.setTimestamp(6, ban_end);
					insertBans.setString(7, reason);
					insertBans.execute();
					insertBans.clearParameters();
					getIP.clearParameters();
					convertedEntries++;

					// Every 100 entries converted, show the progess
					if(convertedEntries % 100 == 0){
						sender.sendMessage(BAT.__("&a" + (convertedEntries / totalEntries) +  "%&e entries converted !&a"
								+ (totalEntries - convertedEntries) + "&e remaining entries on a total of &6" + totalEntries));
					}
				}

				sender.sendMessage(BAT.__("Congratulations, the migration is finished. &a" + convertedEntries + " entries&e was converted successfully."));
				BAT.getInstance().getModules().loadModules();
			} catch (final SQLException e) {
				sender.sendMessage(BAT.__(DataSourceHandler.handleException(e)));
			} finally{
				DataSourceHandler.close(res);
			}
		}
	}

	@RunAsync
	public static class MigrateCmd extends BATCommand {
		public MigrateCmd() { super("bat migrate", "<target>", "Migrate from the source to the target datasource (mysql or sqlite)", "bat.migrate");}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			final String target = args[1];
			checkArgument(!Arrays.asList("mysql", "sqlite").contains(target.toLowerCase()), "Target must be mysql or sqlite.");	
			if("sqlite".equalsIgnoreCase(target)){
				checkArgument(!DataSourceHandler.isSQLite(), "SQLite is already used.");
			}else if("mysql".equalsIgnoreCase(target)){
				checkArgument(DataSourceHandler.isSQLite(), "MySQL is already used.");
			}
			BAT.getInstance().migrate(target);
		}
	}
}