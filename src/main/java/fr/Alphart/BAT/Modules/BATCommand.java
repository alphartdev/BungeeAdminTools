package fr.Alphart.BAT.Modules;

import static fr.Alphart.BAT.I18n.I18n._;
import static fr.Alphart.BAT.I18n.I18n.__;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.MissingResourceException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Setter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.TabExecutor;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.imaginarycode.minecraft.redisbungee.RedisBungee;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.Core.CommandQueue;
import fr.Alphart.BAT.Modules.Core.Core;
import fr.Alphart.BAT.Modules.Core.CoreCommand;
import fr.Alphart.BAT.Utils.UUIDNotFoundException;

public abstract class BATCommand extends net.md_5.bungee.api.plugin.Command implements TabExecutor {
	private static final Pattern pattern = Pattern.compile("<.*?>");
	private final String name;
	private final String syntax;
	private final String description;
	private final String permission;
	private boolean runAsync = false;
	private boolean coreCommand = false;

	@Setter
	private int minArgs = 0;

	/**
	 * Constructor
	 * 
	 * @param name
	 *            name of this command
	 * @param description
	 *            description of this command
	 * @param permission
	 *            permission required to use this commands
	 * @param aliases
	 *            aliases of this commnad (optionnal)
	 */
	public BATCommand(final String name, final String syntax, final String description, final String permission,
			final String... aliases) {
		super(name, null, aliases); // Use own permission system
		this.name = name;
		this.syntax = syntax;
		this.permission = permission;
		this.description = description;

		
		// Compute min args
		final Matcher matcher = pattern.matcher(syntax);
		while (matcher.find()) {
			minArgs++;
		}

		final RunAsync asyncAnnot = getClass().getAnnotation(RunAsync.class);
		if (asyncAnnot != null) {
			runAsync = true;
		}
		
		if(CoreCommand.class.equals(getClass().getEnclosingClass())){
			coreCommand = true;
		}
	}

	public String getDescription() {
		return description;
	}

	public String getUsage() {
		final String usage = Joiner.on(' ').join(name, syntax, description);
		return usage;
	}

	public String getSyntax(){
		return syntax;
	}
	
	/**
	 * Get a nice coloured usage
	 * 
	 * @return coloured usage
	 */
	public String getFormatUsage() {
		return ChatColor.translateAlternateColorCodes('&', "&e" + name + " &6" + syntax + " &f-&B " + description);
	}

	public String getBATPermission(){
		return permission;
	}
	
	public void handleCommandException(final CommandSender sender, final Exception exception){
		if(exception instanceof IllegalArgumentException){
			if (exception.getMessage() == null) {
				if(coreCommand){
					// Just need to add the /bat if it's a core command
					sender.sendMessage(__("invalidArgsUsage", new String[] { "&e/bat " + getFormatUsage() }));
				}else{
					sender.sendMessage(__("invalidArgsUsage", new String[] { "&e/" + getFormatUsage() }));
				}
			} else if (_("noPerm").equals(exception.getMessage())) {
				sender.sendMessage(__("noPerm"));
			} else {
				sender.sendMessage(__("invalidArgs", new String[] { exception.getMessage() }));
			}
		}
		else if(exception instanceof UUIDNotFoundException){
			sender.sendMessage(__("invalidArgs", new String[] { _("cannotGetUUID", new String[] { ((UUIDNotFoundException)exception).getInvolvedPlayer() }) }));
		}
		else if(exception instanceof MissingResourceException){
			sender.sendMessage(BAT.__("&cAn error occured with the translation. Key involved : &a" + ((MissingResourceException)exception).getKey()));
		}else{
			sender.sendMessage(BAT.__("A command errror happens ! Please check the console."));
			BAT.getInstance().getLogger().severe("A command errror happens ! Please report this stacktrace :");
			exception.printStackTrace();
		}
	}
	
