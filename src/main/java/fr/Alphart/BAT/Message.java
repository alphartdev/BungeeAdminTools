package fr.Alphart.BAT;

/**
 * Group together all the messages used by several class at the time
 */
public class Message {
	public static final String SPECIFY_SERVER = "You must specify a server!";
	public static final String INVALID_SERVER = "The specified server is invalid!";
	public static final String IP_UNKNOWN_PLAYER = "You can't perfom operation on this user's ip because he never connected.";
	public static final String OPERATION_UNKNOWN_PLAYER = "You're currently perfoming operation on the player %player% who never connected.";

	public static final String BAN_BROADCAST = "&a%entity%&e was &6banned definitively&e by &a%staff%&e from the server &a%serv%&e. Reason : %reason%";
	public static final String BANTEMP_BROADCAST = "&a%entity%&e was &6banned&e &a%duration%&e by &a%staff%&e from the server &a%serv%&e. Reason : %reason%";
	public static final String UNBAN_BROADCAST = "&a%entity%&e was &6unbanned&e by &a%staff%&e from the server &a%serv%&e. Reason : %reason%";
	public static final String WAS_BANNED_NOTIF = "You were banned. Reason : %reason%";
	public static final String IS_BANNED = "You are banned from this server.";
	public static final String ALREADY_BAN = "&cThis player is already banned from this server!";
	public static final String NOT_BAN = "&c%entity% isn't banned from this server.";
	public static final String NOT_BANIP = "&c%entity% isn't banned IP from this server.";
	public static final String NOT_BAN_ANY = "&c%entity% isn't banned from any server !";

	public static final String MUTE_BROADCAST = "&a%entity%&e was &6muted definitively&e by &a%staff%&e from the server &a%serv%&e. Reason : %reason%";
	public static final String MUTETEMP_BROADCAST = "&a%entity%&e was &6muted &eduring &a%duration%&e by &a%staff%&e from the server &a%serv%&e. Reason : %reason%";
	public static final String UNMUTE_BROADCAST = "&a%entity%&e was &6demuted &eby &a%staff%&e from the server &a%serv%&e. Reason : %reason%";
	public static final String WAS_MUTED_NOTIF = "You were muted. Reason : %reason%";
	public static final String WAS_UNMUTED_NOTIF = "You were unmuted. Reason : %reason%";
	public final static String ISMUTE = "You're muted, you can't talk.";
	public final static String LOADING_MUTEDATA = "Loading of data in progress : you may speak in a little while.";
	public static final String ALREADY_MUTE = "&cThis player is already muted from this server!";
	public static final String NOT_MUTE = "&c%entity% isn't muted from this server.";
	public static final String NOT_MUTEIP = "&c%entity% isn't IP muted from this server.";
	public static final String NOT_MUTE_ANY = "&c%entity% isn't muted from any server !";
}
