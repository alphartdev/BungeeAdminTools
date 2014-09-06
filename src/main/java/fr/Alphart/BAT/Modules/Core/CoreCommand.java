package fr.Alphart.BAT.Modules.Core;

import static com.google.common.base.Preconditions.checkArgument;
import static fr.Alphart.BAT.I18n.I18n._;
import static fr.Alphart.BAT.I18n.I18n.__;

import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.UUID;

import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.imaginarycode.minecraft.redisbungee.RedisBungee;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.I18n.I18n;
import fr.Alphart.BAT.Modules.BATCommand;
import fr.Alphart.BAT.Modules.IModule;
import fr.Alphart.BAT.Modules.InvalidModuleException;
import fr.Alphart.BAT.Modules.ModulesManager;
import fr.Alphart.BAT.Modules.Ban.BanEntry;
import fr.Alphart.BAT.Modules.Comment.CommentEntry;
import fr.Alphart.BAT.Modules.Comment.CommentEntry.Type;
import fr.Alphart.BAT.Modules.Core.Importer.BungeeSuiteImporter;
import fr.Alphart.BAT.Modules.Core.Importer.GeSuiteImporter;
import fr.Alphart.BAT.Modules.Core.Importer.ImportStatus;
import fr.Alphart.BAT.Modules.Core.Importer.MinecraftPreUUIDImporter;
import fr.Alphart.BAT.Modules.Core.PermissionManager.Action;
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
	private final boolean simpleAliases;

	
	public CoreCommand(final boolean simpleAliases) {
		super("bat", "", "", null);
		this.simpleAliases = simpleAliases;
		subCmd = new HashMap<List<String>, BATCommand>();
		CREDIT = TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes(
				'&', "&9Bungee&fAdmin&cTools&a Version {version}&e - Developped by &aAlphart")
				.replace("{version}", BAT.getInstance().getDescription().getVersion()));
		HELP_MSG = TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', 
				"&eType &6" + ((simpleAliases) ? "/help" : "/bat help") + "&e to get help"));
		
		// Dynamic commands load, commands are not configurable as with other modules
		for (final Class<?> subClass : CoreCommand.this.getClass().getDeclaredClasses()) {
			try {
				if(subClass.getAnnotation(BATCommand.Disable.class) != null){
					continue;
				}
				final BATCommand command = (BATCommand) subClass.getConstructors()[0].newInstance();
				final List<String> aliases = new ArrayList<String>(Arrays.asList(command.getAliases()));
				aliases.add(command.getName());
				subCmd.put(aliases, command);
			} catch (final InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | SecurityException e) {
				BAT.getInstance()
				.getLogger()
				.severe("An error happend during loading of CORE commands please report this :");
				e.printStackTrace();
			}
		}
		

	}

	public List<BATCommand> getSubCmd() {
		return new ArrayList<BATCommand>(subCmd.values());
	}

	// Route the core subcmd
	@Override
	public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
			throws IllegalArgumentException {
		if (args.length == 0 || simpleAliases) {
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
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			final List<BATCommand> cmdsList = new ArrayList<BATCommand>();
			for (final BATCommand cmd : BAT.getInstance().getModules().getCore().getCommands()) {
				if (cmd instanceof CoreCommand) {
					cmdsList.addAll(((CoreCommand) cmd).getSubCmd());
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
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
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
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
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
		private final ModulesManager modules;
		private final Calendar localTime = Calendar.getInstance(TimeZone.getDefault());
		private static final int entriesPerPage = 15;
		private final String lookupHeader = "\n&f---- &9Lookup &f- &b{entity} &f-&a {module} &f-&6 Page {page} &f----\n";
		private final String lookupFooter = "\n&f---- &9Lookup &f- &b{entity} &f-&a {module} &f-&6 Page {page} &f----";
		
		public LookupCmd() {
			super("lookup", "<player/ip> [module] [page]", "Displays a player or an ip related information (universal or per module).", Action.LOOKUP.getPermission());
			modules = BAT.getInstance().getModules();
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			final String entity = args[0];
			if (Utils.validIP(entity)) {
				checkArgument(sender.hasPermission(Action.LOOKUP.getPermission() + ".ip"), _("noPerm"));
				if(args.length == 1){
					for (final BaseComponent[] msg : getSummaryLookupIP(entity)) {
						sender.sendMessage(msg);
					}
				}
			} else {
				if(args.length == 1){
					for (final BaseComponent[] msg : getSummaryLookupPlayer(entity, sender.hasPermission(Action.LOOKUP.getPermission() + ".displayip"))) {
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
							message = formatBanLookup(entity, bans, page, lookupHeader, lookupFooter, false);
						}else{
							message = new ArrayList<BaseComponent[]>();
							message.add(BAT.__((!Utils.validIP(entity))
										? "&eThe player &a" + entity + "&e wasn't ever banned."
										: "&eThe IP &a" + entity + "&e wasn't ever banned."));
						}
						break;
					case "mute":
						final List<MuteEntry> mutes = modules.getMuteModule().getMuteData(entity);
						if(!mutes.isEmpty()){
							message = formatMuteLookup(entity, mutes, page, lookupHeader, lookupFooter, false);
						}else{
							message = new ArrayList<BaseComponent[]>();
							message.add(BAT.__((!Utils.validIP(entity))
										? "&eThe player &a" + entity + "&e wasn't ever mute."
										: "&eThe IP &a" + entity + "&e wasn't ever mute."));
						}
						break;
					case "kick":
						final List<KickEntry> kicks = modules.getKickModule().getKickData(entity);
						if(!kicks.isEmpty()){
							message = formatKickLookup(entity, kicks, page, lookupHeader, lookupFooter, false);
						}else{
							message = new ArrayList<BaseComponent[]>();
							message.add(BAT.__((!Utils.validIP(entity))
										? "&eThe player &a" + entity + "&e wasn't ever kicked."
										: "&eThe IP &a" + entity + "&e wasn't ever kicked."));
						}
						break;
					case "comment":
						final List<CommentEntry> comments = modules.getCommentModule().getComments(entity);
						if(!comments.isEmpty()){
							message = commentRowLookup(entity, comments, page, lookupHeader, lookupFooter, false);
						}else{
							message = new ArrayList<BaseComponent[]>();
							message.add(BAT.__((!Utils.validIP(entity))
										? "&eThe player &a" + entity + "&e has no comment on him."
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

		public List<BaseComponent[]> getSummaryLookupIP(final String ip) {
			final StringBuilder msg = new StringBuilder();
			msg.append(lookupHeader.replace("{entity}", ip).replace("{module}", "Summary").replace("{page}", "1/1"));

			final EntityEntry ipDetails = new EntityEntry(ip);

			if (!ipDetails.exist()) {
				final List<BaseComponent[]> returnedMsg = new ArrayList<BaseComponent[]>();
				returnedMsg.add(__("&eThe IP &a" + ip + "&e doesn't have any recording."));
				return returnedMsg;
			}

			boolean isBan = false;
			int bansNumber = 0;
			final List<String> banServers = new ArrayList<String>();
			boolean isMute = false;
			int mutesNumber = 0;
			final List<String> muteServers = new ArrayList<String>();

			if (!ipDetails.getBans().isEmpty()) {
				for (final BanEntry banEntry : ipDetails.getBans()) {
					if (banEntry.isActive()) {
						isBan = true;
						banServers.add(banEntry.getServer());
					}
				}
				bansNumber = ipDetails.getBans().size();
			}
			if (!ipDetails.getMutes().isEmpty()) {
				for (final MuteEntry muteEntry : ipDetails.getMutes()) {
					if (muteEntry.isActive()) {
						isMute = true;
						muteServers.add(muteEntry.getServer());
					}
				}
				mutesNumber = ipDetails.getMutes().size();
			}

			msg.append("&eThis IP is used by the following players : \n&3 ");
			msg.append(Joiner.on("&f, &3").join(ipDetails.getUsers()));

			if (isBan || isMute) {
				msg.append("\n&eState : ");
				if (isBan) {
					msg.append("\n&c&lBanned &efrom &3");
					msg.append(Joiner.on("&f, &3").join(banServers).toLowerCase());
				}
				if (isMute) {
					msg.append("\n&c&lMute &efrom &3");
					msg.append(Joiner.on("&f, &3").join(muteServers).toLowerCase());
				}
			}

			if (bansNumber > 0 || mutesNumber > 0) {
				msg.append("\n&eHistory : ");
				if (bansNumber > 0) {
					msg.append("&B&l");
					msg.append(bansNumber);
					msg.append((bansNumber > 1) ? "&e bans" : "&e ban");
				}
				if (mutesNumber > 0) {
					msg.append("\n&B&l            ");
					msg.append(mutesNumber);
					msg.append((mutesNumber > 1) ? "&e mutes" : "&e mute");
				}
			} else {
				msg.append("\n&eNo sanctions ever imposed.");
			}

			msg.append(lookupFooter.replace("{entity}", ip).replace("{module}", "Summary").replace("{page}", "1/1"));

			return FormatUtils.formatNewLine(ChatColor.translateAlternateColorCodes('&', msg.toString()));
		}
		public List<BaseComponent[]> getSummaryLookupPlayer(final String pName, final boolean displayID) {
			final StringBuilder msg = new StringBuilder();
			msg.append(lookupHeader.replace("{entity}", pName).replace("{module}", "Summary").replace("{page}", "1/1"));

			// Get players data related to each modules
			final EntityEntry pDetails = new EntityEntry(pName);

			if (!pDetails.exist()) {
				final List<BaseComponent[]> returnedMsg = new ArrayList<BaseComponent[]>();
				returnedMsg.add(__("playerNotFound"));
				return returnedMsg;
			}

			final EntityEntry ipDetails = new EntityEntry(Core.getPlayerIP(pName));

			boolean isBan = false;
			boolean isBanIP = false;
			int bansNumber = 0;
			final List<String> banServers = Lists.newArrayList();
			final List<String> banIPServers = Lists.newArrayList();
			boolean isMute = false;
			boolean isMuteIP = false;
			int mutesNumber = 0;
			final List<String> muteServers = Lists.newArrayList();
			final List<String> muteIPServers = Lists.newArrayList();
			int kicksNumber = 0;

			for (final BanEntry banEntry : pDetails.getBans()) {
				if (banEntry.isActive()) {
					isBan = true;
					banServers.add(banEntry.getServer());
				}
			}
			for (final BanEntry banEntry : ipDetails.getBans()) {
				if (banEntry.isActive()) {
					isBanIP = true;
					banIPServers.add(banEntry.getServer());
				}
			}
			for (final MuteEntry muteEntry : pDetails.getMutes()) {
				if (muteEntry.isActive()) {
					isMute = true;
					muteServers.add(muteEntry.getServer());
				}
			}
			for (final MuteEntry muteEntry : ipDetails.getMutes()) {
				if (muteEntry.isActive()) {
					isMuteIP = true;
					muteIPServers.add(muteEntry.getServer());
				}
			}
			bansNumber = pDetails.getBans().size() + ipDetails.getBans().size();
			mutesNumber = pDetails.getMutes().size() + ipDetails.getMutes().size();
			kicksNumber = pDetails.getKicks().size();

			// Construction of the message
			if (BAT.getInstance().getRedis().isRedisEnabled()) {
			    	UUID pUUID = RedisBungee.getApi().getUuidFromName(pName, true);
			    	msg.append((pUUID != null && RedisBungee.getApi().isPlayerOnline(pUUID)) ? "&a&lConnected &r&eon the &3"
					+ RedisBungee.getApi().getServerFor(pUUID).getName() + " &eserver" : "&8&lOffline");
			} else {
			    	msg.append((ProxyServer.getInstance().getPlayer(pName) != null) ? "&a&lConnected &r&eon the &3"
					+ ProxyServer.getInstance().getPlayer(pName).getServer().getInfo().getName() + " &eserver" : "&8&lOffline");
			}
			
			if (isBan || isMute || isBanIP || isMuteIP) {
				msg.append("\n&eState : ");
				if (isBan) {
					msg.append("\n&c&lBanned &efrom &3");
					msg.append(Joiner.on("&f, &3").join(banServers).toLowerCase());
				}
				if (isBanIP) {
					msg.append("\n&c&lBanned IP &efrom &3");
					msg.append(Joiner.on("&f, &3").join(banIPServers).toLowerCase());
				}
				if (isMute) {
					msg.append("\n&c&lMute &efrom &3");
					msg.append(Joiner.on("&f, &3").join(muteServers).toLowerCase());
				}
				if (isMuteIP) {
					msg.append("\n&c&lMute IP &efrom &3");
					msg.append(Joiner.on("&f, &3").join(muteIPServers).toLowerCase());
				}
			}

			
			msg.append("\n&eFirst login : &a");
			if(pDetails.getFirstLogin() != EntityEntry.noDateFound){
				localTime.setTimeInMillis(pDetails.getFirstLogin().getTime());
				msg.append(Core.defaultDF.format(localTime.getTime()));
			}else{
				msg.append("&cNever connected");
			}

			msg.append("\n&eLast login : &a");
			if(pDetails.getLastLogin() != EntityEntry.noDateFound){
				localTime.setTimeInMillis(pDetails.getLastLogin().getTime());
				msg.append(Core.defaultDF.format(localTime.getTime()));
			}else{
				msg.append("&cNever connected");
			}

			msg.append("\n&eLast IP : &a");
			if("0.0.0.0".equals(pDetails.getLastIP())){
				msg.append("&cNever connected");
			}else{
				msg.append((displayID) ? pDetails.getLastIP() : "&7Hidden");
			}

			if (bansNumber > 0 || mutesNumber > 0 || kicksNumber > 0 || pDetails.getComments().size() > 0) {
				msg.append("\n&eHistory : ");
				if (bansNumber > 0) {
					msg.append("&B&l");
					msg.append(bansNumber);
					msg.append((bansNumber > 1) ? "&e bans" : "&e ban");
				}
				if (mutesNumber > 0) {
					msg.append("\n&B&l            ");
					msg.append(mutesNumber);
					msg.append((mutesNumber > 1) ? "&e mutes" : "&e mute");
				}
				if (kicksNumber > 0) {
					msg.append("\n&B&l            ");
					msg.append(kicksNumber);
					msg.append((kicksNumber > 1) ? "&e kicks" : "&e kick");
				}
				if(pDetails.getComments().size() > 0){
					msg.append("\n&aLast three comments: ");
					int commentsDisplayed = 0;
					for(final CommentEntry comm : pDetails.getComments()){
						msg.append("\n");
						msg.append(_("commentRow", new String[]{String.valueOf(comm.getID()), 
								(comm.getType() == Type.NOTE) ? "&eComment" : "&cWarning", comm.getContent(),
								comm.getFormattedDate(), comm.getAuthor()}));
						commentsDisplayed++;
						if(commentsDisplayed == 3){
							break;
						}
					}
				}
			} else {
				msg.append("\n&eNo sanctions ever imposed.");
			}

			msg.append(lookupFooter.replace("{entity}", pName).replace("{module}", "Summary").replace("{page}", "1/1"));

			return FormatUtils.formatNewLine(ChatColor.translateAlternateColorCodes('&', msg.toString()));
		}

		public static List<BaseComponent[]> formatBanLookup(final String entity, final List<BanEntry> bans,
				int page, final String header, final String footer, final boolean staffLookup) throws InvalidModuleException {
			final StringBuilder msg = new StringBuilder();

			int totalPages = (int) Math.ceil((double)bans.size()/entriesPerPage);
			if(bans.size() > entriesPerPage){
				if(page > totalPages){
					page = totalPages;
				}
				int beginIndex = (page - 1) * entriesPerPage;
				int endIndex = (beginIndex + entriesPerPage < bans.size()) ? beginIndex + entriesPerPage : bans.size();
				for(int i=bans.size() -1; i > 0; i--){
					if(i >= beginIndex && i < endIndex){
						continue;
					}
					bans.remove(i);
				}
			}
			msg.append(header.replace("{entity}", entity).replace("{module}", "Ban")
					.replace("{page}", page + "/" + totalPages));
			
			boolean isBan = false;
			for (final BanEntry banEntry : bans) {
				if (banEntry.isActive()) {
					isBan = true;
				}
			}

			// We begin with active ban
			if(isBan){
				msg.append("&6&lActive bans: &e");
				final Iterator<BanEntry> it = bans.iterator();
				while(it.hasNext()){
					final BanEntry ban = it.next();
					if(!ban.isActive()){
						break;
					}
					final String begin = Core.defaultDF.format(ban.getBeginDate());
					final String server = ban.getServer();
					final String reason = ban.getReason();	
					final String end;
					if(ban.getEndDate() == null){
						end = "permanent ban";
					}else{
						end = Core.defaultDF.format(ban.getEndDate());
					}
					
					msg.append("\n");
					if(staffLookup){
						msg.append(_("activeStaffBanLookupRow", 
								new String[] { ban.getEntity(), begin, server, reason, end}));
					}else{
						msg.append(_("activeBanLookupRow", 
								new String[] { begin, server, reason, ban.getStaff(), end}));
					}
					it.remove();
				}
			}
			
			if(!bans.isEmpty()){
				msg.append("\n&7&lArchive bans: &e");
				for(final BanEntry ban : bans){
					final String begin = Core.defaultDF.format(ban.getBeginDate());
					final String server = ban.getServer();
					final String reason = ban.getReason();
					
					final String endDate;
					if(ban.getEndDate() == null){
						endDate = Core.defaultDF.format(ban.getUnbanDate());
					}else{
						endDate = Core.defaultDF.format(ban.getEndDate());
					}
					final String unbanReason = ban.getUnbanReason();
					String unbanStaff = ban.getUnbanStaff();
					if(unbanStaff == null){
						unbanStaff = "temporary ban";
					}
					
					msg.append("\n");
					if(staffLookup){
						msg.append(_("archiveStaffBanLookupRow", 
								new String[] { ban.getEntity(), begin, server, reason, endDate, unbanReason, unbanStaff}));
					}else{
						msg.append(_((staffLookup) ? "archiveStaffBanLookupRow" : "archiveBanLookupRow", 
								new String[] { begin, server, reason, ban.getStaff(), endDate, unbanReason, unbanStaff}));
					}
					
				}
			}

			msg.append(footer.replace("{entity}", entity).replace("{module}", "Ban")
					.replace("{page}", page + "/" + totalPages));

			return FormatUtils.formatNewLine(ChatColor.translateAlternateColorCodes('&', msg.toString()));
		}
		public static List<BaseComponent[]> formatMuteLookup(final String entity, final List<MuteEntry> mutes,
				int page, final String header, final String footer, final boolean staffLookup) throws InvalidModuleException {
			final StringBuilder msg = new StringBuilder();

			int totalPages = (int) Math.ceil((double)mutes.size()/entriesPerPage);
			if(mutes.size() > entriesPerPage){
				if(page > totalPages){
					page = totalPages;
				}
				int beginIndex = (page - 1) * entriesPerPage;
				int endIndex = (beginIndex + entriesPerPage < mutes.size()) ? beginIndex + entriesPerPage : mutes.size();
				for(int i=mutes.size() -1; i > 0; i--){
					if(i >= beginIndex && i < endIndex){
						continue;
					}
					mutes.remove(i);
				}
			}
			msg.append(header.replace("{entity}", entity).replace("{module}", "Mute")
					.replace("{page}", page + "/" + totalPages));
			
			boolean isMute = false;
			for (final MuteEntry muteEntry : mutes) {
				if (muteEntry.isActive()) {
					isMute = true;
				}
			}

			// We begin with active ban
			if(isMute){
				msg.append("&6&lActive mutes: &e");
				final Iterator<MuteEntry> it = mutes.iterator();
				while(it.hasNext()){
					final MuteEntry mute = it.next();
					if(!mute.isActive()){
						break;
					}
					final String begin = Core.defaultDF.format(mute.getBeginDate());
					final String server = mute.getServer();
					final String reason = mute.getReason();
					final String end;
					if(mute.getEndDate() == null){
						end = "permanent mute";
					}else{
						end = Core.defaultDF.format(mute.getEndDate());
					}
					
					msg.append("\n");
					if(staffLookup){
						msg.append(_("activeStaffMuteLookupRow", 
								new String[] { mute.getEntity(), begin, server, reason, end}));
					}else{
						msg.append(_("activeMuteLookupRow", 
								new String[] { begin, server, reason, mute.getStaff(), end}));
					}
					it.remove();
				}
			}
			
			if(!mutes.isEmpty()){
				msg.append("\n&7&lArchive mutes: &e");
				for(final MuteEntry mute : mutes){
					final String begin = Core.defaultDF.format(mute.getBeginDate());
					final String server = mute.getServer();
					final String reason = mute.getReason();
					
					final String unmuteDate;
					if(mute.getUnmuteDate() == null){
						unmuteDate = Core.defaultDF.format(mute.getEndDate());
					}else{
						unmuteDate = Core.defaultDF.format(mute.getUnmuteDate());
					}
					final String unmuteReason = mute.getUnmuteReason();
					String unmuteStaff = mute.getUnmuteStaff();
					if(unmuteStaff == "null"){
						unmuteStaff = "temporary mute";
					}
					
					msg.append("\n");
					if(staffLookup){
						msg.append(_("archiveStaffMuteLookupRow", 
								new String[] { mute.getEntity(), begin, server, reason, unmuteDate, unmuteReason, unmuteStaff}));
					}else{
						msg.append(_("archiveMuteLookupRow", 
								new String[] { begin, server, reason, mute.getStaff(), unmuteDate, unmuteReason, unmuteStaff}));
					}
				}
			}

			msg.append(footer.replace("{entity}", entity).replace("{module}", "Mute")
					.replace("{page}", page + "/" + totalPages));

			return FormatUtils.formatNewLine(ChatColor.translateAlternateColorCodes('&', msg.toString()));
		}
		public static List<BaseComponent[]> formatKickLookup(final String entity, final List<KickEntry> kicks,
				int page, final String header, final String footer, final boolean staffLookup) throws InvalidModuleException {
			final StringBuilder msg = new StringBuilder();

			int totalPages = (int) Math.ceil((double)kicks.size()/entriesPerPage);
			if(kicks.size() > entriesPerPage){
				if(page > totalPages){
					page = totalPages;
				}
				int beginIndex = (page - 1) * entriesPerPage;
				int endIndex = (beginIndex + entriesPerPage < kicks.size()) ? beginIndex + entriesPerPage : kicks.size();
				for(int i=kicks.size() -1; i > 0; i--){
					if(i >= beginIndex && i < endIndex){
						continue;
					}
					kicks.remove(i);
				}
			}
			msg.append(header.replace("{entity}", entity).replace("{module}", "Kick")
					.replace("{page}", page + "/" + totalPages));
			
			msg.append("&6&lKick list :");
			
			for(final KickEntry kick : kicks){
				final String date = Core.defaultDF.format(kick.getDate());
				final String server = kick.getServer();
				final String reason = kick.getReason();
				
				msg.append("\n");
				if(staffLookup){
					msg.append(_("kickStaffLookupRow", 
							new String[] { kick.getEntity(), date, server, reason}));
				}else{
					msg.append(_("kickLookupRow", 
							new String[] { date, server, reason, kick.getStaff()}));
				}
			}

			msg.append(footer.replace("{entity}", entity).replace("{module}", "Kick")
					.replace("{page}", page + "/" + totalPages));

			return FormatUtils.formatNewLine(ChatColor.translateAlternateColorCodes('&', msg.toString()));
		}
		public static List<BaseComponent[]> commentRowLookup(final String entity, final List<CommentEntry> comments,
				int page, final String header, final String footer, final boolean staffLookup) throws InvalidModuleException {{
			final StringBuilder msg = new StringBuilder();

			int totalPages = (int) Math.ceil((double)comments.size()/entriesPerPage);
			if(comments.size() > entriesPerPage){
				if(page > totalPages){
					page = totalPages;
				}
				int beginIndex = (page - 1) * entriesPerPage;
				int endIndex = (beginIndex + entriesPerPage < comments.size()) ? beginIndex + entriesPerPage : comments.size();
				for(int i=comments.size() -1; i > 0; i--){
					if(i >= beginIndex && i < endIndex){
						continue;
					}
					comments.remove(i);
				}
			}
			msg.append(header.replace("{entity}", entity).replace("{module}", "Comment")
					.replace("{page}", page + "/" + totalPages));
			
			msg.append("&6&lComment list :");
			
			for(final CommentEntry comm : comments){
				msg.append("\n");
				if(staffLookup){
					msg.append(_("commentRow", new String[]{String.valueOf(comm.getID()), 
							(comm.getType() == Type.NOTE) ? "&eComment" : "&cWarning", comm.getContent(),
							comm.getFormattedDate(), comm.getAuthor()}));
				}
				else{
					msg.append(_("commentStaffRow", new String[]{String.valueOf(comm.getID()), 
							(comm.getType() == Type.NOTE) ? "&eComment" : "&cWarning", 
							comm.getEntity(), comm.getContent(), comm.getFormattedDate()}));
				}
			}

			msg.append(footer.replace("{entity}", entity).replace("{module}", "Comment")
					.replace("{page}", page + "/" + totalPages));

			return FormatUtils.formatNewLine(ChatColor.translateAlternateColorCodes('&', msg.toString()));
		}
	}
	}
		
	public static class StaffLookupCmd extends BATCommand {
		private final ModulesManager modules;
		private final String lookupHeader = "\n&f---- &9Staff Lookup &f- &b{entity} &f-&a {module} &f-&6 Page {page} &f----\n";
		private final String lookupFooter = "\n&f---- &9Staff Lookup &f- &b{entity} &f-&a {module} &f-&6 Page {page} &f----";
		
		public StaffLookupCmd() {
			super("stafflookup", "<staff> [module] [page]", "Displays a staff member history (universal or per module).", "bat.stafflookup");
			modules = BAT.getInstance().getModules();
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			final String entity = args[0];
			if(args.length == 1){
				for (final BaseComponent[] msg : getSummaryStaffLookup(entity, sender.hasPermission(Action.LOOKUP.getPermission() + ".displayip"))) {
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
							message = LookupCmd.formatBanLookup(entity, bans, page, lookupHeader, lookupFooter, true);
						}else{
							message = new ArrayList<BaseComponent[]>();
							message.add(BAT.__("&b" + entity + "&e has never performed any operation concerning ban."));
						}
						break;
					case "mute":
						final List<MuteEntry> mutes = modules.getMuteModule().getManagedMute(entity);
						if(!mutes.isEmpty()){
							message = LookupCmd.formatMuteLookup(entity, mutes, page, lookupHeader, lookupFooter, true);
						}else{
							message = new ArrayList<BaseComponent[]>();
							message.add(BAT.__("&b" + entity + "&e has never performed any operation concerning mute."));
						}
						break;
					case "kick":
						final List<KickEntry> kicks = modules.getKickModule().getManagedKick(entity);
						if(!kicks.isEmpty()){
							message = LookupCmd.formatKickLookup(entity, kicks, page, lookupHeader, lookupFooter, true);
						}else{
							message = new ArrayList<BaseComponent[]>();
							message.add(BAT.__("&b" + entity + "&e has never performed any operation concerning kick."));
						}
						break;
					case "comment":
						final List<CommentEntry> comments = modules.getCommentModule().getManagedComments(entity);
						if(!comments.isEmpty()){
							message = LookupCmd.commentRowLookup(entity, comments, page, lookupHeader, lookupFooter, true);
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
		
		public List<BaseComponent[]> getSummaryStaffLookup(final String staff, final boolean displayID) {
			final StringBuilder msg = new StringBuilder();
			msg.append(lookupHeader.replace("{entity}", staff).replace("{module}", "Summary").replace("{page}", "1/1"));

			msg.append("&eStatistics :");
			try{
				if(modules.isLoaded("ban")){
					int banNo = 0;
					int unbanNo = 0;
					for(final BanEntry ban : modules.getBanModule().getManagedBan(staff)){
						if(staff.equalsIgnoreCase(ban.getStaff())){
							banNo++;
						}
						if(staff.equalsIgnoreCase(ban.getUnbanStaff())){
							unbanNo++;
						}
					}
					msg.append("\n&b" + staff + "&e has issued &c" + banNo + " bans &eand &a" + unbanNo + " unbans.");
				}
				if(modules.isLoaded("mute")){
					int muteNo = 0;
					int unmuteNo = 0;
					for(final MuteEntry mute : modules.getMuteModule().getManagedMute(staff)){
						if(staff.equalsIgnoreCase(mute.getStaff())){
							muteNo++;
						}
						if(staff.equalsIgnoreCase(mute.getUnmuteStaff())){
							unmuteNo++;
						}
					}
					msg.append("\n&b" + staff + "&e has issued &c" + muteNo + " mutes &eand &a" + unmuteNo + " unmutes.");
				}
				if(modules.isLoaded("kick")){
					int kickNo = 0;
					for(final KickEntry kick : modules.getKickModule().getManagedKick(staff)){
						if(staff.equalsIgnoreCase(kick.getStaff())){
							kickNo++;
						}
					}
					msg.append("\n&b" + staff + "&e has issued &c" + kickNo + " kicks.");
				}
				if(modules.isLoaded("comment")){
					int commentNo = 0;
					int warningNo = 0;
					for(final CommentEntry mute : modules.getCommentModule().getManagedComments(staff)){
						if(mute.getType() == Type.NOTE){
							commentNo++;
						}
						else{
							warningNo++;
						}
					}
					msg.append("\n&b" + staff + "&e has written &c" + commentNo + " comments &eand &a" + warningNo + " warnings.");
				}
				// It means the only loaded module is core
				if(modules.getLoadedModules().size() == 1){
					msg.append("\nNo informations were found on this staff memeber.");
				}
			}catch(final InvalidModuleException e){
				e.printStackTrace();
			}
			
			msg.append(lookupFooter.replace("{entity}", staff).replace("{module}", "Summary").replace("{page}", "1/1"));

			return FormatUtils.formatNewLine(ChatColor.translateAlternateColorCodes('&', msg.toString()));
		}
	}
	
	public static class ConfirmCmd extends BATCommand {
		public ConfirmCmd() {
			super("confirm", "", "Confirm your queued command.", "");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			if (!CommandQueue.executeQueueCommand(sender)) {
				sender.sendMessage(__("noQueuedCommand"));
			}
		}

	}

	@RunAsync
	public static class ImportCmd extends BATCommand{
	    private final static Map<String, Importer> importers = new HashMap<String, Importer>(){{
            importers.put("bungeeSuiteBans", new BungeeSuiteImporter());
            importers.put("geSuitBans", new GeSuiteImporter());
            importers.put("MC-Previous1.7.8", new MinecraftPreUUIDImporter());
	    }};
	    
		public ImportCmd() { 
		    super("import", "<bungeeSuiteBans/geSuitBans/MC-Previous1.7>", "Imports ban data from the specified source. Available sources : &a" 
		            + Joiner.on("&e,&a").join(importers.keySet()), "bat.import");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
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
                                if(throwable.getMessage() != null){
                                    sender.sendMessage(BAT.__("An error has occured during the import. Please check the logs"));
                                }else{
                                    sender.sendMessage(BAT.__(throwable.getMessage()));
                                }
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
                });
			}else{
			    throw new IllegalArgumentException("The specified source is incorrect. Available sources : &a" 
	                + Joiner.on("&e,&a").join(importers.keySet()));
			}
		}
	}

	public static class BackupCmd extends BATCommand{
		public BackupCmd() { super("backup", "", "Backup the BAT's data from the mysql database into a file", "bat.backup");}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
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
	
	@Disable
	@RunAsync
	public static class MigrateCmd extends BATCommand {
		public MigrateCmd() { super("migrate", "<target>", "Migrate from the source to the target datasource (mysql or sqlite)", "bat.migrate");}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			final String target = args[1];
			checkArgument(!Arrays.asList("mysql", "sqlite").contains(target.toLowerCase()), "Target must be mysql or sqlite.");	
			if("sqlite".equalsIgnoreCase(target)){
				checkArgument(!DataSourceHandler.isSQLite(), "SQLite is already used.");
			}else if("mysql".equalsIgnoreCase(target)){
				checkArgument(DataSourceHandler.isSQLite(), "MySQL is already used.");
			}
			BAT.getInstance().migrate(target);
		}
	}
}