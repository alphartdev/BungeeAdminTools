package fr.Alphart.BAT.Modules.Mute;

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

public class MuteCommand extends CommandHandler{
	private final static String MUTE_PERM = Mute.MUTE_PERM;
	private static Mute mute;

	public MuteCommand(final Mute muteModule){
		super(muteModule);
		mute = muteModule;
	}

	@RunAsync
	public static class MuteCmd extends BATCommand{
		public MuteCmd() {super("mute", "<player> [server] [reason]", "Mute definitively the player from the specified server", MUTE_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			if(args[0].equals("help")){
				try {
					FormatUtils.showFormattedHelp(BAT.getInstance().getModules().getModule("mute").getCommands(), sender, "MUTE");
				} catch (final InvalidModuleException e) {
					e.printStackTrace();
				}
				return;
			}
			handleMuteCommand(this, false, false, sender, args, confirmedCmd);
		}
	}
	@RunAsync
	public static class MuteIPCmd extends BATCommand{
		public MuteIPCmd() {super("muteip", "<player/ip> [server] [reason]", "Mute definitively the player's IP", MUTE_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			handleMuteCommand(this, false, true, sender, args, confirmedCmd);
		}
	}
	@RunAsync
	public static class GMuteCmd extends BATCommand{
		public GMuteCmd() {super("gmute", "<name> [reason]", "Mute definitively the player from the whole network", MUTE_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			handleMuteCommand(this, true, false, sender, args, confirmedCmd);
		}
	}
	@RunAsync
	public static class GMuteIPCmd extends BATCommand{
		public GMuteIPCmd() {super("gmuteip", "<player/ip> [reason]", "Mute definitively player's IP from the whole network", MUTE_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			handleMuteCommand(this, true, true, sender, args, confirmedCmd);
		}
	}
	public static void handleMuteCommand(final BATCommand command, final boolean global, final boolean ipMute, final CommandSender sender, final String[] args, final boolean confirmedCmd){
		String target = args[0];
		String server = IModule.GLOBAL_SERVER;
		final String staff = sender.getName();
		String reason = IModule.NO_REASON;

		final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(target);

		String returnedMsg;

		if(global){
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
				checkArgument(Utils.isServer(args[1]), Message.INVALID_SERVER);
				server = args[1];
				reason = (args.length > 2) ? Utils.getFinalArg(args, 2) : IModule.NO_REASON;
			}
		}

		// Check if the target isn't an ip and the player is offline
		if(!Utils.validIP(target) && player == null)
		{
			final String ip = Core.getPlayerIP(target);
			if(ipMute){
				checkArgument(!"0.0.0.0".equals(ip), Message.IP_UNKNOWN_PLAYER);
				target = ip;
			}			
			// If ip = 0.0.0.0, it means the player never connects
			else if("0.0.0.0".equals(ip) && !confirmedCmd)
			{
				command.mustConfirmCommand(sender, command.getName() + " " + Joiner.on(' ').join(args), Message.OPERATION_UNKNOWN_PLAYER.replace("%player%", target));
				return;
			}
		}

		checkArgument(!mute.isMute(target, server, false), Message.ALREADY_MUTE);	

		if(ipMute && player != null){
			returnedMsg = mute.muteIP(player, server, staff, 0, reason);
		}else{
			returnedMsg = mute.mute(target, server, staff, 0, reason);
		}

		BAT.broadcast(returnedMsg, MUTE_PERM);
	}

