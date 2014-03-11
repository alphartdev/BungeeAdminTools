package fr.Alphart.BAT.Modules.Core;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class PermissionManager {
	static String permPrefix = "bat.";

	public static enum Action {
		BAN("ban"), BANIP("banip"), TEMPBAN("tempban"), TEMPBANIP("tempbanip"), UNBAN("unban"), UNBANIP("unbanip"), 
		BAN_BROADCAST("ban.broadcast"),

		MUTE("mute"), MUTEIP("muteip"), TEMPMUTE("tempmute"), TEMPMUTEIP("tempmuteip"), UNMUTE("unmute"), 
		UNMUTEIP("unmuteip"), MUTE_BROADCAST("mute.broadcast"),

		KICK("kick"), KICK_BROADCAST("kick.broadcast");

		String permission;

		Action(String permission) {
			this.permission = permission;
		}

		public String getPermission() {
			return permPrefix + permission;
		}

		public String toString() {
			return getPermission();
		}
	}

	/**
	 * Check if the command sender can exec this action on this server
	 * 
	 * @param action
	 * @param executor
	 * @param server
	 * @return true if he can otherwise false
	 */
	public static boolean canExecuteAction(Action action, CommandSender executor, String server) {
		return (executor.hasPermission(action.getPermission() + ".global")
				|| executor.hasPermission(permPrefix + ".grantall." + server)
				|| executor.hasPermission(action.getPermission() + '.' + server));
	}

	/**
	 * Check if this entity is an online player (if it's a player) and then
	 * return if it is exempt of the specified action
	 * 
	 * @param action
	 * @param target
	 * @return true if it is exempt from this action otherwise false
	 */
	public static boolean isExemptFrom(Action action, String target) {
		final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(target);
		if (player != null) {
			return player.hasPermission(action.getPermission() + ".exempt");
		}
		return false;
	}
}