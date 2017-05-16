package fr.Alphart.BAT.Modules.Watch;

import static com.google.common.base.Preconditions.checkArgument;
import static fr.Alphart.BAT.I18n.I18n._;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import com.google.common.base.Joiner;

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

public class WatchCommand extends CommandHandler {
	private static Watch watch;

	public WatchCommand(final Watch watchModule) {
		super(watchModule);
		watch = watchModule;
	}

	@RunAsync
	public static class WatchCmd extends BATCommand {
		public WatchCmd() {
			super("watch", "<player> [server] [reason]",
					"Watch the player on username basis on the specified server permanently or until unwatched.",
					Action.WATCH.getPermission());
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			if (args[0].equals("help")) {
				try {
					FormatUtils.showFormattedHelp(BAT.getInstance().getModules().getModule("watch").getCommands(),
							sender, "WATCH");
				} catch (final InvalidModuleException e) {
					e.printStackTrace();
				}
				return;
			}
			handleWatchCommand(this, false, false, sender, args, confirmedCmd);
		}
	}

	@RunAsync
	public static class WatchIPCmd extends BATCommand {
		public WatchIPCmd() {
			super(
					"watchip",
					"<player/ip> [server] [reason]",
					"Watch player on an IP basis on the specified server permanently or until unbanned. No player logged in with that IP will be able to speak.",
					Action.WATCHIP.getPermission());
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleWatchCommand(this, false, true, sender, args, confirmedCmd);
		}
	}

	@RunAsync
	public static class GWatchCmd extends BATCommand {
		public GWatchCmd() {
			super(
					"gwatch",
					"<name> [reason]",
					"Watch the player on username basis on all servers (the whole network) permanently or until unbanned. Staff will be notified when the player connects",
					Action.WATCH.getPermission() + ".global");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleWatchCommand(this, true, false, sender, args, confirmedCmd);
		}
	}

	@RunAsync
	public static class GWatchIPCmd extends BATCommand {
		public GWatchIPCmd() {
			super(
					"gwatchip",
					"<player/ip> [reason]",
					"Watch player on an IP basis on all servers (the whole network) permanently or until unwatched. Staff will be notified when any player connects from that IP.",
					Action.WATCHIP.getPermission() + ".global");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleWatchCommand(this, true, true, sender, args, confirmedCmd);
		}
	}

