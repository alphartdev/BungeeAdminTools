package fr.Alphart.BAT.Modules.Ban;

import static com.google.common.base.Preconditions.checkArgument;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Message;
import fr.Alphart.BAT.Modules.BATCommand;
import fr.Alphart.BAT.Modules.BATCommand.RunAsync;
import fr.Alphart.BAT.Modules.CommandHandler;
import fr.Alphart.BAT.Modules.IModule;
import fr.Alphart.BAT.Modules.InvalidModuleException;
import fr.Alphart.BAT.Utils.FormatUtils;
import fr.Alphart.BAT.Utils.Utils;
import fr.Alphart.BAT.database.DataSourceHandler;

public class BanCommand extends CommandHandler {
	private static final String BAN_PERM = Ban.BAN_PERM;
	private static Ban ban;

	private static final String ALREADY_BAN = "&cThis player is already banned from this server!";
	private static final String NOT_BAN = "&c%entity% isn't banned from this server.";
	private static final String NOT_BANIP = "&c%entity% isn't banned IP from this server.";
	private static final String NOT_BAN_ANY = "&c%entity% isn't banned from any server !";

	public BanCommand(final Ban banModule){
		super(banModule);
		ban = banModule;
	}

	@RunAsync
	public static class BanCmd extends BATCommand{
		public BanCmd() {super("ban", "<player> [server] [reason]", "Ban definitively the player from the specified server", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			if(args[0].equals("help")){
				try {
					FormatUtils.showFormattedHelp(BAT.getInstance().getModules().getModule("ban").getCommands(), sender, "BAN");
				} catch (final InvalidModuleException e) {
					e.printStackTrace();
				}
				return;
			}
			final String pName = args[0];
			String returnedMsg = null;

			// Command pattern : /ban <name>
			if(args.length == 1){
				// If the sender isn't a player, he has to specify a server
				checkArgument(isPlayer(sender), Message.SPECIFY_SERVER);

				final String server = ((ProxiedPlayer)sender).getServer().getInfo().getName();
				// If the player is already ban of this server, the command is gonna be cancelled
				checkArgument(!ban.isBan(pName, server), ALREADY_BAN);

				returnedMsg = ban.ban(pName, server, sender.getName(), 0, IModule.NO_REASON);
			}
			else{
				final String server = args[1];
				// Check if the server is an valid server
				checkArgument(Utils.isServer(server), Message.INVALID_SERVER);
				// Check if the player isn't already banned from this server
				checkArgument(!ban.isBan(pName, server), ALREADY_BAN);

				// Command pattern : /ban <name> <server>
				if(args.length == 2) {
					returnedMsg = ban.ban(pName, server, sender.getName(), 0, IModule.NO_REASON);
				} else{
					final String reason = Utils.getFinalArg(args, 2);
					returnedMsg = ban.ban(pName, server, sender.getName(), 0, reason);
				}
			}


			BAT.broadcast(returnedMsg, BAN_PERM);
		}
	}
	@RunAsync
	public static class BanIPCmd extends BATCommand{
		public BanIPCmd() {super("banip", "<player/ip> [server] [reason]", "Ban definitively player's IP from the specified server", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			final String entity = args[0];
			final boolean isIP = Utils.validIP(entity);
			final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(entity);
			if(!isIP) {
				checkArgument(player != null, Message.IP_OFFLINE_PLAYER);
			}
			String returnedMsg = null;

			// Command pattern : /ban <name>
			if(args.length == 1){
				checkArgument(isPlayer(sender), Message.SPECIFY_SERVER);

				final String server = ((ProxiedPlayer)sender).getServer().getInfo().getName();
				checkArgument(!ban.isBan(entity, server), ALREADY_BAN);

				if(isIP) {
					returnedMsg = ban.ban(entity, server, sender.getName(), 0, IModule.NO_REASON);
				} else {
					returnedMsg = ban.banIP(player, server, sender.getName(), 0, IModule.NO_REASON);
				}
			}
			else{
				final String server = args[1];
				checkArgument(Utils.isServer(server), Message.INVALID_SERVER);
				checkArgument(!ban.isBan(entity, server), ALREADY_BAN);

				// Command pattern : /ban <name> <server>
				if(args.length == 2){
					if(isIP) {
						returnedMsg = ban.ban(entity, server, sender.getName(), 0, IModule.NO_REASON);
					} else {
						returnedMsg = ban.banIP(player, server, sender.getName(), 0, IModule.NO_REASON);
					}
				}

				// Command pattern : /ban <name> <server> <reason>
				else{
					final String reason = Utils.getFinalArg(args, 2);

					if(isIP) {
						returnedMsg = ban.ban(entity, server, sender.getName(), 0, reason);
					} else {
						returnedMsg = ban.banIP(player, server, sender.getName(), 0, reason);
					}
				}
			}


			BAT.broadcast(returnedMsg, BAN_PERM);
		}
	}
	@RunAsync
	public static class GBanCmd extends BATCommand{
		public GBanCmd() {super("gban", "<player> [reason]", "Ban definitively the player from the whole network", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			final String pName = args[0];
			String returnedMsg;

			checkArgument(!ban.isBan(pName, IModule.GLOBAL_SERVER), ALREADY_BAN);

			if(args.length == 1){
				returnedMsg = ban.ban(pName, IModule.GLOBAL_SERVER, sender.getName(), 0, IModule.NO_REASON);
			}
			else{
				final String reason = Utils.getFinalArg(args, 1);
				returnedMsg = ban.ban(pName, IModule.GLOBAL_SERVER, sender.getName(), 0, reason);
			}

			BAT.broadcast(returnedMsg, BAN_PERM);
		}
	}
	@RunAsync
	public static class GBanIPCmd extends BATCommand{
		public GBanIPCmd() {super("gbanip", "<player/ip> [reason]", "Ban definitively player's IP from the whole network", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			final String entity = args[0];
			final boolean isIP = Utils.validIP(entity);
			final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(entity);
			if(!isIP) {
				checkArgument(player != null, Message.IP_OFFLINE_PLAYER);
			}
			String returnedMsg;

			checkArgument(!ban.isBan(entity, IModule.GLOBAL_SERVER), ALREADY_BAN);

			if(args.length == 1){
				if(isIP) {
					returnedMsg = ban.ban(entity, IModule.GLOBAL_SERVER, sender.getName(), 0, IModule.NO_REASON);
				} else {
					returnedMsg = ban.banIP(player, IModule.GLOBAL_SERVER, sender.getName(), 0, IModule.NO_REASON);
				}
			}
			else{
				final String reason = Utils.getFinalArg(args, 1);
				if(isIP) {
					returnedMsg = ban.ban(entity, IModule.GLOBAL_SERVER, sender.getName(), 0, reason);
				} else {
					returnedMsg = ban.banIP(player, IModule.GLOBAL_SERVER, sender.getName(), 0, reason);
				}
			}

			BAT.broadcast(returnedMsg, BAN_PERM);
		}
	}

