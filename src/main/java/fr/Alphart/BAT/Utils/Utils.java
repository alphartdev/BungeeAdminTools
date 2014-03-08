package fr.Alphart.BAT.Utils;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class Utils {
	private static StringBuilder sb = new StringBuilder();
	private static Pattern ipPattern = Pattern.compile("(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)");
	private final static Pattern timePattern = Pattern.compile("(?:([0-9]+)\\s*y[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*mo[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*w[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*d[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*h[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*m[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*(?:s[a-z]*)?)?", Pattern.CASE_INSENSITIVE);

	/**
	 * Get the timestamp corresponding to the current date + this duration
	 * @param durationStr
	 * @return timestamp in millis
	 * @throws IllegalArgumentException
	 */
	public static long parseDuration(final String durationStr) throws IllegalArgumentException
	{
		final Matcher m = timePattern.matcher(durationStr);
		int years = 0;
		int months = 0;
		int weeks = 0;
		int days = 0;
		int hours = 0;
		int minutes = 0;
		int seconds = 0;
		boolean found = false;
		while (m.find())
		{
			if (m.group() == null || m.group().isEmpty())
			{
				continue;
			}
			for (int i = 0; i < m.groupCount(); i++)
			{
				if (m.group(i) != null && !m.group(i).isEmpty())
				{
					found = true;
					break;
				}
			}
			if (found)
			{
				if (m.group(1) != null && !m.group(1).isEmpty())
				{
					years = Integer.parseInt(m.group(1));
				}
				if (m.group(2) != null && !m.group(2).isEmpty())
				{
					months = Integer.parseInt(m.group(2));
				}
				if (m.group(3) != null && !m.group(3).isEmpty())
				{
					weeks = Integer.parseInt(m.group(3));
				}
				if (m.group(4) != null && !m.group(4).isEmpty())
				{
					days = Integer.parseInt(m.group(4));
				}
				if (m.group(5) != null && !m.group(5).isEmpty())
				{
					hours = Integer.parseInt(m.group(5));
				}
				if (m.group(6) != null && !m.group(6).isEmpty())
				{
					minutes = Integer.parseInt(m.group(6));
				}
				if (m.group(7) != null && !m.group(7).isEmpty())
				{
					seconds = Integer.parseInt(m.group(7));
				}
				break;
			}
		}
		if (!found)
		{
			throw new IllegalArgumentException(ChatColor.RED + "Invalid duration !");
		}
		final Calendar c = new GregorianCalendar();
		if (years > 0)
		{
			c.add(Calendar.YEAR, years);
		}
		if (months > 0)
		{
			c.add(Calendar.MONTH, months);
		}
		if (weeks > 0)
		{
			c.add(Calendar.WEEK_OF_YEAR, weeks);
		}
		if (days > 0)
		{
			c.add(Calendar.DAY_OF_MONTH, days);
		}
		if (hours > 0)
		{
			c.add(Calendar.HOUR_OF_DAY, hours);
		}
		if (minutes > 0)
		{
			c.add(Calendar.MINUTE, minutes);
		}
		if (seconds > 0)
		{
			c.add(Calendar.SECOND, seconds);
		}
		return c.getTimeInMillis();
	}

	/**
	 * Get the final args from start
	 * @param args
	 * @param start
	 * @return finalArg from start
	 */
	public static String getFinalArg(final String[] args, final int start)
	{
		for (int i = start; i < args.length; i++)
		{
			if (i != start){
				sb.append(" ");
			}
			sb.append(args[i]);
		}
		final String msg = sb.toString();
		sb.setLength(0);
		return msg;
	}

	/**
	 * Check if a server with his name exist
	 * @return
	 */
	public static boolean isServer(final String serverName){
		for (final ServerInfo si : ProxyServer.getInstance().getServers().values()) {
			if (si.getName().equalsIgnoreCase(serverName)) {
				return true;
			}
		}
		return false;
	}

	public static String getPlayerIP(final ProxiedPlayer player){
		return player.getAddress().getAddress().getHostAddress();
	}

	public static boolean validIP(final String ip){
		return ipPattern.matcher(ip).matches();
	}
}