	@Override
	public void execute(final CommandSender sender, final String[] args) {
		// If the sender doesn't have the permission, we're gonna check if he has this permission with children permission
		// Example : in this plugin, if the sender has "bat.ban.server1", he also has "bat.ban"
		if(!(permission == null || sender.hasPermission(permission) || sender.hasPermission("bat.admin")
				|| (sender.hasPermission("bat.grantall.global") && permission.endsWith("global")))){
			boolean hasPerm = false;
			Collection<String> senderPerm = Core.getCommandSenderPermission(sender);
			for(final String perm : senderPerm){
				// The grantall give acces to all command (used when command is executed, but the plugin check in the command if the sender can execute this action)
				// except the /bat ... commands
				if(perm.toLowerCase().startsWith(permission)){
					hasPerm = true;
					break;
				}
				// The global grantall perm has already been checked before
				if (!coreCommand && perm.toLowerCase().startsWith("bat.grantall") && !permission.endsWith("global")) {
					// We're going to check if there is no perm to cancel (using -)
					final String searchedPattern = "-" + permission;
					boolean permFound = false;
					for(final String perm2 : senderPerm){
						if(perm2.toLowerCase().startsWith(searchedPattern)){
							permFound = true;
							break;
						}
					}
					if(permFound){
						hasPerm = false;
					}
					else{
						hasPerm = true;
						break;
					}
				}
			}
			if(!hasPerm){
				sender.sendMessage(__("noPerm"));
				return;
			}
		}
		// Overrides command to confirm if /bat confirm is disabled
		final boolean confirmedCmd = (BAT.getInstance().getConfiguration().isConfirmCommand()) ? CommandQueue.isExecutingQueueCommand(sender) : true;
		try {
			Preconditions.checkArgument(args.length >= minArgs);
			if (runAsync) {
				ProxyServer.getInstance().getScheduler().runAsync(BAT.getInstance(), new Runnable() {
					@Override
					public void run() {
						try {
							onCommand(sender, args, confirmedCmd);
						} catch (final Exception exception) {
							handleCommandException(sender, exception);
						} 
					}
				});
			} else {
				onCommand(sender, args, confirmedCmd);
			}
		} catch (final Exception exception) {
			handleCommandException(sender, exception);
		}
		if (confirmedCmd) {
			CommandQueue.removeFromExecutingQueueCommand(sender);
		}
	}

	@Override
	public Iterable<String> onTabComplete(final CommandSender sender, final String[] args) {
		final List<String> result = new ArrayList<String>();
		if (args.length == 0) {
			sender.sendMessage(BAT.__("Add the first letter to autocomplete"));
			return result;
		}
		final String playerToCheck = args[args.length - 1];
		if (playerToCheck.length() > 0) {
		    	if (BAT.getInstance().getRedis().isRedisEnabled()) {
		    	    	for (final String player : RedisBungee.getApi().getHumanPlayersOnline()) {
		    	    	    	if (player
		    	    	    		.substring(
		    	    	    				0,
		    	    	    				(playerToCheck.length() < player.length()) ? playerToCheck.length() : player
		    	    	    					.length()).equalsIgnoreCase(playerToCheck)) {
		    	    	    		result.add(player);
		    	    		}
		    	    	}
		    	} else {
		    	    	for (final ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
					if (player
							.getName()
							.substring(
									0,
									(playerToCheck.length() < player.getName().length()) ? playerToCheck.length() : player
										.getName().length()).equalsIgnoreCase(playerToCheck)) {
						result.add(player.getName());
					}
				}
		    	}
		}
		return result;
	}

	public abstract void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
			throws IllegalArgumentException;

	/**
	 * Use this annotation onCommand if the command need to be runned async
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface RunAsync {
	}

	/**
	 * Use this annotation to disable a command
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface Disable {
	}
	
	/* Utils for command */
	/**
	 * Check if the sender is a player <br>
	 * Use for readibility
	 * 
	 * @param sender
	 * @return true if the sender is a player otherwise false
	 */
	public boolean isPlayer(final CommandSender sender) {
		if (sender instanceof ProxiedPlayer) {
			return true;
		}
		return false;
	}

	public void mustConfirmCommand(final CommandSender sender, final String command, final String message) {
		final String cmdToConfirm = (BAT.getInstance().getConfiguration().getSimpleAliasesCommands().get("confirm"))
		        ? "confirm" : "bat confirm";
		if (!CommandQueue.isExecutingQueueCommand(sender)) {
			if ("".equals(message)) {
				sender.sendMessage(__("mustConfirm", new String[] { "", cmdToConfirm }));
			} else {
				sender.sendMessage(__("mustConfirm", new String[] { "&e"+message, cmdToConfirm }));
			}
			CommandQueue.queueCommand(sender, command);
		}
	}
}