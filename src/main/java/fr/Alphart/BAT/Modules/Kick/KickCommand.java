package fr.Alphart.BAT.Modules.Kick;

import static com.google.common.base.Preconditions.checkArgument;
import static fr.Alphart.BAT.I18n.I18n._;

import java.util.UUID;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.BATCommand;
import fr.Alphart.BAT.Modules.CommandHandler;
import fr.Alphart.BAT.Modules.IModule;
import fr.Alphart.BAT.Modules.InvalidModuleException;
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
			if (BAT.getInstance().getRedis().isRedisEnabled()) {
			    	UUID pUUID = RedisBungee.getApi().getUuidFromName(pName, true);
			    	System.out.println("checking if player is online");
			    	checkArgument(pUUID != null, _("playerNotFound"));
			    	
			    	checkArgument(
					PermissionManager.canExecuteAction(Action.KICK, sender, RedisBungee.getApi().getServerFor(pUUID).getName()),
					_("noPerm"));
			    	
			    	checkArgument(!PermissionManager.isExemptFrom(Action.KICK, pName), _("isExempt"));
			    	
			    	final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
			    	final String returnedMsg;
			    	if (player != null) {
			    	    	final String pServer = player.getServer().getInfo().getName();
			    		checkArgument(
			    			pServer != null && !pServer.equals(player.getPendingConnection().getListener().getDefaultServer()),
			    			_("cantKickDefaultServer", new String[] { pName }));
   				
			    	    	returnedMsg = kick.kick(player, sender.getName(),
			    	    		(args.length == 1) ? IModule.NO_REASON : Utils.getFinalArg(args, 1));
			    	} else {
			    	    	// Cross bungeecord standard kick is difficult to implement.
			    	        // I would have to make a round trip to check thier server and default server.
			    	        // Doing a gkick instead.
				    	returnedMsg = kick.gKickSQL(pUUID, sender.getName(),
				    		(args.length == 1) ? IModule.NO_REASON : Utils.getFinalArg(args, 1));
			    	        BAT.getInstance().getRedis().sendGKickPlayer(pUUID, returnedMsg);
			    	}
		    	    	BAT.broadcast(returnedMsg, Action.KICK_BROADCAST.getPermission());
		    	    	BAT.getInstance().getRedis().sendBroadcast(Action.KICK_BROADCAST, returnedMsg);
			} else {
			    	final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
		    		checkArgument(player != null, _("playerNotFound"));
	    			final String pServer = player.getServer().getInfo().getName();
   				checkArgument(
					pServer != null && !pServer.equals(player.getPendingConnection().getListener().getDefaultServer()),
					_("cantKickDefaultServer", new String[] { pName }));

   				checkArgument(
					PermissionManager.canExecuteAction(Action.KICK, sender, player.getServer().getInfo().getName()),
					_("noPerm"));

   				checkArgument(!PermissionManager.isExemptFrom(Action.KICK, pName), _("isExempt"));

   				final String returnedMsg = kick.kick(player, sender.getName(),
					(args.length == 1) ? IModule.NO_REASON : Utils.getFinalArg(args, 1));
   				BAT.broadcast(returnedMsg, Action.KICK_BROADCAST.getPermission());
			}
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

			if (BAT.getInstance().getRedis().isRedisEnabled()) {
			    	UUID pUUID = RedisBungee.getApi().getUuidFromName(pName, true);
			    	checkArgument(pUUID != null, _("playerNotFound"));
			    	
			    	checkArgument(!PermissionManager.isExemptFrom(Action.KICK, pName), _("isExempt"));
			    	
			    	final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
			    	final String returnedMsg;
			    	if (player != null) {
			    	    	returnedMsg = kick.gKick(player, sender.getName(),
			    	    		(args.length == 1) ? IModule.NO_REASON : Utils.getFinalArg(args, 1));
			    	} else {
				    	returnedMsg = kick.gKickSQL(pUUID, sender.getName(),
				    		(args.length == 1) ? IModule.NO_REASON : Utils.getFinalArg(args, 1));
			    	        BAT.getInstance().getRedis().sendGKickPlayer(pUUID, returnedMsg);
			    	}
		    	    	BAT.broadcast(returnedMsg, Action.KICK_BROADCAST.getPermission());
		    	    	BAT.getInstance().getRedis().sendBroadcast(Action.KICK_BROADCAST, returnedMsg);
			    	
			} else {
			final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
				checkArgument(player != null, _("playerNotFound"));

				checkArgument(!PermissionManager.isExemptFrom(Action.KICK, pName), _("isExempt"));

				final String returnedMsg = kick.gKick(player, sender.getName(),
					(args.length == 1) ? IModule.NO_REASON : Utils.getFinalArg(args, 1));

				BAT.broadcast(returnedMsg, Action.KICK_BROADCAST.getPermission());
			}
		}
	}
}