	@RunAsync
	public static class TempBanCmd extends BATCommand{
		public TempBanCmd() {super("tempban", "<player> <duration> [server] [reason]", "Ban temporarily the player from the specified server", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			final String pName = args[0];
			String returnedMsg;
			final int durate = Utils.parseDateDiff(args[1], true) - DataSourceHandler.getTimestamp();

			// Command pattern : /ban <name> <durate>
			if(args.length == 2){
				checkArgument(isPlayer(sender), Message.SPECIFY_SERVER);
				final String server = ((ProxiedPlayer)sender).getServer().getInfo().getName();
				checkArgument(!ban.isBan(pName, server), ALREADY_BAN);

				returnedMsg = ban.ban(pName, server, sender.getName(), durate, IModule.NO_REASON);
			}
			else{
				final String server = args[2];
				checkArgument(Utils.isServer(server), Message.INVALID_SERVER);
				checkArgument(!ban.isBan(pName, server), ALREADY_BAN);

				// Command pattern: /ban <name> <durate> <server>
				if(args.length == 3) {
					returnedMsg = ban.ban(pName, server, sender.getName(), durate, IModule.NO_REASON);
				} else{
					final String reason = Utils.getFinalArg(args, 3);
					returnedMsg = ban.ban(pName, server, sender.getName(), durate, reason);
				}
			}

			BAT.broadcast(returnedMsg, BAN_PERM); 
		}
	}
	@RunAsync
	public static class TempBanIPCmd extends BATCommand{
		public TempBanIPCmd() {super("tempbanip", "<player/ip> <duration> [server] [reason]", "Ban temporarily player's IP from the specified server", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			final String entity = args[0];
			final boolean isIP = Utils.validIP(entity);
			final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(entity);
			if(!isIP) {
				checkArgument(player != null, Message.IP_OFFLINE_PLAYER);
			}
			String returnedMsg;
			final int durate = Utils.parseDateDiff(args[1], true) - DataSourceHandler.getTimestamp();

			// Command pattern : /ban <name> <durate>
			if(args.length == 2){
				checkArgument(isPlayer(sender), Message.SPECIFY_SERVER);
				final String server = ((ProxiedPlayer)sender).getServer().getInfo().getName();
				checkArgument(!ban.isBan(entity, server), ALREADY_BAN);

				if(isIP) {
					returnedMsg = ban.ban(entity, server, sender.getName(), durate, IModule.NO_REASON);
				} else {
					returnedMsg = ban.banIP(player, server, sender.getName(), durate, IModule.NO_REASON);
				}
			}
			else{
				final String server = args[2];
				checkArgument(Utils.isServer(server), Message.INVALID_SERVER);
				checkArgument(!ban.isBan(entity, server), ALREADY_BAN);

				// Command pattern: /ban <name> <durate> <server>
				if(args.length == 3){
					if(isIP) {
						returnedMsg = ban.ban(entity, server, sender.getName(), durate, IModule.NO_REASON);
					} else {
						returnedMsg = ban.banIP(player, server, sender.getName(), durate, IModule.NO_REASON);
					}
				}

				// Command pattern: /ban <name> <durate> <server> <reason>
				else{
					final String reason = Utils.getFinalArg(args, 3);
					if(isIP) {
						returnedMsg = ban.ban(entity, server, sender.getName(), durate, reason);
					} else {
						returnedMsg = ban.banIP(player, server, sender.getName(), durate, reason);
					}
				}
			}

			BAT.broadcast(returnedMsg, BAN_PERM); 

		}
	}
	@RunAsync
	public static class GTempBanCmd extends BATCommand{
		public GTempBanCmd() {super("gtempban", "<player> <duration> [reason]", "Ban temporarily the player from the whole network", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			final String pName = args[0];
			final int durate = Utils.parseDateDiff(args[1], true) - DataSourceHandler.getTimestamp();
			String returnedMsg;

			checkArgument(!ban.isBan(pName, IModule.GLOBAL_SERVER), ALREADY_BAN);

			if(args.length == 2){
				returnedMsg = ban.ban(pName, IModule.GLOBAL_SERVER, sender.getName(), durate, IModule.NO_REASON);
			}
			else{
				final String reason = Utils.getFinalArg(args, 2);
				returnedMsg = ban.ban(pName, IModule.GLOBAL_SERVER, sender.getName(), durate, reason);
			}

			BAT.broadcast(returnedMsg, BAN_PERM);
		}
	}
	@RunAsync
	public static class GTempBanIPCmd extends BATCommand{
		public GTempBanIPCmd() {super("gtempbanip", "<player/ip> <duration> [reason]", "Ban temporarily player's IP from the whole network", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			final String entity = args[0];
			final boolean isIP = Utils.validIP(entity);
			final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(entity);
			if(!isIP) {
				checkArgument(player != null, Message.IP_OFFLINE_PLAYER);
			}
			final int durate = Utils.parseDateDiff(args[1], true) - DataSourceHandler.getTimestamp();
			String returnedMsg;

			checkArgument(!ban.isBan(entity, IModule.GLOBAL_SERVER), ALREADY_BAN);

			if(args.length == 2){
				if(isIP) {
					returnedMsg = ban.ban(entity, IModule.GLOBAL_SERVER, sender.getName(), durate, IModule.NO_REASON);
				} else {
					returnedMsg = ban.banIP(player, IModule.GLOBAL_SERVER, sender.getName(), durate, IModule.NO_REASON);
				}
			}
			else{
				final String reason = Utils.getFinalArg(args, 2);
				if(isIP) {
					returnedMsg = ban.ban(entity, IModule.GLOBAL_SERVER, sender.getName(), durate, reason);
				} else {
					returnedMsg = ban.banIP(player, IModule.GLOBAL_SERVER, sender.getName(), durate, reason);
				}
			}

			BAT.broadcast(returnedMsg, BAN_PERM);
		}
	}

