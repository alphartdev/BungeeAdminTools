package fr.Alphart.BAT.Utils;

import java.util.ArrayList;
import java.util.List;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

import com.google.common.base.Joiner;

import fr.Alphart.BAT.Modules.BATCommand;
import fr.Alphart.BAT.Modules.IModule;

public class FormatUtils {
	private static StringBuilder sb = new StringBuilder();

	/**
	 * Replace %p%,%staff%,%serv%,%reason%,%duration% by the specified value
	 * @param message
	 * @param pName
	 * @param staff
	 * @param server
	 * @param reason
	 * @return message filled with data
	 */
	public static String formatBroadcastMsg(final String message, final String pName, final String staff, final String server, final String reason, final String duration){
		String formattedMsg = message.replaceAll("%entity%", pName).replaceAll("%staff%", staff).replaceAll("%duration%", duration);
		if(server.equals(IModule.GLOBAL_SERVER) || server.equals(IModule.ANY_SERVER)){
			formattedMsg = formattedMsg.replaceAll("%serv%", "global");
		}else{
			formattedMsg = formattedMsg.replaceAll("%serv%", server);
		}
		formattedMsg = (reason != null) ? formattedMsg.replaceAll("%reason%", reason) : formattedMsg.replaceAll("%reason%", "Non specifie");
		return formattedMsg;
	}

	/**
	 * Convert seconds into readable duration (day, hours, minutes)
	 * @param seconds
	 * @return readable duration
	 */
	public static String secToDate(int seconds)
	{
		final List<String> item = new ArrayList<String>();
		int days = 0;
		while(seconds >= 86400){
			days++;
			seconds -= 86400;
		}
		if(days > 0) {
			item.add(days + " jours");
		}

		int hours = 0;
		while(seconds >= 3600){
			hours++;
			seconds -= 3600;
		}
		if(hours > 0) {
			item.add(hours + " heures");
		}

		int mins = 0;
		while(seconds >= 60){
			mins++;
			seconds -= 60;
		}
		if(mins > 0) {
			item.add(mins + " minutes");
		}

		if(seconds > 0) {
			item.add(seconds + " secondes");
		}

		return Joiner.on(", ").join(item);
	}

	public static void showFormattedHelp(final List<BATCommand> cmds, final CommandSender sender, final String helpName){
		final List<BaseComponent[]> msg = new ArrayList<BaseComponent[]>();
		sb.append("&9 ---- &9Bungee&fAdmin&cTools&9 - &6");
		sb.append(helpName);
		sb.append("&9 - &fAIDE &9---- ");
		msg.add(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',  sb.toString())));
		sb.setLength(0);
		for(final BATCommand cmd: cmds){
			if(sender.hasPermission(cmd.getPermission())){
				sb.append(" &f- &e/");
				if(helpName.equals("CORE")) {
					sb.append(cmd.getUsage().replaceFirst("(\\w*\\s\\w*)", "$1&6").replaceAll("-", "&f:&B"));
				} else {
					sb.append(cmd.getFormatUsage());
				}
				msg.add(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',  sb.toString())));
				sb.setLength(0);
			}
		}
		for(final BaseComponent[] tx : msg) {
			sender.sendMessage(tx);
		}
		if(msg.size() == 1) {
			sender.sendMessage( TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',  "&c Aucune commande correspondant a vos permissions n'a ete trouvee")) );
		}
	}

	public static List<BaseComponent[]> formatNewLine(final String message){
		final String[] strMessageArray = message.split("\n");
		final List<BaseComponent[]> bsList = new ArrayList<BaseComponent[]>();
		for(int i =0; i<strMessageArray.length; i++){
			bsList.add( TextComponent.fromLegacyText(strMessageArray[i]) );
		}
		return bsList;
	}
}
