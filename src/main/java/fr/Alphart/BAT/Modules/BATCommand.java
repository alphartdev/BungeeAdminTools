package fr.Alphart.BAT.Modules;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.TabExecutor;
import fr.Alphart.BAT.BAT;

public abstract class BATCommand extends net.md_5.bungee.api.plugin.Command implements TabExecutor{
	private final String name;
	private final String description;
	private final String permission;
	private boolean runAsync = false;

	/**
	 * Constructor
	 * @param name  name of this command
	 * @param description description of this command
	 * @param permission  permission required to use this commands
	 * @param aliases  aliases of this commnad (optionnal)
	 */
	public BATCommand(final String name, final String description, final String permission, final String... aliases){
		super(name, permission, aliases);
		this.name = name;
		this.description = description;
		this.permission = permission;

		final RunAsync asyncAnnot = getClass().getAnnotation(RunAsync.class);
		if(asyncAnnot != null){
			runAsync = true;
		}
	}

	@Override
	public String getName(){return name;}

	public String getDescription(){return description;}

	public String getPermissions(){return permission;}

	/**
	 * Get command usage
	 * @return command's usage
	 */
	public String getUsage(){
		final String usage = name + " " + description;
		return usage;
	}

	/**
	 * Get a nice coloured usage
	 * @return coloured usage
	 */
	public String getFormatUsage(){
		return ChatColor.translateAlternateColorCodes('&', getUsage().replaceAll("(\\w*\\s)(.*)", "$1&6$2").replaceAll(" - ", "&f : &B"));
	}

	@Override
	public Iterable<String> onTabComplete(final CommandSender sender, final String[] args) {
		final List<String> result = new ArrayList<String>();
		if(args.length == 0){
			sender.sendMessage( BAT.__("Add the first letter to autocomplete") );
			return result;
		}
		final String playerToCheck = args[args.length - 1];
		if ( playerToCheck.length() > 0 ){
			for ( final ProxiedPlayer player : ProxyServer.getInstance().getPlayers() ) {
				if ( player.getName().substring( 0, (playerToCheck.length() < player.getName().length() ) ? playerToCheck.length() : player.getName().length() ).equalsIgnoreCase( playerToCheck ) ){
					result.add( player.getName() );
				}
			}
		}
		return result;
	}


	@Override
	public void execute(final CommandSender sender, final String[] args) {
		if(runAsync){
			ProxyServer.getInstance().getScheduler().runAsync(BAT.getInstance(), new Runnable(){
				@Override
				public void run() {
					try{
						onCommand(sender, args);
					}catch(final IllegalArgumentException exception){
						if(exception.getMessage() == null){
							sender.sendMessage(BAT.__("&cArguments invalides. &BUsage : "));
							sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', "&e/") + getFormatUsage() ));
						} else {
							sender.sendMessage(BAT.__("&cArguments invalides. &6" + exception.getMessage()));
						}
					}
				}			
			});
		}
		else{
			try{
				onCommand(sender, args);
			}catch(final IllegalArgumentException exception){
				if(exception.getMessage() == null){
					sender.sendMessage(BAT.__("&cArguments invalides. &BUsage : "));
					sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', "&e/") + getFormatUsage() ));
				} else {
					sender.sendMessage(BAT.__("&cArguments invalides. &6" + exception.getMessage()));
				}
			}	
		}
	}

	public abstract void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException;

	/**
	 * Use this annotation onCommand if the command need to be runned async
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface RunAsync{} 

	/* Utils for command */
	/**
	 * Check if the sender is a player <br>
	 * Use for readibility
	 * @param sender
	 * @return true if the sender is a player otherwise false
	 */
	public boolean isPlayer(final CommandSender sender){
		if(sender instanceof ProxiedPlayer) {
			return true;
		}
		return false;
	}

	public static void invalidArgs(final CommandSender sender, final String message){
		if(message == null) {
			sender.sendMessage(BAT.__("&cArguments invalides. "));
		} else{
			sender.sendMessage(BAT.__("&cArguments invalides."));
			sender.sendMessage( TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', "&6Utilisation: &B" + message)) );
		}
	}

}