	public static void handleWatchCommand(final BATCommand command, final boolean global, final boolean ipWatch,
			final CommandSender sender, final String[] args, final boolean confirmedCmd) {
		String target = args[0];
		String server = IModule.GLOBAL_SERVER;
		final String staff = sender.getName();
		String reason = IModule.NO_REASON;

		final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(target);

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
		if (!Utils.validIP(target) && player == null) {
			ip = Core.getPlayerIP(target);
			if (ipWatch) {
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
			checkArgument(PermissionManager.canExecuteAction((ipWatch) ? Action.WATCHIP : Action.WATCH, sender, server),
					_("noPerm"));
		}
		target = (ip == null) ? target : ip;

		checkArgument(!PermissionManager.isExemptFrom(Action.WATCH, target), _("isExempt"));

		checkArgument(!watch.isWatched((ip == null) ? target : ip, server, false), _("alreadyWatch"));

		if (ipWatch && !BAT.getInstance().getRedis().isRedisEnabled() && player != null) {
			returnedMsg = watch.watchIP(player, server, staff, 0, reason);
		} else {
			returnedMsg = watch.watch(target, server, staff, 0, reason);
		}

		BAT.broadcast(returnedMsg, Action.WATCH_BROADCAST.getPermission());
	}

	@RunAsync
	public static class TempWatchCmd extends BATCommand {
		public TempWatchCmd() {
			super("tempwatch", "<player/ip> <duration> [server] [reason]",
					"Temporarily watch the player on username basis on from the specified server for duration.",
					Action.TEMPWATCH.getPermission());
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleTempWatchCommand(this, false, false, sender, args, confirmedCmd);
		}
	}

	@RunAsync
	public static class TempWatchIPCmd extends BATCommand {
		public TempWatchIPCmd() {
			super(
					"tempwatchip",
					"<player> <duration> [server] [reason]",
					"Temporarily watch the player on IP basis on the specified server for duration. No player logged in with that IP will be able to speak.",
					Action.TEMPWATCHIP.getPermission());
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleTempWatchCommand(this, false, true, sender, args, confirmedCmd);
		}
	}

	@RunAsync
	public static class GTempWatchCmd extends BATCommand {
		public GTempWatchCmd() {
			super("gtempwatch", "<player> <duration> [reason]",
					"Temporarily watch the player on username basis on all servers (the whole network) for duration.",
					Action.TEMPWATCH.getPermission() + ".global");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleTempWatchCommand(this, true, false, sender, args, confirmedCmd);
		}
	}

	@RunAsync
	public static class GTempWatchIPCmd extends BATCommand {
		public GTempWatchIPCmd() {
			super(
					"gtempwatchip",
					"<player/ip> <duration> [reason]",
					"Temporarily watch the player on IP basis on all servers (the whole network) for duration. Staff will be notified when any player connects from that IP.",
					Action.TEMPWATCHIP.getPermission() + ".global");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleTempWatchCommand(this, true, true, sender, args, confirmedCmd);
		}
	}

	public static void handleTempWatchCommand(final BATCommand command, final boolean global, final boolean ipWatch,
			final CommandSender sender, final String[] args, final boolean confirmedCmd) {
		String target = args[0];
		String server = IModule.GLOBAL_SERVER;
		final String staff = sender.getName();
		String reason = IModule.NO_REASON;
		final long expirationTimestamp = Utils.parseDuration(args[1]);

		final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(target);

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
		if (!Utils.validIP(target) && player == null) {
			ip = Core.getPlayerIP(target);
			if (ipWatch) {
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
					PermissionManager.canExecuteAction((ipWatch) ? Action.TEMPWATCHIP : Action.TEMPWATCH, sender, server),
					_("noPerm"));
		}
		target = (ip == null) ? target : ip;

		checkArgument(!PermissionManager.isExemptFrom(Action.WATCH, target), _("isExempt"));

		checkArgument(!watch.isWatched((ip == null) ? target : ip, server, false), _("alreadyWatch"));

		if (ipWatch && !BAT.getInstance().getRedis().isRedisEnabled() && player != null) {
			returnedMsg = watch.watchIP(player, server, staff, expirationTimestamp, reason);
		} else {
			returnedMsg = watch.watch(target, server, staff, expirationTimestamp, reason);
		}

		BAT.broadcast(returnedMsg, Action.WATCH_BROADCAST.getPermission());
	}

	@RunAsync
	public static class UnwatchCmd extends BATCommand {
		public UnwatchCmd() {
			super("unwatch", "<player> [server] [reason]",
					"Unwatch the player on a username basis from the specified server.", Action.UNWATCH.getPermission());
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleUnwatchCommand(this, false, false, sender, args, confirmedCmd);
		}
	}

	@RunAsync
	public static class UnwatchIPCmd extends BATCommand {
		public UnwatchIPCmd() {
			super("unwatchip", "<player/ip> [server] [reason]",
					"Unwatch the player on a username basis from all servers (the whole network).", Action.UNWATCHIP
							.getPermission());
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleUnwatchCommand(this, false, true, sender, args, confirmedCmd);
		}
	}

	@RunAsync
	public static class GUnwatchCmd extends BATCommand {
		public GUnwatchCmd() {
			super("gunwatch", "<player> [reason]", "Unwatch the player on an IP basis from the specified server.",
					Action.UNWATCH.getPermission() + ".global");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleUnwatchCommand(this, true, false, sender, args, confirmedCmd);
		}
	}

	@RunAsync
	public static class GUnwatchIPCmd extends BATCommand {
		public GUnwatchIPCmd() {
			super("gunwatchip", "<player/ip> [reason]",
					"Unwatch the player on an IP basis from all servers (the whole network).", Action.UNWATCHIP
							.getPermission() + ".global");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleUnwatchCommand(this, true, true, sender, args, confirmedCmd);
		}
	}

	public static void handleUnwatchCommand(final BATCommand command, final boolean global, final boolean ipUnwatch,
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
		if (!Utils.validIP(target) && ipUnwatch) {
			ip = Core.getPlayerIP(target);
			checkArgument(!"0.0.0.0".equals(ip), _("ipUnknownPlayer"));
		}

		if (!global) {
			checkArgument(
					PermissionManager.canExecuteAction((ipUnwatch) ? Action.UNWATCHIP : Action.UNWATCH, sender, server),
					_("noPerm"));
		}
		target = (ip == null) ? target : ip;

		final String[] formatArgs = { args[0] };

		checkArgument(
				watch.isWatched((ip == null) ? target : ip, server, true),
				(IModule.ANY_SERVER.equals(server) ? _("notWatchedAny", formatArgs) : ((ipUnwatch) ? _("notWatchedIP",
						formatArgs) : _("notWatched", formatArgs))));

		if (ipUnwatch) {
			returnedMsg = watch.unWatchIP(target, server, staff, reason);
		} else {
			returnedMsg = watch.unWatch(target, server, staff, reason);
		}

		BAT.broadcast(returnedMsg, Action.WATCH_BROADCAST.getPermission());
	}
}