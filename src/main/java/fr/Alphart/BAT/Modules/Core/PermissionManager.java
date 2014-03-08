package fr.Alphart.BAT.Modules.Core;

import net.md_5.bungee.api.CommandSender;

public class PermissionManager {
	static String permPrefix = "bat.";
	public static enum Action{
		BAN("ban"),
		BANIP("banip"),
		TEMPBAN("tempban"),
		TEMPBANIP("tempbanip"),
		UNBAN("unban"),
		UNBANIP("unbanip"),
		BAN_BROADCAST("ban.broadcast"),
		
		MUTE("mute"),
		MUTEIP("muteip"),
		TEMPMUTE("tempmute"),
		UNMUTE("unmute"),
		MUTE_BROADCAST("mute.broadcast"),
		
		KICK("kick"),
		KICK_BROADCAST("kick.broadcast");
		
		String permission;
		
		Action(String permission){
			this.permission = permission;
		}
		
		public String getPermission() {
			return permPrefix + permission;
		}
		
		public String toString(){
			return getPermission();
		}
	}
	
	public static boolean canExecuteAction(Action action, CommandSender executor, String server){
		return (executor.hasPermission(permPrefix + ".grantall." + server) 
				|| executor.hasPermission(action.getPermission() + '.' + server) 
				|| executor.hasPermission(action.getPermission() + ".global"));
	}
}