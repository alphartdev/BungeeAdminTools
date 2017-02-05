package fr.Alphart.BAT.Modules.Core;

import static com.google.common.base.Preconditions.checkArgument;
import static fr.Alphart.BAT.I18n.I18n._;
import static fr.Alphart.BAT.I18n.I18n.__;

import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import lombok.Getter;
import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.I18n.I18n;
import fr.Alphart.BAT.Modules.BATCommand;
import fr.Alphart.BAT.Modules.IModule;
import fr.Alphart.BAT.Modules.InvalidModuleException;
import fr.Alphart.BAT.Modules.ModulesManager;
import fr.Alphart.BAT.Modules.Ban.BanEntry;
import fr.Alphart.BAT.Modules.Comment.CommentEntry;
import fr.Alphart.BAT.Modules.Core.PermissionManager.Action;
import fr.Alphart.BAT.Modules.Core.Importer.BanHammerImporter;
import fr.Alphart.BAT.Modules.Core.Importer.BungeeSuiteImporter;
import fr.Alphart.BAT.Modules.Core.Importer.GeSuiteImporter;
import fr.Alphart.BAT.Modules.Core.Importer.Importer;
import fr.Alphart.BAT.Modules.Core.Importer.Importer.ImportStatus;
import fr.Alphart.BAT.Modules.Core.Importer.MinecraftPreUUIDImporter;
import fr.Alphart.BAT.Modules.Core.Importer.MinecraftUUIDImporter;
import fr.Alphart.BAT.Modules.Core.Importer.SQLiteMigrater;
import fr.Alphart.BAT.Modules.Kick.KickEntry;
import fr.Alphart.BAT.Modules.Mute.MuteEntry;
import fr.Alphart.BAT.Utils.CallbackUtils.Callback;
import fr.Alphart.BAT.Utils.CallbackUtils.ProgressCallback;
import fr.Alphart.BAT.Utils.FormatUtils;
import fr.Alphart.BAT.Utils.Utils;
import fr.Alphart.BAT.database.DataSourceHandler;

public class CoreCommand extends BATCommand{
	private final BaseComponent[] CREDIT;
	private final BaseComponent[] HELP_MSG;
	private final Map<List<String>, BATCommand> subCmd;

	
	public CoreCommand(final Core coreModule) {
		super("bat", "", "", null);
		final Map<String, Boolean> simpleAliasesCommands = BAT.getInstance().getConfiguration().getSimpleAliasesCommands();
		subCmd = new HashMap<List<String>, BATCommand>();
		CREDIT = TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes(
				'&', "&9Bungee&fAdmin&cTools&a Version {version}&e - Developed by &aAlphart")
				.replace("{version}", BAT.getInstance().getDescription().getVersion()));
		
		// Dynamic commands load, commands are not configurable as with other modules
		final List<String> cmdsList = Lists.newArrayList();
		for (final Class<?> subClass : CoreCommand.this.getClass().getDeclaredClasses()) {
			try {
				if(subClass.getAnnotation(BATCommand.Disable.class) != null){
					continue;
				}
				final BATCommand command = (BATCommand) subClass.getConstructors()[0].newInstance();
				cmdsList.add(command.getName());
				final List<String> aliases = new ArrayList<String>(Arrays.asList(command.getAliases()));
				aliases.add(command.getName());
				subCmd.put(aliases, command);
			} catch (final InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | SecurityException e) {
				BAT.getInstance()
				.getLogger()
				.severe("An error occurred during loading of CORE commands please report this :");
				e.printStackTrace();
			}
		}
		
