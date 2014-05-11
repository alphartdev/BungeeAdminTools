package fr.Alphart.BAT.Modules.Comment;

import java.util.Arrays;
import java.util.List;

import lombok.Getter;
import net.cubespace.Yamler.Config.Config;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.PluginManager;

public class Trigger extends Config{
	@Getter
	private int triggersNb = 3;
	@Getter
	private String pattern = "";
	private List<String> commands = Arrays.asList("gtempmute {player} 30m");
	
	public void onTrigger(final ProxiedPlayer trigerringPlayer){
		final PluginManager pm = ProxyServer.getInstance().getPluginManager();
		final CommandSender console = ProxyServer.getInstance().getConsole();
		for (final String command : commands) {
			pm.dispatchCommand(console, command.replace("{player}", trigerringPlayer.getName())
					.replace("{server}", trigerringPlayer.getServer().getInfo().getName()));
		}
	}
	
}