	@RunAsync
	public static class PardonCmd extends BATCommand{
		public PardonCmd() {super("pardon", "<player> [server] [reason]", "Unban the player from the specified server", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			final String pName = args[0];
			String returnedMsg = null;

			// Command pattern : /ban <name>
			if(args.length == 1){
				checkArgument(isPlayer(sender), Message.SPECIFY_SERVER);
				final String server = ((ProxiedPlayer)sender).getServer().getInfo().getName();
				checkArgument(ban.isBan(pName, server), NOT_BAN.replaceAll("%entity%", pName));

				returnedMsg = ban.unBan(pName, server, sender.getName(), IModule.NO_REASON);
			}
			else{
				final String server = args[1];
				checkArgument(Utils.isServer(server), Message.INVALID_SERVER);
				checkArgument(ban.isBan(pName, server), NOT_BAN.replaceAll("%entity%", pName));

				// Command pattern : /ban <name> <server>
				if(args.length == 2) {
					returnedMsg = ban.unBan(pName, server, sender.getName(), IModule.NO_REASON);
				} else{
					final String reason = Utils.getFinalArg(args, 2);
					returnedMsg = ban.unBan(pName, server, sender.getName(), reason);
				}
			}


			BAT.broadcast(returnedMsg, BAN_PERM);
		}	
	}
	@RunAsync
	public static class PardonIPCmd extends BATCommand{
		public PardonIPCmd() {super("pardonip", "<player/ip> [server] [reason]", "Unban IP from the specified server", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			final String entity = args[0];
			String returnedMsg = null;

			// Command pattern : /ban <name>
			if(args.length == 1){
				checkArgument(isPlayer(sender), Message.SPECIFY_SERVER);
				final String server = ((ProxiedPlayer)sender).getServer().getInfo().getName();
				checkArgument(ban.isBan(entity, server), NOT_BANIP.replaceAll("%entity%", entity));

				returnedMsg = ban.unBanIP(entity, server, sender.getName(), IModule.NO_REASON);
			}
			else{
				final String server = args[1];
				checkArgument(Utils.isServer(server), Message.INVALID_SERVER);
				checkArgument(ban.isBan(entity, server), NOT_BANIP.replaceAll("%entity%", entity));

				// Command pattern : /ban <name> <server>
				if(args.length == 2) {
					returnedMsg = ban.unBanIP(entity, server, sender.getName(), IModule.NO_REASON);
				} else{
					final String reason = Utils.getFinalArg(args, 2);
					returnedMsg = ban.unBanIP(entity, server, sender.getName(), reason);
				}
			}


			BAT.broadcast(returnedMsg, BAN_PERM);
		}	
	}
	@RunAsync
	public static class GPardonCmd extends BATCommand{
		public GPardonCmd() {super("gpardon", "<player> [reason]", "Unban the player from the whole network", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			final String pName = args[0];
			String returnedMsg = null;

			checkArgument(ban.isBan(pName, IModule.ANY_SERVER), NOT_BAN.replaceAll("%entity%", pName));

			if(args.length == 1) {
				returnedMsg = ban.unBan(pName, IModule.ANY_SERVER, sender.getName(), IModule.NO_REASON);
			} else{
				final String reason = Utils.getFinalArg(args, 1);
				returnedMsg = ban.unBan(pName, IModule.ANY_SERVER, sender.getName(), reason);
			}

			BAT.broadcast(returnedMsg, BAN_PERM);
		}	
	}
	@RunAsync
	public static class GPardonIPCmd extends BATCommand{
		public GPardonIPCmd() {super("gpardonip", "<player/ip> [reason]", "Unban IP from the whole network", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			final String entity = args[0];
			String returnedMsg = null;

			checkArgument(ban.isBan(entity, IModule.ANY_SERVER), NOT_BAN_ANY.replaceAll("%entity%", entity));

			if(args.length == 1){
				returnedMsg = ban.unBanIP(entity, IModule.ANY_SERVER, sender.getName(), IModule.NO_REASON);
			}
			else{
				final String reason = Utils.getFinalArg(args, 1);
				returnedMsg = ban.unBanIP(entity, IModule.ANY_SERVER, sender.getName(), reason);
			}

			BAT.broadcast(returnedMsg, BAN_PERM);
		}	
	}
}