package fr.Alphart.BAT.Modules.Mute;

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

public class MuteCommand extends CommandHandler {
	private static Mute mute;

	public MuteCommand(final Mute muteModule) {
		super(muteModule);
		mute = muteModule;
	}

	@RunAsync
	public static class MuteCmd extends BATCommand {
		public MuteCmd() {
			super("mute", "<player> [server] [reason]",
					"Mute the player on username basis on the specified server permanently or until unbanned.",
					Action.MUTE.getPermission());
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			if (args[0].equals("help")) {
				try {
					FormatUtils.showFormattedHelp(BAT.getInstance().getModules().getModule("mute").getCommands(),
							sender, "MUTE");
				} catch (final InvalidModuleException e) {
					e.printStackTrace();
				}
				return;
			}
			handleMuteCommand(this, false, false, sender, args, confirmedCmd);
		}
	}

	@RunAsync
	public static class MuteIPCmd extends BATCommand {
		public MuteIPCmd() {
			super(
					"muteip",
					"<player/ip> [server] [reason]",
					"Mute player on an IP basis on the specified server permanently or until unbanned. No player logged in with that IP will be able to speak.",
					Action.MUTEIP.getPermission());
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleMuteCommand(this, false, true, sender, args, confirmedCmd);
		}
	}

	@RunAsync
	public static class GMuteCmd extends BATCommand {
		public GMuteCmd() {
			super(
					"gmute",
					"<name> [reason]",
					"Mute the player on username basis on all servers (the whole network) permanently or until unbanned.",
					Action.MUTE.getPermission() + ".global");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleMuteCommand(this, true, false, sender, args, confirmedCmd);
		}
	}

	@RunAsync
	public static class GMuteIPCmd extends BATCommand {
		public GMuteIPCmd() {
			super(
					"gmuteip",
					"<player/ip> [reason]",
					"Mute player on an IP basis on all servers (the whole network) permanently or until unbanned. No player logged in with that IP will be able to speak.",
					Action.MUTEIP.getPermission() + ".global");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleMuteCommand(this, true, true, sender, args, confirmedCmd);
		}
	}

	public static void handleMuteCommand(final BATCommand command, final boolean global, final boolean ipMute,
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
			if (ipMute) {
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
			checkArgument(PermissionManager.canExecuteAction((ipMute) ? Action.MUTEIP : Action.MUTE, sender, server),
					_("noPerm"));
		}
		target = (ip == null) ? target : ip;

		checkArgument(!PermissionManager.isExemptFrom(Action.MUTE, target), _("isExempt"));

		checkArgument(!mute.isMute((ip == null) ? target : ip, server, false), _("alreadyMute"));

		if (ipMute && !BAT.getInstance().getRedis().isRedisEnabled() && player != null) {
			returnedMsg = mute.muteIP(player, server, staff, 0, reason);
		} else {
			returnedMsg = mute.mute(target, server, staff, 0, reason);
		}

		BAT.broadcast(returnedMsg, Action.MUTE_BROADCAST.getPermission());
	}

	@RunAsync
	public static class TempMuteCmd extends BATCommand {
		public TempMuteCmd() {
			super("tempmute", "<player/ip> <duration> [server] [reason]",
					"Temporarily mute the player on username basis on from the specified server for duration.",
					Action.TEMPMUTE.getPermission());
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleTempMuteCommand(this, false, false, sender, args, confirmedCmd);
		}
	}

	@RunAsync
	public static class TempMuteIPCmd extends BATCommand {
		public TempMuteIPCmd() {
			super(
					"tempmuteip",
					"<player> <duration> [server] [reason]",
					"Temporarily mute the player on IP basis on the specified server for duration. No player logged in with that IP will be able to speak.",
					Action.TEMPMUTEIP.getPermission());
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleTempMuteCommand(this, false, true, sender, args, confirmedCmd);
		}
	}

	@RunAsync
	public static class GTempMuteCmd extends BATCommand {
		public GTempMuteCmd() {
			super("gtempmute", "<player> <duration> [reason]",
					"Temporarily mute the player on username basis on all servers (the whole network) for duration.",
					Action.TEMPMUTE.getPermission() + ".global");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleTempMuteCommand(this, true, false, sender, args, confirmedCmd);
		}
	}

