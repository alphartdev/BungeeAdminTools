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

public class BanCommand extends CommandHandler {
	private static final String BAN_PERM = Ban.BAN_PERM;
	private static Ban ban;

	public BanCommand(final Ban banModule){
		super(banModule);
		ban = banModule;
	}

	@RunAsync
	public static class BanCmd extends BATCommand{
		public BanCmd() {super("ban", "<player> [server] [reason]", "Ban definitively the player from the specified server", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			if(args[0].equals("help")){
				try {
					FormatUtils.showFormattedHelp(BAT.getInstance().getModules().getModule("ban").getCommands(), sender, "BAN");
				} catch (final InvalidModuleException e) {
					e.printStackTrace();
				}
				return;
			}
			handleBanCommand(this, false, false, sender, args, confirmedCmd);
		}
	}
	@RunAsync
	public static class BanIPCmd extends BATCommand{
		public BanIPCmd() {super("banip", "<player/ip> [server] [reason]", "Ban definitively player's IP from the specified server", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			handleBanCommand(this, false, true, sender, args, confirmedCmd);
		}
	}
	@RunAsync
	public static class GBanCmd extends BATCommand{
		public GBanCmd() {super("gban", "<player> [reason]", "Ban definitively the player from the whole network", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			handleBanCommand(this, true, false, sender, args, confirmedCmd);
		}
	}
	@RunAsync
	public static class GBanIPCmd extends BATCommand{
		public GBanIPCmd() {super("gbanip", "<player/ip> [reason]", "Ban definitively player's IP from the whole network", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			handleBanCommand(this, true, true, sender, args, confirmedCmd);
		}
	}
	public static void handleBanCommand(final BATCommand command, final boolean global, final boolean ipBan, final CommandSender sender, final String[] args, final boolean confirmedCmd){
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
			if(ipBan){
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

		checkArgument(!ban.isBan(target, server), Message.ALREADY_BAN);	

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
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			handleTempBanCommand(this, false, false, sender, args, confirmedCmd); 
		}
	}
	@RunAsync
	public static class TempBanIPCmd extends BATCommand{
		public TempBanIPCmd() {super("tempbanip", "<player/ip> <duration> [server] [reason]", "Ban temporarily player's IP from the specified server", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			handleTempBanCommand(this, false, true, sender, args, confirmedCmd);
		}
	}
	@RunAsync
	public static class GTempBanCmd extends BATCommand{
		public GTempBanCmd() {super("gtempban", "<player> <duration> [reason]", "Ban temporarily the player from the whole network", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			handleTempBanCommand(this, true, false, sender, args, confirmedCmd);
		}
	}
	@RunAsync
	public static class GTempBanIPCmd extends BATCommand{
		public GTempBanIPCmd() {super("gtempbanip", "<player/ip> <duration> [reason]", "Ban temporarily player's IP from the whole network", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			handleTempBanCommand(this, true, true, sender, args, confirmedCmd);
		}
	}
	public static void handleTempBanCommand(final BATCommand command, final boolean global, final boolean ipBan, final CommandSender sender, final String[] args, final boolean confirmedCmd){
		String target = args[0];
		final long expirationTimestamp = Utils.parseDuration(args[1]);
		String server = IModule.GLOBAL_SERVER;
		final String staff = sender.getName();
		String reason = IModule.NO_REASON;

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
			if(ipBan){
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

		checkArgument(!ban.isBan(target, server), Message.ALREADY_BAN);	

		if(ipBan && player != null){
			returnedMsg = ban.banIP(player, server, staff, expirationTimestamp, reason);
		}else{
			returnedMsg = ban.ban(target, server, staff, expirationTimestamp, reason);
		}

		BAT.broadcast(returnedMsg, BAN_PERM);
	}

	@RunAsync
	public static class PardonCmd extends BATCommand{
		public PardonCmd() {super("pardon", "<player> [server] [reason]", "Unban the player from the specified server", BAN_PERM);}
		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			handlePardonCommand(this, false, false, sender, args, confirmedCmd);
		}	
	}
	@RunAsync
	public static class PardonIPCmd extends BATCommand{
		public PardonIPCmd() {super("pardonip", "<player/ip> [server] [reason]", "Unban IP from the specified server", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			handlePardonCommand(this, false, true, sender, args, confirmedCmd);
		}	
	}
	@RunAsync
	public static class GPardonCmd extends BATCommand{
		public GPardonCmd() {super("gpardon", "<player> [reason]", "Unban the player from the whole network", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			handlePardonCommand(this, true, false, sender, args, confirmedCmd);
		}	
	}
	@RunAsync
	public static class GPardonIPCmd extends BATCommand{
		public GPardonIPCmd() {super("gpardonip", "<player/ip> [reason]", "Unban IP from the whole network", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			handlePardonCommand(this, true, true, sender, args, confirmedCmd);
		}	
	}
	public static void handlePardonCommand(final BATCommand command, final boolean global, final boolean ipUnban, final CommandSender sender, final String[] args, final boolean confirmedCmd){
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
		if(!Utils.validIP(target) && ipUnban)
		{
			final String ip = Core.getPlayerIP(target);
			checkArgument(!"0.0.0.0".equals(ip), Message.IP_UNKNOWN_PLAYER);
			target = ip;
		}

		checkArgument(ban.isBan(target, server), (IModule.ANY_SERVER.equals(server) 
				? Message.NOT_BAN_ANY 
						: ((ipUnban) ? Message.NOT_BANIP : Message.NOT_BAN)
				).replace("%entity%", args[0]));

		if(ipUnban){
			returnedMsg = ban.unBanIP(target, server, staff, reason);
		}else{
			returnedMsg = ban.unBan(target, server, staff, reason);
		}

		BAT.broadcast(returnedMsg, BAN_PERM);
	}
}