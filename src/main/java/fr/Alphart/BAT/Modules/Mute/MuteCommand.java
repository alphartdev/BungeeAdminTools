package fr.Alphart.BAT.Modules.Mute;

import static com.google.common.base.Preconditions.checkArgument;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.BATCommand;
import fr.Alphart.BAT.Modules.BATCommand.RunAsync;
import fr.Alphart.BAT.Modules.CommandHandler;
import fr.Alphart.BAT.Modules.IModule;
import fr.Alphart.BAT.Modules.InvalidModuleException;
import fr.Alphart.BAT.Utils.FormatUtils;
import fr.Alphart.BAT.Utils.Utils;
import fr.Alphart.BAT.database.DataSourceHandler;

public class MuteCommand extends CommandHandler{
	private final static String MUTE_PERM = Mute.MUTE_PERM;

	private static final String ALREADY_MUTE = "&cThis player is already muted from this server!";
	private static final String NOT_MUTE = "&c%entity% isn't muted from this server.";
	private static final String NOT_MUTEIP = "&c%entity% isn't IP muted from this server..";
	private static final String NOT_MUTE_ANY = "&c%entity% isn't muted from any server !";

	private static final String SPECIFY_SERVER = "&cYou must specify a server!";
	private static final String INVALID_SERVER = "&cThe specified server is invalid!";
	private static final String IP_OFFLINE_PLAYER = "&cThe player must be connected to mute his IP!";

	public MuteCommand(final Mute muteModule){
		super(muteModule);
	}

