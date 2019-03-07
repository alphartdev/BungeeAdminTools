package fr.Alphart.BAT.Modules.Ban;

import static com.google.common.base.Preconditions.checkArgument;
import static fr.Alphart.BAT.I18n.I18n._;

import java.util.UUID;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import com.google.common.base.Joiner;
import com.imaginarycode.minecraft.redisbungee.RedisBungee;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.BATCommand;
import fr.Alphart.BAT.Modules.BATCommand.RunAsync;
import fr.Alphart.BAT.Modules.CommandHandler;
import fr.Alphart.BAT.Modules.IModule;
import fr.Alphart.BAT.Modules.InvalidModuleException;
import fr.Alphart.BAT.Modules.Core.Core;
import fr.Alphart.BAT.Modules.Core.PermissionManager;
import fr.Alphart.BAT.Modules.Core.PermissionManager.Action;
import fr.Alphart.BAT.Utils.FormatUtils;
import fr.Alphart.BAT.Utils.Utils;

public class BanCommand extends CommandHandler {
	private static Ban ban;

	public BanCommand(final Ban banModule) {
		super(banModule);
		ban = banModule;
	}

	@RunAsync
	public static class BanCmd extends BATCommand {
		public BanCmd() {
			super("ban", "<player> [server] [reason]",
					"Ban the player on username basis on the specified server permanently or until unbanned.",
					Action.BAN.getPermission());
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			if (args[0].equals("help")) {
				try {
					FormatUtils.showFormattedHelp(BAT.getInstance().getModules().getModule("ban").getCommands(),
							sender, "BAN");
				} catch (final InvalidModuleException e) {
					e.printStackTrace();
				}
				return;
			}
			handleBanCommand(this, false, false, sender, args, confirmedCmd);
		}
	}

	@RunAsync
	public static class BanIPCmd extends BATCommand {
		public BanIPCmd() {
			super("banip", "<player/ip> [server] [reason]",
					"Ban player on an IP basis on the specified server permanently or until unbanned. ", Action.BANIP
							.getPermission());
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleBanCommand(this, false, true, sender, args, confirmedCmd);
		}
	}

	@RunAsync
	public static class GBanCmd extends BATCommand {
		public GBanCmd() {
			super(
					"gban",
					"<player> [reason]",
					"Ban the player on username basis on all servers (the whole network) permanently or until unbanned.",
					Action.BAN.getPermission() + ".global");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleBanCommand(this, true, false, sender, args, confirmedCmd);
		}
	}

	@RunAsync
	public static class GBanIPCmd extends BATCommand {
		public GBanIPCmd() {
			super("gbanip", "<player/ip> [reason]",
					"Ban player on an IP basis on all servers (the whole network) permanently or until unbanned.",
					Action.BANIP.getPermission() + ".global");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleBanCommand(this, true, true, sender, args, confirmedCmd);
		}
	}

	public static void handleBanCommand(final BATCommand command, final boolean global, final boolean ipBan,
			final CommandSender sender, final String[] args, final boolean confirmedCmd) {
		String target = args[0];
		String server = IModule.GLOBAL_SERVER;
		final String staff = sender.getName();
		String reason = IModule.NO_REASON;

		final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(target);
		
		UUID pUUID = null;
		if (BAT.getInstance().getRedis().isRedisEnabled()) {
		    UUID tempUUID = RedisBungee.getApi().getUuidFromName(target, false);
		    if (tempUUID != null && RedisBungee.getApi().isPlayerOnline(tempUUID)) pUUID = tempUUID;
		}


		String ip = null;

		String returnedMsg;

		if (global) {
			if (args.length > 1) {
				reason = Utils.getFinalArg(args, 1);
			}
		} else {
			if (args.length == 1) {
				checkArgument(sender instanceof ProxiedPlayer, _("specifyServer"));
				server = ((ProxiedPlayer) sender).getServer().getInfo().getName();
			} else {
				checkArgument(Utils.isServer(args[1]), _("invalidServer"));
				server = args[1];
				reason = (args.length > 2) ? Utils.getFinalArg(args, 2) : IModule.NO_REASON;
			}
		}
                
                
        checkArgument(
                !reason.equalsIgnoreCase(IModule.NO_REASON) || !BAT.getInstance().getConfiguration().isMustGiveReason(),
                _("noReasonInCommand"));
                

		// Check if the target isn't an ip and the player is offline
		if (!Utils.validIP(target) && player == null && pUUID == null) {
			ip = Core.getPlayerIP(target);
			if (ipBan) {
				checkArgument(!"0.0.0.0".equals(ip), _("ipUnknownPlayer"));
			}
			// If ip = 0.0.0.0, it means the player never connects
			else {
				if ("0.0.0.0".equals(ip) && !confirmedCmd) {
					command.mustConfirmCommand(sender, command.getName() + " " + Joiner.on(' ').join(args),
							_("operationUnknownPlayer", new String[] { target }));
					return;
				}
				// Set the ip to null to avoid checking if the ip is banned
				ip = null;
			}
		}

		if (!global) {
			checkArgument(PermissionManager.canExecuteAction((ipBan) ? Action.BANIP : Action.BAN, sender, server),
					_("noPerm"));
		}
		target = (ip == null) ? target : ip;

		// We just check if the target is exempt from the ban, which means he's
		// exempt from the full module command
		checkArgument(!PermissionManager.isExemptFrom(Action.BAN, target), _("isExempt"));

		checkArgument(!ban.isBan((ip == null) ? target : ip, server), _("alreadyBan"));

		if (ipBan && player != null) {
			returnedMsg = ban.banIP(player, server, staff, 0, reason);
		} else if (ipBan && pUUID != null) {
		        returnedMsg = ban.banRedisIP(pUUID, server, staff, 0, reason);
		} else {
			returnedMsg = ban.ban(target, server, staff, 0, reason);
		}

		BAT.broadcast(returnedMsg, Action.banBroadcast.getPermission());
	}

