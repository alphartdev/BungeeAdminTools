package fr.Alphart.BAT.Modules.Kick;

import static com.google.common.base.Preconditions.checkArgument;
import static fr.Alphart.BAT.I18n.I18n._;

import java.util.UUID;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.BATCommand;
import fr.Alphart.BAT.Modules.BATCommand.RunAsync;
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

	@RunAsync
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
                        
            checkArgument(args.length != 1 || !BAT.getInstance().getConfiguration().isMustGiveReason(),
                _("noReasonInCommand"));
                        
			final String pName = args[0];
	    	final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
	    	// The player is online on the proxy
	    	if(player != null){
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
	    	}else{
	    		if(!BAT.getInstance().getRedis().isRedisEnabled()){
	    			throw new IllegalArgumentException(_("playerNotFound"));
	    		}
	    		// Check if the per server kick with Redis is working fine.
		    	final UUID pUUID = RedisBungee.getApi().getUuidFromName(pName, true);
		    	final String pServer = RedisBungee.getApi().getServerFor(pUUID).getName();
		    	checkArgument(pUUID != null, _("playerNotFound"));
		    	// Check if the server of the target isn't the default one. We assume there is the same default server on both Bungee
		    	// TODO: Add a method to check if it's really on default server
		    	String defaultServer = null;
		    	for(final ListenerInfo listener : ProxyServer.getInstance().getConfig().getListeners()){
		    		defaultServer = listener.getDefaultServer();
		    	}
		    	if(defaultServer == null || pServer.equals(defaultServer)){
		    		throw new IllegalArgumentException(_("cantKickDefaultServer", new String[] { pName }));
		    	}
		    	
                checkArgument(PermissionManager.canExecuteAction(Action.KICK, sender, pServer), _("noPerm"));
		    	
		    	final String returnedMsg;
		    	returnedMsg = kick.kickSQL(pUUID, RedisBungee.getApi().getServerFor(pUUID).getName(), sender.getName(), 
		    		(args.length == 1) ? IModule.NO_REASON : Utils.getFinalArg(args, 1));
	    	    BAT.getInstance().getRedis().sendMoveDefaultServerPlayer(pUUID);
    	    	BAT.broadcast(returnedMsg, Action.KICK_BROADCAST.getPermission());
	    	}
		}
	}

	@RunAsync
	public static class GKickCmd extends BATCommand {
		public GKickCmd() {
			super("gkick", "<player> [reason]", "Kick the player from the network", Action.KICK.getPermission()
					+ ".global");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			final String pName = args[0];
                        
            checkArgument(args.length != 1 || !BAT.getInstance().getConfiguration().isMustGiveReason(),
                    _("noReasonInCommand"));

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