	@RunAsync
	public static class GTempMuteIPCmd extends BATCommand {
		public GTempMuteIPCmd() {
			super(
					"gtempmuteip",
					"<player/ip> <duration> [reason]",
					"Temporarily mute the player on IP basis on all servers (the whole network) for duration. No player logged in with that IP will be able to speak.",
					Action.TEMPMUTEIP.getPermission() + ".global");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleTempMuteCommand(this, true, true, sender, args, confirmedCmd);
		}
	}

	public static void handleTempMuteCommand(final BATCommand command, final boolean global, final boolean ipMute,
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
			if (ipMute) {
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
					PermissionManager.canExecuteAction((ipMute) ? Action.TEMPMUTEIP : Action.TEMPMUTE, sender, server),
					_("noPerm"));
		}
		target = (ip == null) ? target : ip;

		checkArgument(!PermissionManager.isExemptFrom(Action.MUTE, target), _("isExempt"));

		checkArgument(!mute.isMute((ip == null) ? target : ip, server, false), _("alreadyMute"));

		if (ipMute && !BAT.getInstance().getRedis().isRedisEnabled() && player != null) {
			returnedMsg = mute.muteIP(player, server, staff, expirationTimestamp, reason);
		} else {
			returnedMsg = mute.mute(target, server, staff, expirationTimestamp, reason);
		}

		BAT.broadcast(returnedMsg, Action.MUTE_BROADCAST.getPermission());
	}

	@RunAsync
	public static class UnmuteCmd extends BATCommand {
		public UnmuteCmd() {
			super("unmute", "<player> [server] [reason]",
					"Unmute the player on a username basis from the specified server.", Action.UNMUTE.getPermission());
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleUnmuteCommand(this, false, false, sender, args, confirmedCmd);
		}
	}

	@RunAsync
	public static class UnmuteIPCmd extends BATCommand {
		public UnmuteIPCmd() {
			super("unmuteip", "<player/ip> [server] [reason]",
					"Unmute the player on a username basis from all servers (the whole network).", Action.UNMUTEIP
							.getPermission());
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleUnmuteCommand(this, false, true, sender, args, confirmedCmd);
		}
	}

	@RunAsync
	public static class GUnmuteCmd extends BATCommand {
		public GUnmuteCmd() {
			super("gunmute", "<player> [reason]", "Unmute the player on an IP basis from the specified server.",
					Action.UNMUTE.getPermission() + ".global");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleUnmuteCommand(this, true, false, sender, args, confirmedCmd);
		}
	}

	@RunAsync
	public static class GUnmuteIPCmd extends BATCommand {
		public GUnmuteIPCmd() {
			super("gunmuteip", "<player/ip> [reason]",
					"Unmute the player on an IP basis from all servers (the whole network).", Action.UNMUTEIP
							.getPermission() + ".global");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			handleUnmuteCommand(this, true, true, sender, args, confirmedCmd);
		}
	}

	public static void handleUnmuteCommand(final BATCommand command, final boolean global, final boolean ipUnmute,
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
		if (!Utils.validIP(target) && ipUnmute) {
			ip = Core.getPlayerIP(target);
			checkArgument(!"0.0.0.0".equals(ip), _("ipUnknownPlayer"));
		}

		if (!global) {
			checkArgument(
					PermissionManager.canExecuteAction((ipUnmute) ? Action.UNMUTEIP : Action.UNMUTE, sender, server),
					_("noPerm"));
		}
		target = (ip == null) ? target : ip;

		final String[] formatArgs = { args[0] };

		checkArgument(
				mute.isMute((ip == null) ? target : ip, server, true),
				(IModule.ANY_SERVER.equals(server) ? _("notMutedAny", formatArgs) : ((ipUnmute) ? _("notMutedIP",
						formatArgs) : _("notMuted", formatArgs))));

		if (ipUnmute) {
			returnedMsg = mute.unMuteIP(target, server, staff, reason);
		} else {
			returnedMsg = mute.unMute(target, server, staff, reason);
		}

		BAT.broadcast(returnedMsg, Action.MUTE_BROADCAST.getPermission());
	}
}