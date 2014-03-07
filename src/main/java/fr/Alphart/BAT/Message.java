package fr.Alphart.BAT;

/**
 * Group together all the messages used by several class at the time
 */
public class Message {
	public static final String SPECIFY_SERVER = "&cYou must specify a server!";
	public static final String INVALID_SERVER = "&cThe specified server is invalid!";
	public static final String IP_UNKNOWN_PLAYER = "&cYou can't perfom operation on this user's ip because he never connected.";
	public static final String OPERATION_UNKNOWN_PLAYER = "&ceyou're currently perfoming operation on the player %player% who never connected.";
	
	public static final String BAN_BROADCAST = "&a%entity%&e was &6banned definitively&e by &a%staff%&e from the server &a%serv%&e. Reason : %reason%";
	public static final String BANTEMP_BROADCAST = "&a%entity%&e was &6banned&e &a%duration%&e by &a%staff%&e from the server &a%serv%&e. Reason : %reason%";
	public static final String UNBAN_BROADCAST = "&a%entity%&e was &6unbanned&e by &a%staff%&e from the server &a%serv%&e. Reason : %reason%";
	public static final String WAS_BANNED_MSG = "You were banned. Reason : %reason%";
	public static final String IS_BANNED = "You are banned from this server.";
	public static final String ALREADY_BAN = "&cThis player is already banned from this server!";
	public static final String NOT_BAN = "&c%entity% isn't banned from this server.";
	public static final String NOT_BANIP = "&c%entity% isn't banned IP from this server.";
	public static final String NOT_BAN_ANY = "&c%entity% isn't banned from any server !";

}
