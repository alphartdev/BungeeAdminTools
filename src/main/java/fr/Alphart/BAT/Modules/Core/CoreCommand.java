package fr.Alphart.BAT.Modules.Core;

import static fr.Alphart.BAT.BAT.__;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

import com.google.common.base.Joiner;
import com.google.common.net.InetAddresses;

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
			// InsertCmd insert = new InsertCmd();
			// subCmd.put(insert.getName().split(" ")[1], insert);
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
					if (cmd.getName() == "bat confirm" || sender.hasPermission(cmd.getPermission())) {
						cmd.execute(sender, cleanArgs);
					} else {
						sender.sendMessage(__("&cYou don't have the permission !"));
					}
				} else {
					sender.sendMessage(__("Invalid command !"));
				}
			}
		}
	}

	public static class HelpCmd extends BATCommand {
		public HelpCmd() {
			super("bat help", "", "Show the help", "bat.help");
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
			super("bat modules", "", "Show the loaded modules and their commands", "bat.modules");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			sender.sendMessage(__("The loaded modules are :&a"));
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
				sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',
						"&cThere aren't any loaded modules!")));
			} else {
				sb.setLength(0); // Clean the sb
			}
		}
	}

	@RunAsync
	public static class LookupCmd extends BATCommand {
		private final SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy à HH'h'mm");
		private final Calendar localTime = Calendar.getInstance(TimeZone.getDefault());

		public LookupCmd() {
			super("bat lookup", "<player/ip>", "Display a player or an ip related informations", "bat.lookup");
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
					finalMsg.append("&c&lBanned &efrom &3");
					finalMsg.append(Joiner.on("&f, &3").join(banServers));
				}
				if (isMute) {
					if (isBan) {
						finalMsg.append("\n       ");
					}
					finalMsg.append("&c&lMuted &efrom &3");
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
				finalMsg.append("\n&eNone sanction indexed");
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
				returnedMsg.add(__("&eThe player &a" + pName + "&e was not found."));
				return returnedMsg;
			}

			boolean isBan = false;
			int bansNumber = 0;
			final List<String> banServers = new ArrayList<String>();
			boolean isMute = false;
			int mutesNumber = 0;
			final List<String> muteServers = new ArrayList<String>();
			int kicksNumber = 0;

			if (!pDetails.getBans().isEmpty()) {
				for (final BanEntry banEntry : pDetails.getBans()) {
					if (banEntry.isActive()) {
						isBan = true;
						banServers.add(banEntry.getServer());
					}
				}
				bansNumber = pDetails.getBans().size();
			}
			if (!pDetails.getMutes().isEmpty()) {
				for (final MuteEntry muteEntry : pDetails.getMutes()) {
					if (muteEntry.isActive()) {
						isMute = true;
						muteServers.add(muteEntry.getServer());
					}
				}
				mutesNumber = pDetails.getMutes().size();
			}
			if (!pDetails.getKicks().isEmpty()) {
				kicksNumber = pDetails.getKicks().size();
			}

			// Construction of the message
			finalMsg.append((ProxyServer.getInstance().getPlayer(pName) != null) ? "&a&lConnected &r&eon the &3"
					+ ProxyServer.getInstance().getPlayer(pName).getServer().getInfo().getName() : "&8&lDéconnecté");

			if (isBan || isMute) {
				finalMsg.append("\n&eState : ");
				if (isBan) {
					finalMsg.append("&c&lBanned &efrom &3");
					finalMsg.append(Joiner.on("&f, &3").join(banServers));
				}
				if (isMute) {
					if (isBan) {
						finalMsg.append("\n       ");
					}
					finalMsg.append("&c&lMuted &efrom &3");
					finalMsg.append(Joiner.on("&f, &3").join(muteServers));
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
				finalMsg.append("\n&eNone sanction indexed");
			}

			finalMsg.append("\n&f-- &BLookup &f- &a");
			finalMsg.append(pName);
			finalMsg.append(" &f--");

			return FormatUtils.formatNewLine(ChatColor.translateAlternateColorCodes('&', finalMsg.toString()));
		}
	}

	public static class ConfirmCmd extends BATCommand {
		public ConfirmCmd() {
			super("bat confirm", "", "Confirm your queued command", "");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			if (!CommandQueue.executeQueueCommand(sender)) {
				sender.sendMessage(__("You have no queued command ..."));
			}
		}

	}

	@RunAsync
	public static class InsertCmd extends BATCommand {
		public InsertCmd() {
			super("bat insert", "", "Insère des informations aléatoires dans la DB", "bat.lookup");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {

			sender.sendMessage(__("Insertion des infos en cours ..."));

			final int rowsNumber = 10000;

			// Random column
			final RandomString player = new RandomString(15);
			final RandomString server = new RandomString(10);
			final RandomString staff = new RandomString(15);
			final RandomString reason = new RandomString(30);

			// Random timestamp
			final long offset = Timestamp.valueOf("2014-02-12 15:00:00").getTime();
			final long end = Timestamp.valueOf("2016-01-01 00:00:00").getTime();
			final long diff = end - offset + 1;

			try (Connection conn = BAT.getConnection()) {
				final PreparedStatement statement = conn.prepareStatement(SQLQueries.Ban.createBan);
				final Random random = new Random();

				for (int i = 0; i < rowsNumber; i++) {
					statement.setString(1, player.nextString());
					statement.setString(2, InetAddresses.fromInteger(random.nextInt()).getHostAddress());
					statement.setString(3, staff.nextString());
					statement.setString(4, server.nextString());
					statement.setTimestamp(5, new Timestamp(offset + (long) (Math.random() * diff)));
					statement.setString(6, reason.nextString());
					statement.executeUpdate();

					if (i % 500 == 0) {
						BAT.getInstance().getLogger().info("<!> " + i + " enregistrements ont été insérés ...");
						BAT.getInstance().getLogger()
								.info("<!> Il reste " + (rowsNumber - i) + " enregistrements à insérer.");
					}

				}

			} catch (final SQLException e) {
				DataSourceHandler.handleException(e);
			}

			BAT.getInstance().getLogger().info("<!> Toutes les données ont été insérées !!");
		}

		public static class RandomString {
			private static final char[] symbols = new char[36];
			static {
				for (int idx = 0; idx < 10; ++idx) {
					symbols[idx] = (char) ('0' + idx);
				}
				for (int idx = 10; idx < 36; ++idx) {
					symbols[idx] = (char) ('a' + idx - 10);
				}
			}

			private final Random random = new Random();

			private final char[] buf;

			public RandomString(final int length) {
				if (length < 1) {
					throw new IllegalArgumentException("length < 1: " + length);
				}
				buf = new char[length];
			}

			public String nextString() {
				for (int idx = 0; idx < buf.length; ++idx) {
					buf[idx] = symbols[random.nextInt(symbols.length)];
				}
				return new String(buf);
			}
		}
	}

}