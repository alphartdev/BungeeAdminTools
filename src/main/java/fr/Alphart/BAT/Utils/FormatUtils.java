package fr.Alphart.BAT.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import net.md_5.bungee.api.chat.TextComponent;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ObjectArrays;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.I18n.I18n;
import fr.Alphart.BAT.Modules.BATCommand;

public class FormatUtils {
	private static StringBuilder sb = new StringBuilder();
	private static Pattern hoverParsingPattern = Pattern.compile("(?i)(.*?)\\{effect=\"hover\"\\s*?text=\"(.*?)\"\\s*?onHoverText=\"(.*?)\"\\}(.*)");

	/**
	 * Get the duration between the given timestamp and the current one
	 * 
	 * @param timestamp
	 *            in milliseconds which must be superior to the current
	 *            timestamp
	 * @return readable duration
	 */
	public static String getDuration(final long futureTimestamp) {
		int seconds = (int) ((futureTimestamp - System.currentTimeMillis()) / 1000) + 1;
		Preconditions.checkArgument(seconds > 0,
				"The timestamp passed in parameter must be superior to the current timestamp !");

		final List<String> item = new ArrayList<String>();

		int months = 0;
		while (seconds >= 2678400) {
			months++;
			seconds -= 2678400;
		}
		if (months > 0) {
			item.add(months + I18n._("months"));
		}

		int days = 0;
		while (seconds >= 86400) {
			days++;
			seconds -= 86400;
		}
		if (days > 0) {
			item.add(days + I18n._("days"));
		}

		int hours = 0;
		while (seconds >= 3600) {
			hours++;
			seconds -= 3600;
		}
		if (hours > 0) {
			item.add(hours + I18n._("hours"));
		}

		int mins = 0;
		while (seconds >= 60) {
			mins++;
			seconds -= 60;
		}
		if (mins > 0) {
			item.add(mins + I18n._("minutes"));
		}

		if (seconds > 0) {
			item.add(seconds + I18n._("seconds"));
		}

		return Joiner.on(", ").join(item);
	}

	public static void showFormattedHelp(final List<BATCommand> cmds, final CommandSender sender, final String helpName) {
		final List<BaseComponent[]> msg = new ArrayList<BaseComponent[]>();
		sb.append("&9 ---- &9Bungee&fAdmin&cTools&9 - &6");
		sb.append(helpName);
		sb.append("&9 - &fHELP &9---- ");
		msg.add(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', sb.toString())));
		sb.setLength(0);
		boolean coreHelp = "core".equalsIgnoreCase(helpName);
		final Map<String, Boolean> simpleAliasesCommands = BAT.getInstance().getConfiguration().getSimpleAliasesCommands();
		for (final BATCommand cmd : cmds) {
			if (sender.hasPermission("bat.admin") || sender.hasPermission(cmd.getBATPermission())) {
				if(coreHelp){
					sb.append((simpleAliasesCommands.get(cmd.getName())) ? " &f- &e/" : " &f- &e/bat ");
				}else{
					sb.append(" &f- &e/");	
				}
				sb.append(cmd.getFormatUsage());
				msg.add(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', sb.toString())));
				sb.setLength(0);
			}
		}
		for (final BaseComponent[] tx : msg) {
			sender.sendMessage(tx);
		}
		if (msg.size() == 1) {
			sender.sendMessage(BAT.__("&c No command corresponding to your permission has been found"));
		}
	}

	public static List<BaseComponent[]> formatNewLine(final String message) {
	    //BEWARE: Horrible parsing code below
		final String[] strMessageArray = message.split("\n");
		final List<BaseComponent[]> bsList = new ArrayList<BaseComponent[]>();
		for (String line : strMessageArray) {
		    BaseComponent[] lineComponent = null;
  		    if(line.contains("{effect=\"hover\"")){
  		      // A line may contain mutliple efft hover so we need to handle that
  		      for (String subLine : line.split("(?=\\{effect\\=\"hover\")")){//Trick to split without removing the split char
    		      try{
    		          Matcher matcher = hoverParsingPattern.matcher(subLine);
    		          if(!matcher.find()){
                        if(lineComponent==null){
                          lineComponent = TextComponent.fromLegacyText(subLine);
                        }else{
                          lineComponent = ObjectArrays.concat(lineComponent, TextComponent.fromLegacyText(subLine), BaseComponent.class);
                        }
    		            continue;
    		          }
    	              String previousText=matcher.group(1);
    	              String text=matcher.group(2);
    	              String onHoverText=matcher.group(3);
    	              String followingText=matcher.group(4);
    	              
    	              BaseComponent hoverMessage = componentsArrayToComponent(TextComponent.fromLegacyText(text));
    	              hoverMessage.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, TextComponent.fromLegacyText(onHoverText.replace("{newlinehover}", "\n"))));
    	              
    	              if(lineComponent==null){
    	                lineComponent = TextComponent.fromLegacyText(previousText);
    	              }else{
    	                lineComponent = ObjectArrays.concat(lineComponent, TextComponent.fromLegacyText(previousText), BaseComponent.class);
    	              }
    	              lineComponent=ObjectArrays.concat(lineComponent, new BaseComponent[]{hoverMessage}, BaseComponent.class);
    	              lineComponent=ObjectArrays.concat(lineComponent, TextComponent.fromLegacyText(followingText), BaseComponent.class);
    		      }catch(Exception e){
    		        BAT.getInstance().getLogger().severe("An error occured while parsing hover text. Line: " + line);
    		        e.printStackTrace();
    		        bsList.add(TextComponent.fromLegacyText("Parsing error please check console"));
    		      }
  		      }
		    }else{
		      lineComponent=TextComponent.fromLegacyText(line);
		    }
		  
			bsList.add(lineComponent);
		}
		return bsList;
	}
	
	private static BaseComponent componentsArrayToComponent(BaseComponent[] componentsArray){
	  BaseComponent component=componentsArray[0];
      for(int j =1; j < componentsArray.length; j++){
        component.addExtra(componentsArray[j]);
      }
      return component;
	}
}