package fr.Alphart.BAT.Modules.Kick;

import static com.google.common.base.Preconditions.checkArgument;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Message;
import fr.Alphart.BAT.Modules.BATCommand;
import fr.Alphart.BAT.Modules.CommandHandler;
import fr.Alphart.BAT.Modules.InvalidModuleException;
import fr.Alphart.BAT.Utils.FormatUtils;
import fr.Alphart.BAT.Utils.Utils;

public class KickCommand extends CommandHandler {
	private final static String KICK_PERM = Kick.KICK_PERM;
	private static Kick kick;


	public KickCommand(final Kick kickModule){
		super(kickModule);
		kick = kickModule;
	}

	public static class KickCmd extends BATCommand{
		public KickCmd() {super("kick", "<player> [reason]", "Kick the player from his current server to the lobby", KICK_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			if(args[0].equals("help")){
				try {
					FormatUtils.showFormattedHelp(BAT.getInstance().getModules().getModule("kick").getCommands(), sender, "KICK");
				} catch (final InvalidModuleException e) {
					e.printStackTrace();
				}
				return;
			}
			final String pName = args[0];
			final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
			checkArgument(player != null, Message.OFFLINE_PLAYER);
			String returnedMsg;

			// Command pattern : /kick <name>
			if(args.length == 1){
				returnedMsg = kick.kick(player, sender.getName(), null);
			}
			// Command pattern : /kick <name> <raison>
			else{
				final String reason = Utils.getFinalArg(args, 1);
				returnedMsg = kick.kick(player, sender.getName(), reason);
			}

			BAT.broadcast(returnedMsg, KICK_PERM);
		}
	}

	public static class GKickCmd extends BATCommand{
		public GKickCmd() {super("gkick", "<player> [reason]", "Kick the player from the proxy", KICK_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			final String pName = args[0];
			final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
			checkArgument(player != null, Message.OFFLINE_PLAYER);
			String returnedMsg;

			// Command pattern : /kick <name>
			if(args.length == 1){
				returnedMsg = kick.gKick(player, sender.getName(), null);
			}
			// Command pattern : /kick <name> <raison>
			else{
				final String reason = Utils.getFinalArg(args, 1);
				returnedMsg = kick.gKick(player, sender.getName(), reason);
			}

			BAT.broadcast(returnedMsg, KICK_PERM);
		}
	}

}
