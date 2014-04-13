package fr.Alphart.BAT.Modules.Kick;

import static com.google.common.base.Preconditions.checkArgument;
import static fr.Alphart.BAT.I18n.I18n._;
import static fr.Alphart.BAT.I18n.I18n.__;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import com.google.common.base.Joiner;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.BATCommand;
import fr.Alphart.BAT.Modules.CommandHandler;
import fr.Alphart.BAT.Modules.IModule;
import fr.Alphart.BAT.Modules.InvalidModuleException;
import fr.Alphart.BAT.Modules.Core.Comment.Type;
import fr.Alphart.BAT.Modules.Core.Core;
import fr.Alphart.BAT.Modules.Core.PermissionManager;
import fr.Alphart.BAT.Modules.Core.PermissionManager.Action;
import fr.Alphart.BAT.Utils.FormatUtils;
import fr.Alphart.BAT.Utils.Utils;

public class KickCommand extends CommandHandler {
	private static Kick kick;

	public KickCommand(final Kick kickModule) {
		super(kickModule);
		kick = kickModule;
	}

	public static class KickCmd extends BATCommand {
		public KickCmd() {
			super("kick", "<player> [reason]", "Kick the player from his current server to the lobby", Action.KICK
					.getPermission());
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			if (args[0].equals("help")) {
				try {
					FormatUtils.showFormattedHelp(BAT.getInstance().getModules().getModule("kick").getCommands(),
							sender, "KICK");
				} catch (final InvalidModuleException e) {
					e.printStackTrace();
				}
				return;
			}
			final String pName = args[0];
			final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
			checkArgument(player != null, _("playerNotFound"));
			final String pServer = player.getServer().getInfo().getName();
			checkArgument(
					pServer != null && !pServer.equals(player.getPendingConnection().getListener().getDefaultServer()),
					_("CANT_KICK_DEFAULT_SERVER", new String[] { pName }));

			checkArgument(
					PermissionManager.canExecuteAction(Action.KICK, sender, player.getServer().getInfo().getName()),
					_("NO_PERM"));

			checkArgument(!PermissionManager.isExemptFrom(Action.KICK, pName), _("IS_EXEMPT"));

			final String returnedMsg = kick.kick(player, sender.getName(),
					(args.length == 1) ? IModule.NO_REASON : Utils.getFinalArg(args, 1));

			BAT.broadcast(returnedMsg, Action.KICK_BROADCAST.getPermission());
		}
	}

	public static class GKickCmd extends BATCommand {
		public GKickCmd() {
			super("gkick", "<player> [reason]", "Kick the player from the network", Action.KICK.getPermission()
					+ ".global");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			final String pName = args[0];
			final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
			checkArgument(player != null, _("playerNotFound"));

			checkArgument(!PermissionManager.isExemptFrom(Action.KICK, pName), _("IS_EXEMPT"));

			final String returnedMsg = kick.gKick(player, sender.getName(),
					(args.length == 1) ? IModule.NO_REASON : Utils.getFinalArg(args, 1));

			BAT.broadcast(returnedMsg, Action.KICK_BROADCAST.getPermission());
		}
	}

	public static class WarnCmd extends BATCommand {
		public WarnCmd() { super("warn", "<player> <reason>", "Warn the player", Action.WARN.getPermission());}

		@Override
		public void onCommand(CommandSender sender, String[] args, boolean confirmedCmd)
				throws IllegalArgumentException {
			final ProxiedPlayer target = ProxyServer.getInstance().getPlayer(args[0]);
			final String reason = Utils.getFinalArg(args, 1);
			if(target == null){
				if(!confirmedCmd && Core.getPlayerIP(args[0]).equals("0.0.0.0")){
					mustConfirmCommand(sender, getName() + " " + Joiner.on(' ').join(args),
							_("operationUnknownPlayer", new String[] {args[0]}));
					return;
				}
			}
			else{
				target.sendMessage(__("WAS_WARNED_NOTIF", new String[] {reason}));
			}
			
			if(sender instanceof ProxiedPlayer){
				checkArgument(PermissionManager.canExecuteAction(Action.WARN , sender, ((ProxiedPlayer)sender).getServer().getInfo().getName()),
						_("NO_PERM"));
			}
			
			Core.insertComment(args[0], reason, Type.WARNING, sender.getName());
			
			BAT.broadcast(_("warnBroadcast", new String[]{args[0], sender.getName(), reason}), Action.KICK_BROADCAST.getPermission());
			return;
		}
	}
}