	@RunAsync
	public static class TempMuteCmd extends BATCommand{
		public TempMuteCmd() {super("tempmute", "<player/ip> <duration> [server] [reason]", "Mute temporarily the player from the specified server", MUTE_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			handleTempMuteCommand(this, false, false, sender, args, confirmedCmd);
		}
	}
	@RunAsync
	public static class TempMuteIPCmd extends BATCommand{
		public TempMuteIPCmd() {super("tempmuteip", "<player> <duration> [server] [reason]", "Mute temporarily player's IP from the specified server", MUTE_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			handleTempMuteCommand(this, false, true, sender, args, confirmedCmd);
		}
	}
	@RunAsync
	public static class GTempMuteCmd extends BATCommand{
		public GTempMuteCmd() {super("gtempmute", "<player> <duration> [reason]", "Mute temporarily the player from the whole network", MUTE_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			handleTempMuteCommand(this, true, false, sender, args, confirmedCmd);
		}
	}
	@RunAsync
	public static class GTempMuteIPCmd extends BATCommand{
		public GTempMuteIPCmd() {super("gtempmuteip", "<player/ip> <duration> [reason]", "Mute temporarily player's IP from the whole network", MUTE_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			handleTempMuteCommand(this, true, true, sender, args, confirmedCmd);
		}
	}
	public static void handleTempMuteCommand(final BATCommand command, final boolean global, final boolean ipMute, final CommandSender sender, final String[] args, final boolean confirmedCmd){
		String target = args[0];
		String server = IModule.GLOBAL_SERVER;
		final String staff = sender.getName();
		String reason = IModule.NO_REASON;
		final long expirationTimestamp = Utils.parseDuration(args[1]);

		final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(target);

		String returnedMsg;

		if(global){
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
			if(ipMute){
				checkArgument(!"0.0.0.0".equals(ip), Message.IP_UNKNOWN_PLAYER);
				target = ip;
			}			
			// If ip = 0.0.0.0, it means the player never connects
			else if("0.0.0.0".equals(ip) && !confirmedCmd)
			{
				command.mustConfirmCommand(sender, command.getName() + " " + Joiner.on(' ').join(args), Message.OPERATION_UNKNOWN_PLAYER.replace("%player%", target));
				return;
			}
		}

		checkArgument(!mute.isMute(target, server, false), Message.ALREADY_MUTE);	

		if(ipMute && player != null){
			returnedMsg = mute.muteIP(player, server, staff, expirationTimestamp, reason);
		}else{
			returnedMsg = mute.mute(target, server, staff, expirationTimestamp, reason);
		}

		BAT.broadcast(returnedMsg, MUTE_PERM);
	}

	@RunAsync
	public static class UnmuteCmd extends BATCommand{
		public UnmuteCmd() {super("unmute", "<player> [server] [reason]", "Unmute the player from the specified server", MUTE_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			handleUnmuteCommand(this, false, false, sender, args, confirmedCmd);
		}	
	}
	@RunAsync
	public static class UnmuteIPCmd extends BATCommand{
		public UnmuteIPCmd() {super("unmuteip", "<player/ip> [server] [reason]", "Unmute IP from the specified server", MUTE_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			handleUnmuteCommand(this, false, true, sender, args, confirmedCmd);
		}	
	}
	@RunAsync
	public static class GUnmuteCmd extends BATCommand{
		public GUnmuteCmd() {super("gunmute", "<player> [reason]", "Unmute the player from the whole network", MUTE_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			handleUnmuteCommand(this, true, false, sender, args, confirmedCmd);
		}	
	}
	@RunAsync
	public static class GUnmuteIPCmd extends BATCommand{
		public GUnmuteIPCmd() {super("gunmuteip", "<player/ip> [reason]", "Unmute IP from the whole network", MUTE_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			handleUnmuteCommand(this, true, true, sender, args, confirmedCmd);
		}	
	}
	public static void handleUnmuteCommand(final BATCommand command, final boolean global, final boolean ipUnmute, final CommandSender sender, final String[] args, final boolean confirmedCmd){
		String target = args[0];
		String server = IModule.ANY_SERVER;
		final String staff = sender.getName();
		String reason = IModule.NO_REASON;

		String returnedMsg;

		if(global){
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
				checkArgument(Utils.isServer(args[1]), Message.INVALID_SERVER);
				server = args[1];
				reason = (args.length > 2) ? Utils.getFinalArg(args, 2) : IModule.NO_REASON;
			}
		}

		// Check if the target isn't an ip and the player is offline
		if(!Utils.validIP(target) && ipUnmute)
		{
			final String ip = Core.getPlayerIP(target);
			checkArgument(!"0.0.0.0".equals(ip), Message.IP_UNKNOWN_PLAYER);
			target = ip;
		}

		checkArgument(mute.isMute(target, server, true), (IModule.ANY_SERVER.equals(server) 
				? Message.NOT_MUTE_ANY 
						: ((ipUnmute) ? Message.NOT_MUTEIP : Message.NOT_MUTE)
				).replace("%entity%", args[0]));

		if(ipUnmute){
			returnedMsg = mute.unMuteIP(target, server, staff, reason);
		}else{
			returnedMsg = mute.unMute(target, server, staff, reason);
		}

		BAT.broadcast(returnedMsg, MUTE_PERM);
	}
}