		Collections.sort(cmdsList);
        // Add new commands if there are
        for (final String cmdName : cmdsList) {
            if (!simpleAliasesCommands.containsKey(cmdName)) {
                simpleAliasesCommands.put(cmdName, false);
            }
        }
        // Iterate through the commands map and remove the ones who don't exist (e.g because of an update)
        for(final Iterator<Map.Entry<String, Boolean>> it = simpleAliasesCommands.entrySet().iterator(); it.hasNext();){
            final Map.Entry<String, Boolean> cmdEntry = it.next();
            if(!cmdsList.contains(cmdEntry.getKey())){
                it.remove();
            }
        }
        try {
            BAT.getInstance().getConfiguration().save();
        } catch (InvalidConfigurationException e) {
            BAT.getInstance().getLogger().log(Level.SEVERE, "Error while saving simpleAliasesCmds", e);
        }
        // Register command either as subcommand or as simple alias
        for(final Iterator<Map.Entry<List<String>, BATCommand>> it = subCmd.entrySet().iterator(); it.hasNext();){
            final Map.Entry<List<String>, BATCommand> cmdEntry = it.next();
            if(simpleAliasesCommands.get(cmdEntry.getValue().getName())){
                coreModule.addCommand(cmdEntry.getValue());
                it.remove();
            }
            // Otherwise, do nothing just let the command in the subcommand map
        }
        
