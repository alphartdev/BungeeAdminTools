package fr.Alphart.BAT.Modules.Core;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;

import com.google.common.collect.Maps;

public class CommandQueue{
	/**
	 * Store command which are waiting for a confirmation (/bat confirm) ... <br>
	 * Check at the confirm command execution if the command timestamp is expired <br>
	 * Key : player name <br>
	 * Value: Entry: K: expiration timemstamp; V: command
	 */
	private static Map<String, Entry<Long, String>> preExecCommand = Maps.newHashMap();
	
	public static void queueCommand(final CommandSender sender, final String command){
		preExecCommand.put(sender.getName(), new AbstractMap.SimpleEntry<Long, String>(System.currentTimeMillis(), command));
	}
	
	/**
	 * Execute the queued command of the sender if he has one
	 * @param sender
	 * @return true if a command has been executed. False if no command was queued
	 */
	public static boolean executeQueueCommand(final CommandSender sender){
		Entry<Long, String> entry = preExecCommand.get(sender.getName());
		if(entry != null && entry.getKey() > System.currentTimeMillis()){
			preExecCommand.remove(sender.getName());
			ProxyServer.getInstance().getPluginManager().dispatchCommand(sender, entry.getValue());
			return true;
		}
		return false;
	}
	
	public static void clearQueuedCommand(final CommandSender sender){
		preExecCommand.remove(sender.getName());
	}
}