	@RunAsync
	public static class TempBanCmd extends BATCommand {
		public TempBanCmd() {
			super("tempban", "<player> <duration> [server] [reason]",
					"Temporarily ban the player on username basis on from the specified server for duration.",
					Action.TEMPBAN.getPermission());
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleTempBanCommand(this, false, false, sender, args, confirmedCmd);
		}
	}

	@RunAsync
	public static class TempBanIPCmd extends BATCommand {
		public TempBanIPCmd() {
			super("tempbanip", "<player/ip> <duration> [server] [reason]",
					"Temporarily ban the player on IP basis on the specified server for duration.", Action.TEMPBANIP
							.getPermission());
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleTempBanCommand(this, false, true, sender, args, confirmedCmd);
		}
	}

	@RunAsync
	public static class GTempBanCmd extends BATCommand {
		public GTempBanCmd() {
			super("gtempban", "<player> <duration> [reason]",
					"Temporarily ban the player on username basis on all servers (the whole network) for duration.",
					Action.TEMPBAN.getPermission() + ".global");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleTempBanCommand(this, true, false, sender, args, confirmedCmd);
		}
	}

	@RunAsync
	public static class GTempBanIPCmd extends BATCommand {
		public GTempBanIPCmd() {
			super("gtempbanip", "<player/ip> <duration> [reason]",
					"Temporarily ban the player on IP basis on all servers (the whole network) for duration.",
					Action.TEMPBANIP.getPermission() + ".global");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleTempBanCommand(this, true, true, sender, args, confirmedCmd);
		}
	}

	public static void handleTempBanCommand(final BATCommand command, final boolean global, final boolean ipBan,
			final CommandSender sender, final String[] args, final boolean confirmedCmd) {
		String target = args[0];
		final long expirationTimestamp = Utils.parseDuration(args[1]);
		String server = IModule.GLOBAL_SERVER;
		final String staff = sender.getName();
		String reason = IModule.NO_REASON;

		final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(target);
		
		UUID pUUID = null;
		if (BAT.getInstance().getRedis().isRedisEnabled()) {
		    UUID tempUUID = RedisBungee.getApi().getUuidFromName(target, false);
		    if (tempUUID != null && RedisBungee.getApi().isPlayerOnline(tempUUID)) pUUID = tempUUID;
		}

		String ip = null;

		String returnedMsg;

		if (global) {
			if (args.length > 2) {
				reason = Utils.getFinalArg(args, 2);
			}
		} else {
			if (args.length == 2) {
				checkArgument(sender instanceof ProxiedPlayer, _("specifyServer"));
				server = ((ProxiedPlayer) sender).getServer().getInfo().getName();
			} else {
				checkArgument(Utils.isServer(args[2]), _("invalidServer"));
				server = args[2];
				reason = (args.length > 3) ? Utils.getFinalArg(args, 3) : IModule.NO_REASON;
			}
		}

                
        checkArgument(
                !reason.equalsIgnoreCase(IModule.NO_REASON) || !BAT.getInstance().getConfiguration().isMustGiveReason(),
                _("noReasonInCommand"));
                
		// Check if the target isn't an ip and the player is offline
		if (!Utils.validIP(target) && player == null && pUUID == null) {
			ip = Core.getPlayerIP(target);
			if (ipBan) {
				checkArgument(!"0.0.0.0".equals(ip), _("ipUnknownPlayer"));
			} else {
				// If ip = 0.0.0.0, it means the player never connects
				if ("0.0.0.0".equals(ip) && !confirmedCmd) {
					command.mustConfirmCommand(sender, command.getName() + " " + Joiner.on(' ').join(args),
							_("operationUnknownPlayer", new String[] { target }));
					return;
				}
				// Set the ip to null to avoid checking if the ip is banned
				ip = null;
			}
		}

		if (!global) {
			checkArgument(
					PermissionManager.canExecuteAction((ipBan) ? Action.TEMPBANIP : Action.TEMPBAN, sender, server),
					_("noPerm"));
		}
		target = (ip == null) ? target : ip;
		
		checkArgument(!PermissionManager.isExemptFrom(Action.BAN, target), _("isExempt"));
		checkArgument(!ban.isBan(target, server), _("alreadyBan"));

		if (ipBan && player != null) {
			returnedMsg = ban.banIP(player, server, staff, expirationTimestamp, reason);
		} else if (ipBan && pUUID != null) {
	        returnedMsg = ban.banRedisIP(pUUID, server, staff, expirationTimestamp, reason);
		} else {
			returnedMsg = ban.ban(target , server, staff, expirationTimestamp, reason);
		}

		BAT.broadcast(returnedMsg, Action.banBroadcast.getPermission());
	}