	@RunAsync
	public static class MuteCmd extends BATCommand{
		public MuteCmd() {super("mute", "<player> [server] [reason]", "Mute definitively the player from the specified server", MUTE_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			if(args[0].equals("help")){
				try {
					FormatUtils.showFormattedHelp(BAT.getInstance().getModules().getModule("mute").getCommands(), sender, "MUTE");
				} catch (final InvalidModuleException e) {
					e.printStackTrace();
				}
				return;
			}
			final String pName = args[0];
			String returnedMsg = null;

			// Command pattern : /mute <name>
			if(args.length == 1){
				// If the sender isn't a player, he has to specify a server
				checkArgument(isPlayer(sender), SPECIFY_SERVER);

				final String server = ((ProxiedPlayer)sender).getServer().getInfo().getName();
				// If the player is already mute of this server, the command is gonna be cancelled
				checkArgument(!Mute.isMute(pName, server), ALREADY_MUTE);

				returnedMsg = Mute.mute(pName, server, sender.getName(), 0, IModule.NO_REASON);
			}
			else{
				final String server = args[1];
				// Check if the server is an valid server
				checkArgument(Utils.isServer(server), INVALID_SERVER);
				// Check if the player isn't already mutened from this server
				checkArgument(!Mute.isMute(pName, server), ALREADY_MUTE);

				// Command pattern : /mute <name> <server>
				if(args.length == 2) {
					returnedMsg = Mute.mute(pName, server, sender.getName(), 0, IModule.NO_REASON);
				} else{
					final String reason = Utils.getFinalArg(args, 2);
					returnedMsg = Mute.mute(pName, server, sender.getName(), 0, reason);
				}
			}


			BAT.broadcast(returnedMsg, MUTE_PERM);
		}
	}
	@RunAsync
	public static class MuteIPCmd extends BATCommand{
		public MuteIPCmd() {super("muteip", "<player/ip> [server] [reason]", "Mute definitively the player's IP", MUTE_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {

			final String entity = args[0];
			final boolean isIP = Utils.validIP(entity);
			final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(entity);
			if(!isIP) {
				checkArgument(player != null, IP_OFFLINE_PLAYER);
			}
			String returnedMsg = null;

			// Command pattern : /mute <name>
			if(args.length == 1){
				checkArgument(isPlayer(sender), SPECIFY_SERVER);

				final String server = ((ProxiedPlayer)sender).getServer().getInfo().getName();
				checkArgument(!Mute.isMute(entity, server), ALREADY_MUTE);

				if(isIP) {
					returnedMsg = Mute.mute(entity, server, sender.getName(), 0, IModule.NO_REASON);
				} else {
					returnedMsg = Mute.muteIP(player, server, sender.getName(), 0, IModule.NO_REASON);
				}
			}
			else{
				final String server = args[1];
				checkArgument(Utils.isServer(server), INVALID_SERVER);
				checkArgument(!Mute.isMute(entity, server), ALREADY_MUTE);

				// Command pattern : /mute <name> <server>
				if(args.length == 2){
					if(isIP) {
						returnedMsg = Mute.mute(entity, server, sender.getName(), 0, IModule.NO_REASON);
					} else {
						returnedMsg = Mute.muteIP(player, server, sender.getName(), 0, IModule.NO_REASON);
					}
				}

				// Command pattern : /mute <name> <server> <reason>
				else{
					final String reason = Utils.getFinalArg(args, 2);

					if(isIP) {
						returnedMsg = Mute.mute(entity, server, sender.getName(), 0, reason);
					} else {
						returnedMsg = Mute.muteIP(player, server, sender.getName(), 0, reason);
					}
				}
			}


			BAT.broadcast(returnedMsg, MUTE_PERM);
		}
	}
	@RunAsync
	public static class GMuteCmd extends BATCommand{
		public GMuteCmd() {super("gmute", "<name> [reason]", "Mute definitively the player from the whole network", MUTE_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {

			final String pName = args[0];
			String returnedMsg;

			checkArgument(!Mute.isMute(pName, IModule.GLOBAL_SERVER), ALREADY_MUTE);

			if(args.length == 1){
				returnedMsg = Mute.mute(pName, IModule.GLOBAL_SERVER, sender.getName(), 0, IModule.NO_REASON);
			}
			else{
				final String reason = Utils.getFinalArg(args, 1);
				returnedMsg = Mute.mute(pName, IModule.GLOBAL_SERVER, sender.getName(), 0, reason);
			}

			BAT.broadcast(returnedMsg, MUTE_PERM);
		}
	}
	@RunAsync
	public static class GMuteIPCmd extends BATCommand{
		public GMuteIPCmd() {super("gmuteip", "<player/ip> [reason]", "Mute definitively player's IP from the whole network", MUTE_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {

			final String entity = args[0];
			final boolean isIP = Utils.validIP(entity);
			final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(entity);
			if(!isIP) {
				checkArgument(player != null, IP_OFFLINE_PLAYER);
			}
			String returnedMsg;

			checkArgument(!Mute.isMute(entity, IModule.GLOBAL_SERVER), ALREADY_MUTE);

			if(args.length == 1){
				if(isIP) {
					returnedMsg = Mute.mute(entity, IModule.GLOBAL_SERVER, sender.getName(), 0, IModule.NO_REASON);
				} else {
					returnedMsg = Mute.muteIP(player, IModule.GLOBAL_SERVER, sender.getName(), 0, IModule.NO_REASON);
				}
			}
			else{
				final String reason = Utils.getFinalArg(args, 1);
				if(isIP) {
					returnedMsg = Mute.mute(entity, IModule.GLOBAL_SERVER, sender.getName(), 0, reason);
				} else {
					returnedMsg = Mute.muteIP(player, IModule.GLOBAL_SERVER, sender.getName(), 0, reason);
				}
			}

			BAT.broadcast(returnedMsg, MUTE_PERM);
		}
	}

	@RunAsync
	public static class TempMuteCmd extends BATCommand{
		public TempMuteCmd() {super("tempmute", "<player/ip> <duration> [server] [reason]", "Mute temporarily the player from the specified server", MUTE_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			final String pName = args[0];
			String returnedMsg;
			final int durate = Utils.parseDateDiff(args[1], true) - DataSourceHandler.getTimestamp();

			// Command pattern : /mute <name> <durate>
			if(args.length == 2){
				checkArgument(isPlayer(sender), SPECIFY_SERVER);
				final String server = ((ProxiedPlayer)sender).getServer().getInfo().getName();
				checkArgument(!Mute.isMute(pName, server), ALREADY_MUTE);

				returnedMsg = Mute.mute(pName, server, sender.getName(), durate, IModule.NO_REASON);
			}
			else{
				final String server = args[2];
				checkArgument(Utils.isServer(server), INVALID_SERVER);
				checkArgument(!Mute.isMute(pName, server), ALREADY_MUTE);

				// Command pattern: /mute <name> <durate> <server>
				if(args.length == 3) {
					returnedMsg = Mute.mute(pName, server, sender.getName(), durate, IModule.NO_REASON);
				} else{
					final String reason = Utils.getFinalArg(args, 3);
					returnedMsg = Mute.mute(pName, server, sender.getName(), durate, reason);
				}
			}

			BAT.broadcast(returnedMsg, MUTE_PERM); 
		}
	}
	@RunAsync
	public static class TempMuteIPCmd extends BATCommand{
		public TempMuteIPCmd() {super("tempmuteip", "<player> <duration> [server] [reason]", "Mute temporarily player's IP from the specified server", MUTE_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {

			final String entity = args[0];
			final boolean isIP = Utils.validIP(entity);
			final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(entity);
			if(!isIP) {
				checkArgument(player != null, IP_OFFLINE_PLAYER);
			}
			String returnedMsg;
			final int durate = Utils.parseDateDiff(args[1], true) - DataSourceHandler.getTimestamp();

			// Command pattern : /mute <name> <durate>
			if(args.length == 2){
				checkArgument(isPlayer(sender), SPECIFY_SERVER);
				final String server = ((ProxiedPlayer)sender).getServer().getInfo().getName();
				checkArgument(!Mute.isMute(entity, server), ALREADY_MUTE);

				if(isIP) {
					returnedMsg = Mute.mute(entity, server, sender.getName(), durate, IModule.NO_REASON);
				} else {
					returnedMsg = Mute.muteIP(player, server, sender.getName(), durate, IModule.NO_REASON);
				}
			}
			else{
				final String server = args[2];
				checkArgument(Utils.isServer(server), INVALID_SERVER);
				checkArgument(!Mute.isMute(entity, server), ALREADY_MUTE);

				// Command pattern: /mute <name> <durate> <server>
				if(args.length == 3){
					if(isIP) {
						returnedMsg = Mute.mute(entity, server, sender.getName(), durate, IModule.NO_REASON);
					} else {
						returnedMsg = Mute.muteIP(player, server, sender.getName(), durate, IModule.NO_REASON);
					}
				}

				// Command pattern: /mute <name> <durate> <server> <reason>
				else{
					final String reason = Utils.getFinalArg(args, 3);
					if(isIP) {
						returnedMsg = Mute.mute(entity, server, sender.getName(), durate, reason);
					} else {
						returnedMsg = Mute.muteIP(player, server, sender.getName(), durate, reason);
					}
				}
			}

			BAT.broadcast(returnedMsg, MUTE_PERM); 
		}
	}
	@RunAsync
	public static class GTempMuteCmd extends BATCommand{
		public GTempMuteCmd() {super("gtempmute", "<player> <duration> [reason]", "Mute temporarily the player from the whole network", MUTE_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {

			final String pName = args[0];
			final int durate = Utils.parseDateDiff(args[1], true) - DataSourceHandler.getTimestamp();
			String returnedMsg;

			checkArgument(!Mute.isMute(pName, IModule.GLOBAL_SERVER), ALREADY_MUTE);

			if(args.length == 2){
				returnedMsg = Mute.mute(pName, IModule.GLOBAL_SERVER, sender.getName(), durate, IModule.NO_REASON);
			}
			else{
				final String reason = Utils.getFinalArg(args, 2);
				returnedMsg = Mute.mute(pName, IModule.GLOBAL_SERVER, sender.getName(), durate, reason);
			}

			BAT.broadcast(returnedMsg, MUTE_PERM);
		}
	}
	@RunAsync
	public static class GTempMuteIPCmd extends BATCommand{
		public GTempMuteIPCmd() {super("gtempmuteip", "<player/ip> <duration> [reason]", "Mute temporarily player's IP from the whole network", MUTE_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {

			final String entity = args[0];
			final boolean isIP = Utils.validIP(entity);
			final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(entity);
			if(!isIP) {
				checkArgument(player != null, IP_OFFLINE_PLAYER);
			}
			final int durate = Utils.parseDateDiff(args[1], true) - DataSourceHandler.getTimestamp();
			String returnedMsg;

			checkArgument(!Mute.isMute(entity, IModule.GLOBAL_SERVER), ALREADY_MUTE);

			if(args.length == 2){
				if(isIP) {
					returnedMsg = Mute.mute(entity, IModule.GLOBAL_SERVER, sender.getName(), durate, IModule.NO_REASON);
				} else {
					returnedMsg = Mute.muteIP(player, IModule.GLOBAL_SERVER, sender.getName(), durate, IModule.NO_REASON);
				}
			}
			else{
				final String reason = Utils.getFinalArg(args, 2);
				if(isIP) {
					returnedMsg = Mute.mute(entity, IModule.GLOBAL_SERVER, sender.getName(), durate, reason);
				} else {
					returnedMsg = Mute.muteIP(player, IModule.GLOBAL_SERVER, sender.getName(), durate, reason);
				}
			}

			BAT.broadcast(returnedMsg, MUTE_PERM);
		}
	}

	@RunAsync
	public static class UnmuteCmd extends BATCommand{
		public UnmuteCmd() {super("unmute", "<player> [server] [reason]", "Unmute the player from the specified server", MUTE_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			final String pName = args[0];
			String returnedMsg = null;

			// Command pattern : /mute <name>
			if(args.length == 1){
				checkArgument(isPlayer(sender), SPECIFY_SERVER);
				final String server = ((ProxiedPlayer)sender).getServer().getInfo().getName();
				checkArgument(Mute.isMute(pName, server), NOT_MUTE.replaceAll("%entity%", pName));

				returnedMsg = Mute.unMute(pName, server, sender.getName(), IModule.NO_REASON);
			}
			else{
				final String server = args[1];
				checkArgument(Utils.isServer(server), INVALID_SERVER);
				checkArgument(Mute.isMute(pName, server), NOT_MUTE.replaceAll("%entity%", pName));

				// Command pattern : /mute <name> <server>
				if(args.length == 2) {
					returnedMsg = Mute.unMute(pName, server, sender.getName(), IModule.NO_REASON);
				} else{
					final String reason = Utils.getFinalArg(args, 2);
					returnedMsg = Mute.unMute(pName, server, sender.getName(), reason);
				}
			}


			BAT.broadcast(returnedMsg, MUTE_PERM);
		}	
	}
	@RunAsync
	public static class UnmuteIPCmd extends BATCommand{
		public UnmuteIPCmd() {super("unmuteip", "<player/ip> [server] [reason]", "Unmute IP from the specified server", MUTE_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			final String entity = args[0];
			String returnedMsg = null;

			// Command pattern : /mute <name>
			if(args.length == 1){
				checkArgument(isPlayer(sender), SPECIFY_SERVER);
				final String server = ((ProxiedPlayer)sender).getServer().getInfo().getName();
				checkArgument(Mute.isMute(entity, server), NOT_MUTEIP.replaceAll("%entity%", entity));

				returnedMsg = Mute.unMuteIP(entity, server, sender.getName(), IModule.NO_REASON);
			}
			else{
				final String server = args[1];
				checkArgument(Utils.isServer(server), INVALID_SERVER);
				checkArgument(Mute.isMute(entity, server), NOT_MUTEIP.replaceAll("%entity%", entity));

				// Command pattern : /mute <name> <server>
				if(args.length == 2) {
					returnedMsg = Mute.unMuteIP(entity, server, sender.getName(), IModule.NO_REASON);
				} else{
					final String reason = Utils.getFinalArg(args, 2);
					returnedMsg = Mute.unMuteIP(entity, server, sender.getName(), reason);
				}
			}


			BAT.broadcast(returnedMsg, MUTE_PERM);
		}	
	}
	@RunAsync
	public static class GUnmuteCmd extends BATCommand{
		public GUnmuteCmd() {super("gunmute", "<player> [reason]", "Unmute the player from the whole network", MUTE_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			final String pName = args[0];
			String returnedMsg = null;

			checkArgument(Mute.isMute(pName, IModule.ANY_SERVER), NOT_MUTE.replaceAll("%entity%", pName));

			if(args.length == 1) {
				returnedMsg = Mute.unMute(pName, IModule.ANY_SERVER, sender.getName(), IModule.NO_REASON);
			} else{
				final String reason = Utils.getFinalArg(args, 1);
				returnedMsg = Mute.unMute(pName, IModule.ANY_SERVER, sender.getName(), reason);
			}

			BAT.broadcast(returnedMsg, MUTE_PERM);
		}	
	}
	@RunAsync
	public static class GUnmuteIPCmd extends BATCommand{
		public GUnmuteIPCmd() {super("gunmuteip", "<player/ip> [reason]", "Unmute IP from the whole network", MUTE_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			final String entity = args[0];
			String returnedMsg = null;

			checkArgument(Mute.isMute(entity, IModule.ANY_SERVER), NOT_MUTE_ANY.replaceAll("%entity%", entity));

			if(args.length == 1){
				returnedMsg = Mute.unMuteIP(entity, IModule.ANY_SERVER, sender.getName(), IModule.NO_REASON);
			}
			else{
				final String reason = Utils.getFinalArg(args, 1);
				returnedMsg = Mute.unMuteIP(entity, IModule.ANY_SERVER, sender.getName(), reason);
			}

			BAT.broadcast(returnedMsg, MUTE_PERM);
		}	
	}

}