        HELP_MSG = TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', 
                "&eType &6" + ((simpleAliasesCommands.get("help")) ? "/help" : "/bat help") + "&e to get help"));
	}

	public List<BATCommand> getSubCmd() {
		return new ArrayList<BATCommand>(subCmd.values());
	}

	// Route the core subcmd
	@Override
	public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd, boolean broadcast)
			throws IllegalArgumentException {
		if (args.length == 0 || subCmd.isEmpty()) {
			sender.sendMessage(CREDIT);
			sender.sendMessage(HELP_MSG);
		} else {
			BATCommand cmd = null;
			for(final Entry<List<String>, BATCommand> aliasesCommand : subCmd.entrySet()){
				if(aliasesCommand.getKey().contains(args[0])){
					cmd = aliasesCommand.getValue();
					break;
				}
			}

			if (cmd != null) {
				// Reorganize args (remove subcommand)
				final String[] cleanArgs = new String[args.length - 1];
				for (int i = 1; i < args.length; i++) {
					cleanArgs[i - 1] = args[i];
				}
				
				if (cmd.getBATPermission().isEmpty() || sender.hasPermission(cmd.getBATPermission()) || sender.hasPermission("bat.admin")) {
					cmd.execute(sender, cleanArgs);
				} else {
					sender.sendMessage(__("noPerm"));
				}
			} else {
				sender.sendMessage(__("invalidCommand"));
			}
		}
	}
	
	public static class HelpCmd extends BATCommand {
		public HelpCmd() {
			super("help", "", "Displays help for core BAT commands.", "bat.help");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd, boolean broadcast)
				throws IllegalArgumentException {
			final List<BATCommand> cmdsList = new ArrayList<BATCommand>();
			for (final BATCommand cmd : BAT.getInstance().getModules().getCore().getCommands()) {
				if (cmd instanceof CoreCommand) {
					cmdsList.addAll(((CoreCommand) cmd).getSubCmd());
				}else{
				    cmdsList.add(cmd);
				}
			}
			FormatUtils.showFormattedHelp(cmdsList, sender, "CORE");
		}
	}

	public static class ModulesCmd extends BATCommand {
		private final StringBuilder sb = new StringBuilder();

		public ModulesCmd() {
			super("modules", "", "Displays what modules are loaded and commands for those modules.", "bat.modules");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd, boolean broadcast)
				throws IllegalArgumentException {
			sender.sendMessage(BAT.__("The loaded modules are :&a"));
			for (final IModule module : BAT.getInstance().getModules().getLoadedModules()) {
				if (module instanceof Core) {
					continue;
				}
				sb.setLength(0);
				sb.append("&f - &9");
				sb.append(module.getName());
				if(module.getMainCommand() == null){
					sb.append(" &f| &eNo main command");
				}else{
					sb.append(" &f| &eMain command : &a/");
					sb.append(module.getMainCommand());
				}
				sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',
						sb.toString())));
			}
			// It means that no module were loaded otherwise there would be
			// something remaining in the StringBuilder
			if (sb.length() == 0) {
				sender.sendMessage(BAT.__("&cThere aren't any loaded modules!"));
			} else {
				sb.setLength(0); // Clean the sb
			}
		}
	}

	public static class ReloadCmd extends BATCommand {
		public ReloadCmd() {
			super("reload", "", "Reload the whole plugin", "bat.reload");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd, boolean broadcast)
				throws IllegalArgumentException {
			sender.sendMessage(BAT.__("Starting reload ..."));
			try {
				BAT.getInstance().getConfiguration().reload();
			} catch (InvalidConfigurationException e) {
				BAT.getInstance().getLogger().severe("Error during reload of main configuration :");
				e.printStackTrace();
			}
			I18n.reload();
			BAT.getInstance().getModules().unloadModules();
			BAT.getInstance().getModules().loadModules();		
			sender.sendMessage(BAT.__("Reload successfully executed ..."));
		}
	}
	
	@RunAsync
	public static class LookupCmd extends BATCommand {
	    @Getter
		private static LookupFormatter lookupFormatter;
		private ModulesManager modules;
		
		public LookupCmd() {
			super("lookup", "<player/ip> [module] [page]", "Displays a player or an ip related information (universal or per module).", Action.LOOKUP.getPermission());
			modules = BAT.getInstance().getModules();
			lookupFormatter = new LookupFormatter();
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd, boolean broadcast)
				throws IllegalArgumentException {
			final String entity = args[0];
			if (Utils.validIP(entity)) {
				checkArgument(sender.hasPermission("bat.admin") || sender.hasPermission(Action.LOOKUP.getPermission() + ".ip"), _("noPerm"));
				if(args.length == 1){
					for (final BaseComponent[] msg : lookupFormatter.getSummaryLookupIP(entity)) {
						sender.sendMessage(msg);
					}
				}
			} else {
				if(args.length == 1){
					for (final BaseComponent[] msg : lookupFormatter.getSummaryLookupPlayer(entity, sender.hasPermission(Action.LOOKUP.getPermission() + ".displayip"))) {
						sender.sendMessage(msg);
					}
				}
			}
			if(args.length > 1){
				int page = 1;
				if(args.length > 2){	
					try{
						page = Integer.parseInt(args[2]);
						if(page <= 0){
							page = 1;
						}
					}catch(final NumberFormatException e){
						throw new IllegalArgumentException("Incorrect page number");
					}
				}
				try{
					final List<BaseComponent[]> message;
					switch(args[1]){
					case "ban":
						final List<BanEntry> bans = modules.getBanModule().getBanData(entity);
						if(!bans.isEmpty()){
							message = lookupFormatter.formatBanLookup(entity, bans, page, false);
						}else{
							message = new ArrayList<BaseComponent[]>();
							message.add(BAT.__((!Utils.validIP(entity))
										? "&eThe player &a" + entity + "&e has never been banned."
										: "&eThe IP &a" + entity + "&e has never been banned."));
						}
						break;
					case "mute":
						final List<MuteEntry> mutes = modules.getMuteModule().getMuteData(entity);
						if(!mutes.isEmpty()){
							message = lookupFormatter.formatMuteLookup(entity, mutes, page, false);
						}else{
							message = new ArrayList<BaseComponent[]>();
							message.add(BAT.__((!Utils.validIP(entity))
										? "&eThe player &a" + entity + "&e has never been muted."
										: "&eThe IP &a" + entity + "&e has never been muted."));
						}
						break;
					case "kick":
						final List<KickEntry> kicks = modules.getKickModule().getKickData(entity);
						if(!kicks.isEmpty()){
							message = lookupFormatter.formatKickLookup(entity, kicks, page, false);
						}else{
							message = new ArrayList<BaseComponent[]>();
							message.add(BAT.__((!Utils.validIP(entity))
										? "&eThe player &a" + entity + "&e has never been kicked."
										: "&eThe IP &a" + entity + "&e has never been kicked."));
						}
						break;
					case "comment":
						final List<CommentEntry> comments = modules.getCommentModule().getComments(entity);
						if(!comments.isEmpty()){
							message = lookupFormatter.commentRowLookup(entity, comments, page, false);
						}else{
							message = new ArrayList<BaseComponent[]>();
							message.add(BAT.__((!Utils.validIP(entity))
										? "&eThe player &a" + entity + "&e has no comment about him."
										: "&eThe IP &a" + entity + "&e has no comment."));
						}
						break;
					default:
						throw new InvalidModuleException("Module not found or invalid");
					}
					
					for (final BaseComponent[] msg : message) {
						sender.sendMessage(msg);
					}			
				}catch(final InvalidModuleException e){
					throw new IllegalArgumentException(e.getMessage());
				}
			}
		}

	}
		
	@RunAsync
	public static class StaffLookupCmd extends BATCommand {
		private final ModulesManager modules;
		
		public StaffLookupCmd() {
			super("stafflookup", "<staff> [module] [page]", "Displays a staff member history (universal or per module).", "bat.stafflookup");
			modules = BAT.getInstance().getModules();
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd, boolean broadcast) throws IllegalArgumentException {
			final String entity = args[0];
			if(args.length == 1){
				for (final BaseComponent[] msg : LookupCmd.getLookupFormatter().getSummaryStaffLookup(entity, 
				        sender.hasPermission(Action.LOOKUP.getPermission() + ".displayip"))) {
					sender.sendMessage(msg);
				}
			}
			if(args.length > 1){
				int page = 1;
				if(args.length > 2){	
					try{
						page = Integer.parseInt(args[2]);
						if(page <= 0){
							page = 1;
						}
					}catch(final NumberFormatException e){
						throw new IllegalArgumentException("Incorrect page number");
					}
				}
				try{
					final List<BaseComponent[]> message;
					switch(args[1]){
					case "ban":
						final List<BanEntry> bans = modules.getBanModule().getManagedBan(entity);
						if(!bans.isEmpty()){
							message = LookupCmd.getLookupFormatter().formatBanLookup(entity, bans, page, true);
						}else{
							message = new ArrayList<BaseComponent[]>();
							message.add(BAT.__("&b" + entity + "&e has never performed any operation concerning ban."));
						}
						break;
					case "mute":
						final List<MuteEntry> mutes = modules.getMuteModule().getManagedMute(entity);
						if(!mutes.isEmpty()){
							message = LookupCmd.getLookupFormatter().formatMuteLookup(entity, mutes, page, true);
						}else{
							message = new ArrayList<BaseComponent[]>();
							message.add(BAT.__("&b" + entity + "&e has never performed any operation concerning mute."));
						}
						break;
					case "kick":
						final List<KickEntry> kicks = modules.getKickModule().getManagedKick(entity);
						if(!kicks.isEmpty()){
							message = LookupCmd.getLookupFormatter().formatKickLookup(entity, kicks, page, true);
						}else{
							message = new ArrayList<BaseComponent[]>();
							message.add(BAT.__("&b" + entity + "&e has never performed any operation concerning kick."));
						}
						break;
					case "comment":
						final List<CommentEntry> comments = modules.getCommentModule().getManagedComments(entity);
						if(!comments.isEmpty()){
							message = LookupCmd.getLookupFormatter().commentRowLookup(entity, comments, page, true);
						}else{
							message = new ArrayList<BaseComponent[]>();
							message.add(BAT.__("&b" + entity + "&e has never performed any operation concerning comment."));
						}
						break;
					default:
						throw new InvalidModuleException("Module not found or invalid");
					}
					
					for (final BaseComponent[] msg : message) {
						sender.sendMessage(msg);
					}			
				}catch(final InvalidModuleException e){
					throw new IllegalArgumentException(e.getMessage());
				}
			}
		}
	}
	
	public static class ConfirmCmd extends BATCommand {
		public ConfirmCmd() {
			super("confirm", "", "Confirm your queued command.", "");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd, boolean broadcast)
				throws IllegalArgumentException {
			if (!CommandQueue.executeQueueCommand(sender)) {
				sender.sendMessage(__("noQueuedCommand"));
			}
		}

	}

	@RunAsync
	public static class ImportCmd extends BATCommand{
	    private final static Map<String, Importer> importers = new HashMap<String, Importer>(){{
            put("bungeeSuiteBans", new BungeeSuiteImporter());
            put("geSuitBans", new GeSuiteImporter());
            put("MC-Previous1.7.8", new MinecraftPreUUIDImporter());
            put("BanHammer", new BanHammerImporter());
            put("BATSQLite", new SQLiteMigrater());
            put("MC-Post1.7.8", new MinecraftUUIDImporter());
	    }};
	    
		public ImportCmd() { 
		    super("import", "<" + Joiner.on('/').join(importers.keySet()) + ">", "Imports ban data from the specified source. Available sources : &a" 
		            + Joiner.on("&e,&a").join(importers.keySet()), "bat.import");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd, boolean broadcast)
				throws IllegalArgumentException {
		    checkArgument(BAT.getInstance().getConfiguration().isMysql_enabled(), "You must use MySQL in order to use the import function.");
			final String source = args[0];
			
			final Importer importer = importers.get(source);
			if(importer != null){
			    sender.sendMessage(BAT.__("BAT will be disabled during the import ..."));
			    BAT.getInstance().getModules().unloadModules();
			    
                importer.startImport(new ProgressCallback<Importer.ImportStatus>() {
                    @Override
                    public void done(ImportStatus result, Throwable throwable) {
                        if(throwable != null){
                            if(throwable instanceof RuntimeException){
                                sender.sendMessage(BAT.__("An error (" + throwable.getMessage()
                                        + ") has occured during the import. Please check the logs"));
                                throwable.printStackTrace();
                            }else{
                                sender.sendMessage(BAT.__("An error has occured during the import. Please check the logs"));
                                BAT.getInstance().getLogger().severe("An error has occured during the import of data from " + source 
                                        + ". Please report this :");
                                throwable.printStackTrace();
                            }
                        }else{
                            sender.sendMessage(BAT.__("Congratulations, the migration is finished. &a" 
                                    + result.getConvertedEntries() + " entries&e were converted successfully."));
                        }
                        BAT.getInstance().getModules().loadModules();
                        sender.sendMessage(BAT.__("BAT is now reenabled ..."));
                    }
                    
                    @Override
                    public void onProgress(ImportStatus progressStatus) {
                        sender.sendMessage(BAT.__("&a" + new DecimalFormat("0.00").format(progressStatus.getProgressionPercent()) 
                                + "%&e entries converted !&a" + (progressStatus.getRemainingEntries()) 
                                + "&e remaining entries on a total of &6" + progressStatus.getTotalEntries()));
                    }
                    
                    @Override
                    public void onMinorError(String errorMessage) {
                        sender.sendMessage(BAT.__(errorMessage));
                    }
                }, Utils.getFinalArg(args, 1));
			}else{
			    throw new IllegalArgumentException("The specified source is incorrect. Available sources : &a" 
	                + Joiner.on("&e,&a").join(importers.keySet()));
			}
		}
	}

	public static class BackupCmd extends BATCommand{
		public BackupCmd() { super("backup", "", "Backup the BAT's data from the mysql database into a file", "bat.backup");}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd, boolean broadcast)
				throws IllegalArgumentException {
			if(DataSourceHandler.isSQLite()){
				throw new IllegalArgumentException("You can't backup an SQLite database with this command. "
						+ "To save an SQLite database just copy and paste the file 'bat_database.db'.");
			}
			sender.sendMessage(BAT.__("Starting backup of BAT datas ..."));
			BAT.getInstance().getDsHandler().generateMysqlBackup(new Callback<String>() {
				@Override
				public void done(final String result, Throwable throwable) {
					sender.sendMessage(BAT.__(result));
				}
			});
		}
	}
	
	@RunAsync
	public static class MigrateCmd extends BATCommand {
		public MigrateCmd() { super("migrateToMysql", "", "Migrate from sqlite to mysql (one-way conversion)", "bat.import");}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd, boolean broadcast) throws IllegalArgumentException {
		    boolean isImportSimpleAlias = BAT.getInstance().getConfiguration().getSimpleAliasesCommands().get("import");
			ProxyServer.getInstance().getPluginManager().dispatchCommand(sender, 
			        ((!isImportSimpleAlias) ? "bat " : "") + "import BATSQLite");
		}

	}
}