	@RunAsync
	public static class UnbanCmd extends BATCommand {
		public UnbanCmd() {
			super("unban", "<player> [server] [reason]",
					"Unban the player on a username basis from the specified server.", Action.UNBAN.getPermission(),
					"pardon");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handlePardonCommand(this, false, false, sender, args, confirmedCmd);
		}
	}

	@RunAsync
	public static class UnbanIPCmd extends BATCommand {
		public UnbanIPCmd() {
			super("unbanip", "<player/ip> [server] [reason]", "Unban IP from the specified server", Action.UNBANIP
					.getPermission(), "pardonip");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handlePardonCommand(this, false, true, sender, args, confirmedCmd);
		}
	}

	@RunAsync
	public static class GUnbanCmd extends BATCommand {
		public GUnbanCmd() {
			super("gunban", "<player> [reason]",
					"Unban the player on a username basis from all servers (the whole network).", Action.UNBAN
							.getPermission() + ".global", "gpardon");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handlePardonCommand(this, true, false, sender, args, confirmedCmd);
		}
	}

	@RunAsync
	public static class GUnbanIPCmd extends BATCommand {
		public GUnbanIPCmd() {
			super("gunbanip", "<player/ip> [reason]",
					"Unban the player on an IP basis from all servers (the whole network).", Action.UNBANIP
							.getPermission() + ".global", "gpardonip");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handlePardonCommand(this, true, true, sender, args, confirmedCmd);
		}
	}

	public static void handlePardonCommand(final BATCommand command, final boolean global, final boolean ipUnban,
			final CommandSender sender, final String[] args, final boolean confirmedCmd) {
		String target = args[0];
		String server = IModule.ANY_SERVER;
		final String staff = sender.getName();
		String reason = IModule.NO_REASON;

		String ip = null;

		String returnedMsg;

		if (global) {
			if (args.length > 1) {
				reason = Utils.getFinalArg(args, 1);
			}
		} else {
			if (args.length == 1) {
				checkArgument(sender instanceof ProxiedPlayer, _("specifyServer"));
				server = ((ProxiedPlayer) sender).getServer().getInfo().getName();
			} else {
				checkArgument(Utils.isServer(args[1]), _("invalidServer"));
				server = args[1];
				reason = (args.length > 2) ? Utils.getFinalArg(args, 2) : IModule.NO_REASON;
			}
		}
                
                
        checkArgument(
                !reason.equalsIgnoreCase(IModule.NO_REASON) || !BAT.getInstance().getConfiguration().isMustGiveReason(),
                _("noReasonInCommand"));
                

		// Check if the target isn't an ip and the player is offline
		if (!Utils.validIP(target) && ipUnban) {
			ip = Core.getPlayerIP(target);
			checkArgument(!"0.0.0.0".equals(ip), _("ipUnknownPlayer"));
		}

		if (!global) {
			checkArgument(
					PermissionManager.canExecuteAction((ipUnban) ? Action.UNBANIP : Action.UNBAN, sender, server),
					_("noPerm"));
		}
		target = (ip == null) ? target : ip;

		final String[] formatArgs = { args[0] };

		checkArgument(
				ban.isBan((ip == null) ? target : ip, server),
				(IModule.ANY_SERVER.equals(server) ? _("notBannedAny", formatArgs) : ((ipUnban) ? _("notBannedIP",
						formatArgs) : _("notBanned", formatArgs))));

		if (ipUnban) {
			returnedMsg = ban.unBanIP(target, server, staff, reason);
		} else {
			returnedMsg = ban.unBan(target, server, staff, reason);
		}

		BAT.broadcast(returnedMsg, Action.banBroadcast.getPermission());
	}
}