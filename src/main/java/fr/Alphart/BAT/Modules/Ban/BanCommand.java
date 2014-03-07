package fr.Alphart.BAT.Modules.Ban;

import static com.google.common.base.Preconditions.checkArgument;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import com.google.common.base.Joiner;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Message;
import fr.Alphart.BAT.Modules.BATCommand;
import fr.Alphart.BAT.Modules.BATCommand.RunAsync;
import fr.Alphart.BAT.Modules.CommandHandler;
import fr.Alphart.BAT.Modules.IModule;
import fr.Alphart.BAT.Modules.InvalidModuleException;
import fr.Alphart.BAT.Modules.Core.Core;
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
		public void onCommand(final CommandSender sender, final String[] args, boolean confirmed) throws IllegalArgumentException {
			if(args[0].equals("help")){
				try {
					FormatUtils.showFormattedHelp(BAT.getInstance().getModules().getModule("ban").getCommands(), sender, "BAN");
				} catch (final InvalidModuleException e) {
					e.printStackTrace();
				}
				return;
			}
			handleBanCommand(this, false, false, sender, args, confirmed);
		}
	}
	@RunAsync
	public static class BanIPCmd extends BATCommand{
		public BanIPCmd() {super("banip", "<player/ip> [server] [reason]", "Ban definitively player's IP from the specified server", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, boolean confirmed) throws IllegalArgumentException {
			handleBanCommand(this, false, true, sender, args, confirmed);
		}
	}
	@RunAsync
	public static class GBanCmd extends BATCommand{
		public GBanCmd() {super("gban", "<player> [reason]", "Ban definitively the player from the whole network", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, boolean confirmed) throws IllegalArgumentException {
			handleBanCommand(this, true, false, sender, args, confirmed);
		}
	}
	@RunAsync
	public static class GBanIPCmd extends BATCommand{
		public GBanIPCmd() {super("gbanip", "<player/ip> [reason]", "Ban definitively player's IP from the whole network", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, boolean confirmed) throws IllegalArgumentException {
			handleBanCommand(this, true, true, sender, args, confirmed);
		}
	}
	public static void handleBanCommand(final BATCommand command, final boolean global, final boolean ipBan, final CommandSender sender, final String[] args, boolean confirmedCmd){
		String target = args[0];
		String server;
		final String staff = sender.getName();
		String reason = IModule.NO_REASON;

		final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(target);

		String returnedMsg;

		if(global){
			server = IModule.GLOBAL_SERVER;
			if(args.length > 1){
				reason = Utils.getFinalArg(args, 1);
			}
		}
		else
		{
			if(args.length == 1){
				checkArgument(sender instanceof ProxiedPlayer, Message.SPECIFY_SERVER);
				server = ((ProxiedPlayer)sender).getServer().getInfo().getName();
			}
			else{
				server = args[1];
				checkArgument(Utils.isServer(args[1]), Message.INVALID_SERVER);
				reason = (args.length > 2) ? Utils.getFinalArg(args, 2) : IModule.NO_REASON;
			}
		}

		// Check if the target isn't an ip and the player is offline
		if(!Utils.validIP(target) && player == null)
		{
			final String ip = Core.getPlayerIP(target);
			if(ipBan){
				checkArgument(!"0.0.0.0".equals(ip), "You can't ban this user's ip because he never connected to the server.");
				target = ip;
			}			
			// If ip = 0.0.0.0, it means the player never connects
			else if("0.0.0.0".equals(ip) && !confirmedCmd)
			{
				command.mustConfirmCommand(sender, command.getName() + " " + Joiner.on(' ').join(args), "you're performing operation on a player which never connects on the server.");
				return;
			}
		}

		checkArgument(!ban.isBan(target, server), ALREADY_BAN);

		if(ipBan && player != null){
			returnedMsg = ban.banIP(player, server, staff, 0, reason);
		}else{
			returnedMsg = ban.ban(target, server, staff, 0, reason);
		}

		BAT.broadcast(returnedMsg, BAN_PERM);
	}

	@RunAsync
	public static class TempBanCmd extends BATCommand{
		public TempBanCmd() {super("tempban", "<player> <duration> [server] [reason]", "Ban temporarily the player from the specified server", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, boolean confirmed) throws IllegalArgumentException {
			handleTempBanCommand(this, false, false, sender, args, confirmed); 
		}
	}
	@RunAsync
	public static class TempBanIPCmd extends BATCommand{
		public TempBanIPCmd() {super("tempbanip", "<player/ip> <duration> [server] [reason]", "Ban temporarily player's IP from the specified server", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, boolean confirmed) throws IllegalArgumentException {
			handleTempBanCommand(this, false, true, sender, args, confirmed);
		}
	}
	@RunAsync
	public static class GTempBanCmd extends BATCommand{
		public GTempBanCmd() {super("gtempban", "<player> <duration> [reason]", "Ban temporarily the player from the whole network", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, boolean confirmed) throws IllegalArgumentException {
			handleTempBanCommand(this, true, false, sender, args, confirmed);
		}
	}
	@RunAsync
	public static class GTempBanIPCmd extends BATCommand{
		public GTempBanIPCmd() {super("gtempbanip", "<player/ip> <duration> [reason]", "Ban temporarily player's IP from the whole network", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, boolean confirmed) throws IllegalArgumentException {
			handleTempBanCommand(this, true, true, sender, args, confirmed);
		}
	}
	public static void handleTempBanCommand(final BATCommand command, final boolean global, final boolean ipBan, final CommandSender sender, final String[] args, boolean confirmedCmd){
		String target = args[0];
		int duration = Utils.parseDateDiff(args[1], true) - DataSourceHandler.getTimestamp();
		String server;
		final String staff = sender.getName();
		String reason = IModule.NO_REASON;

		final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(target);
		
		String returnedMsg;

		if(global){
			server = IModule.GLOBAL_SERVER;
			if(args.length > 2){
				reason = Utils.getFinalArg(args, 2);
			}
		}
		else
		{
			if(args.length == 2){
				checkArgument(sender instanceof ProxiedPlayer, Message.SPECIFY_SERVER);
				server = ((ProxiedPlayer)sender).getServer().getInfo().getName();
			}
			else{
				checkArgument(Utils.isServer(args[2]), Message.INVALID_SERVER);
				server = args[2];
				reason = (args.length > 3) ? Utils.getFinalArg(args, 3) : IModule.NO_REASON;
			}
		}

		// Check if the target isn't an ip and the player is offline
		if(!Utils.validIP(target) && player == null)
		{
			final String ip = Core.getPlayerIP(target);
			if(ipBan){
				checkArgument(!"0.0.0.0".equals(ip), "You can't ban this user's ip because he never connected to the server.");
				target = ip;
			}
			// If ip = 0.0.0.0, it means the player never connects
			else if("0.0.0.0".equals(ip) && !confirmedCmd)
			{
				command.mustConfirmCommand(sender, command.getName() + " " + Joiner.on(' ').join(args), "you're performing operation on a player which never connects on the server.");
				return;
			}
		}

		checkArgument(!ban.isBan(target, server), ALREADY_BAN);

		if(ipBan && player != null){
			returnedMsg = ban.banIP(player, server, staff, duration, reason);
		}else{
			returnedMsg = ban.ban(target, server, staff, duration, reason);
		}

		BAT.broadcast(returnedMsg, BAN_PERM);
	}
	
	@RunAsync
	public static class PardonCmd extends BATCommand{
		public PardonCmd() {super("pardon", "<player> [server] [reason]", "Unban the player from the specified server", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, boolean confirmed) throws IllegalArgumentException {
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
		public void onCommand(final CommandSender sender, final String[] args, boolean confirmed) throws IllegalArgumentException {
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
		public void onCommand(final CommandSender sender, final String[] args, boolean confirmed) throws IllegalArgumentException {
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
		public void onCommand(final CommandSender sender, final String[] args, boolean confirmed) throws IllegalArgumentException {
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