package fr.Alphart.BAT.Modules.Core;

import static fr.Alphart.BAT.I18n.I18n._;
import static fr.Alphart.BAT.I18n.I18n.__;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ServerInfo;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.imaginarycode.minecraft.redisbungee.RedisBungee;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.InvalidModuleException;
import fr.Alphart.BAT.Modules.ModulesManager;
import fr.Alphart.BAT.Modules.Ban.BanEntry;
import fr.Alphart.BAT.Modules.Comment.CommentEntry;
import fr.Alphart.BAT.Modules.Comment.CommentEntry.Type;
import fr.Alphart.BAT.Modules.Kick.KickEntry;
import fr.Alphart.BAT.Modules.Mute.MuteEntry;
import fr.Alphart.BAT.Utils.FormatUtils;
import fr.Alphart.BAT.Utils.Utils;
import fr.Alphart.BAT.Utils.thirdparty.MojangAPIProvider;

public class LookupFormatter {
    private ModulesManager modules;
    private static final int entriesPerPage = 15;
    private final String lookupHeader;
    private final String lookupFooter;
    private final String currentPunishmentHover= "{effect=\"hover\" text=\"{server}\" onHoverText=\"&eStaff: &a{staff}, &eReason: &a{reason},"
        + "{newlinehover} &eBegin: &a{begin}\"}";
    
    public LookupFormatter(){
        lookupHeader = _("perModuleLookupHeader");
        lookupFooter = _("perModuleLookupFooter");
        modules = BAT.getInstance().getModules();
    }

    public List<BaseComponent[]> getSummaryLookupPlayer(final String pName, final boolean displayIP) {
        // Gather players data related to each modules
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
        // Compute player's state (as well as his ip) concerning ban and mute
        for (final BanEntry banEntry : pDetails.getBans()) {
            if (banEntry.isActive()) {
                isBan = true;
                banServers.add(currentPunishmentHover.replace("{server}", banEntry.getServer())
                    .replace("{staff}", banEntry.getStaff()).replace("{reason}", banEntry.getReason())
                    .replace("{begin}", Core.defaultDF.format(banEntry.getBeginDate())));
            }
        }
        for (final BanEntry banEntry : ipDetails.getBans()) {
            if (banEntry.isActive()) {
                isBanIP = true;
                banIPServers.add(currentPunishmentHover.replace("{server}", banEntry.getServer())
                    .replace("{staff}", banEntry.getStaff()).replace("{reason}", banEntry.getReason())
                    .replace("{begin}", Core.defaultDF.format(banEntry.getBeginDate())));
            }
        }
        for (final MuteEntry muteEntry : pDetails.getMutes()) {
            if (muteEntry.isActive()) {
                isMute = true;
                muteServers.add(currentPunishmentHover.replace("{server}", muteEntry.getServer())
                    .replace("{staff}", muteEntry.getStaff()).replace("{reason}", muteEntry.getReason())
                    .replace("{begin}", Core.defaultDF.format(muteEntry.getBeginDate())));
            }
        }
        for (final MuteEntry muteEntry : ipDetails.getMutes()) {
            if (muteEntry.isActive()) {
                isMuteIP = true;
                muteIPServers.add(currentPunishmentHover.replace("{server}", muteEntry.getServer())
                    .replace("{staff}", muteEntry.getStaff()).replace("{reason}", muteEntry.getReason())
                    .replace("{begin}", Core.defaultDF.format(muteEntry.getBeginDate())));
            }
        }
        bansNumber = pDetails.getBans().size() + ipDetails.getBans().size();
        mutesNumber = pDetails.getMutes().size() + ipDetails.getMutes().size();
        kicksNumber = pDetails.getKicks().size();
        
        // Load the lookup pattern
        String lookupPattern = _("playerLookup");
        
        // Initialize all the strings to prepare the big replace
        String connection_state;
        if (BAT.getInstance().getRedis().isRedisEnabled()) {
                UUID pUUID = RedisBungee.getApi().getUuidFromName(pName, true);
                if(pUUID != null && RedisBungee.getApi().isPlayerOnline(pUUID)){
                    ServerInfo si = RedisBungee.getApi().getServerFor(pUUID);
                    connection_state = _("connectionStateOnline").replace("{server}", si != null ? si.getName() : "unknown state");
                }else{
                    connection_state = _("connectionStateOffline");
                }
        } else {
            if(ProxyServer.getInstance().getPlayer(pName) != null){
                connection_state = _("connectionStateOnline")
                        .replace("{server}", ProxyServer.getInstance().getPlayer(pName).getServer().getInfo().getName());
            }else{
                connection_state = _("connectionStateOffline");
            }

        }
        
        final String joinChar = "&f, &3";
        final String ban_servers = !banServers.isEmpty()
                ? Joiner.on(joinChar).join(banServers).toLowerCase()
                : _("none");
        final String banip_servers = !banIPServers.isEmpty()
                ? Joiner.on(joinChar).join(banIPServers).toLowerCase()
                : _("none");
        final String mute_servers = !muteServers.isEmpty()
                ? Joiner.on(joinChar).join(muteServers).toLowerCase()
                : _("none");
        final String muteip_servers = !muteIPServers.isEmpty()
                ? Joiner.on(joinChar).join(muteIPServers).toLowerCase()
                : _("none");

        final String first_login = pDetails.getFirstLogin() != EntityEntry.noDateFound
                ? Core.defaultDF.format(new Date(pDetails.getFirstLogin().getTime()))
                : _("unknownDate");
        final String last_login = pDetails.getLastLogin() != EntityEntry.noDateFound
                ? Core.defaultDF.format(new Date(pDetails.getLastLogin().getTime()))
                : _("unknownDate");
        final String last_ip = !"0.0.0.0".equals(pDetails.getLastIP())
                ? ((displayIP) ? pDetails.getLastIP() : _("hiddenIp"))
                : _("unknownIp");
                
        String name_history_list;
        // Create a function for that or something better than a big chunk of code inside the lookup
        if(Core.isOnlineMode()){
            try{
                name_history_list = Joiner.on("&e, &a").join(MojangAPIProvider.getPlayerNameHistory(pName));
            }catch(final RuntimeException e){
                name_history_list = "unable to fetch player's name history. Check the logs";
                BAT.getInstance().getLogger().severe("An error occured while fetching " + pName + "'s name history from mojang servers."
                        + "Please report this : ");
                e.printStackTrace();
            }
        }else{
            name_history_list = "offline server";
        }
        
        int commentsNumber = pDetails.getComments().size();
        String last_comments = "";
        // We need to parse the number of last comments from the lookup pattern
        final Pattern lastCommentsPattern = Pattern.compile("(?:.|\n)*?\\{last_comments:(\\d*)\\}(?:.|\n)*?");
        final Matcher matcher = lastCommentsPattern.matcher(lookupPattern);
        try{
            if(!matcher.matches()){
                throw new NumberFormatException();
            }
            int nLastComments = Integer.parseInt(matcher.group(1));
            if(nLastComments < 1){
                throw new NumberFormatException();
            }
            int i = 0;
            for(final CommentEntry comm : pDetails.getComments()){
                last_comments += _("commentRow", new String[]{String.valueOf(comm.getID()), 
                        (comm.getType() == Type.NOTE) ? "&eComment" : "&cWarning", comm.getContent(),
                        comm.getFormattedDate(), comm.getAuthor()});
                i++;
                if(i == 3){
                    break;
                }
            }
            if(last_comments.isEmpty()){
                last_comments = _("none");
            }
        }catch(final NumberFormatException e){
            last_comments = "Unable to parse the number of last_comments";
        }
        
        final String ip_users;
        if("0.0.0.0".equals(pDetails.getLastIP())){
          ip_users = _("unknownIp");
        }else{
          ip_users = !ipDetails.getUsers().isEmpty()
              ? Joiner.on(joinChar).join(ipDetails.getUsers())
              : _("none");
        }
        
        final List<BaseComponent[]> finalMessage = FormatUtils.formatNewLine(ChatColor.translateAlternateColorCodes('&',
                lookupPattern
                .replace("{connection_state}", connection_state)
                .replace("{ban_servers}", ban_servers).replace("{banip_servers}", banip_servers)
                .replace("{mute_servers}", mute_servers).replace("{muteip_servers}", muteip_servers)
                .replace("{first_login}", first_login).replace("{last_login}", last_login).replace("{last_ip}", last_ip)
                .replace("{bans_number}", String.valueOf(bansNumber)).replace("{mutes_number}", String.valueOf(mutesNumber))
                .replace("{kicks_number}", String.valueOf(kicksNumber)).replace("{comments_number}", String.valueOf(commentsNumber))
                .replace("{name_history_list}", name_history_list).replaceAll("\\{last_comments:\\d\\}", last_comments)
                .replace("{player}", pName).replace("{uuid}", Core.getUUID(pName))
                .replace("{ip_users}", ip_users)
                // '¤' is used as a space character, so we replace it with space and display correctly the escaped one
                .replace("¤", " ").replace("\\¤", "¤")
                ));
        
        return finalMessage;
    }
    
    public List<BaseComponent[]> getSummaryLookupIP(final String ip) {
        final EntityEntry ipDetails = new EntityEntry(ip);
        if (!ipDetails.exist()) {
            final List<BaseComponent[]> returnedMsg = new ArrayList<BaseComponent[]>();
            returnedMsg.add(__("unknownIp"));
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

        // Initialize all strings
        final String joinChar = "&f, &3";
        final String ip_users = !ipDetails.getUsers().isEmpty()
                ? Joiner.on(joinChar).join(ipDetails.getUsers())
                : _("none");
        final String ban_servers = !banServers.isEmpty()
                ? Joiner.on(joinChar).join(banServers).toLowerCase()
                : _("none");
        final String mute_servers = !muteServers.isEmpty()
                ? Joiner.on(joinChar).join(muteServers).toLowerCase()
                : _("none");
        
        String replacedString = _("ipLookup")
                .replace("{ban_servers}", ban_servers).replace("{mute_servers}", mute_servers)
                .replace("{bans_number}", String.valueOf(bansNumber)).replace("{mutes_number}", String.valueOf(mutesNumber))
                .replace("{ip}", ip).replace("{ip_users}", ip_users)
                // '¤' is used as a space character, so we replace it with space and display correctly the escaped one
                .replace("¤", " ").replace("\\¤", "¤");
                
        if(replacedString.contains("{ip_location}")){
            String ipLocation = "";
            try{
                ipLocation = Utils.getIpDetails(ip);
            }catch(final Exception e){
                BAT.getInstance().getLogger().log(Level.SEVERE, 
                        "Error while fetching ip location from the API. Please report this :", e);
                ipLocation = "unresolvable ip location. Check your logs";
            }
            replacedString = replacedString.replace("{ip_location}", ipLocation);
        }
        
        final List<BaseComponent[]> finalMessage = FormatUtils.formatNewLine(ChatColor.translateAlternateColorCodes('&',
                replacedString));

        return finalMessage;
    }
    
    public List<BaseComponent[]> getSummaryStaffLookup(final String staff, final boolean displayID) {
        int bans_number = 0;
        int unbans_number = 0;
        int mutes_number = 0;
        int unmutes_number = 0;
        int kicks_number = 0;
        int comments_number = 0;
        int warnings_number = 0;
        try{
            if(modules.isLoaded("ban")){
                for(final BanEntry ban : modules.getBanModule().getManagedBan(staff)){
                    if(staff.equalsIgnoreCase(ban.getStaff())){
                        bans_number++;
                    }
                    if(staff.equalsIgnoreCase(ban.getUnbanStaff())){
                        unbans_number++;
                    }
                }
            }
            if(modules.isLoaded("mute")){
                for(final MuteEntry mute : modules.getMuteModule().getManagedMute(staff)){
                    if(staff.equalsIgnoreCase(mute.getStaff())){
                        mutes_number++;
                    }
                    if(staff.equalsIgnoreCase(mute.getUnmuteStaff())){
                        unmutes_number++;
                    }
                }
            }
            if(modules.isLoaded("kick")){
                for(final KickEntry kick : modules.getKickModule().getManagedKick(staff)){
                    if(staff.equalsIgnoreCase(kick.getStaff())){
                        kicks_number++;
                    }
                }
            }
            if(modules.isLoaded("comment")){
                for(final CommentEntry mute : modules.getCommentModule().getManagedComments(staff)){
                    if(mute.getType() == Type.NOTE){
                        comments_number++;
                    }
                    else{
                        warnings_number++;
                    }
                }
            }
        }catch(final InvalidModuleException e){
            e.printStackTrace();
        }
        
        final List<BaseComponent[]> finalMessage = FormatUtils.formatNewLine(ChatColor.translateAlternateColorCodes('&',
                _("staffLookup")
                .replace("{bans_number}", String.valueOf(bans_number)).replace("{unbans_number}", String.valueOf(unbans_number))
                .replace("{mutes_number}", String.valueOf(mutes_number)).replace("{unmutes_number}", String.valueOf(unmutes_number))
                .replace("{kicks_number}", String.valueOf(kicks_number))
                .replace("{comments_number}", String.valueOf(comments_number))
                .replace("{warnings_number}", String.valueOf(warnings_number))
                .replace("{staff}", staff).replace("{uuid}", Core.getUUID(staff))
                .replace("¤", " ").replace("\\¤", "¤")
                ));

        return finalMessage;
    }
    
    public List<BaseComponent[]> formatBanLookup(final String entity, final List<BanEntry> bans, 
            int page, final boolean staffLookup) throws InvalidModuleException {
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
        msg.append(lookupHeader.replace("{entity}", entity).replace("{module}", "Ban")
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

        msg.append(lookupFooter.replace("{entity}", entity).replace("{module}", "Ban")
                .replace("{page}", page + "/" + totalPages));

        return FormatUtils.formatNewLine(ChatColor.translateAlternateColorCodes('&', msg.toString()));
    }
    
    public List<BaseComponent[]> formatMuteLookup(final String entity, final List<MuteEntry> mutes,
            int page, final boolean staffLookup) throws InvalidModuleException {
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
        msg.append(lookupHeader.replace("{entity}", entity).replace("{module}", "Mute")
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

        msg.append(lookupFooter.replace("{entity}", entity).replace("{module}", "Mute")
                .replace("{page}", page + "/" + totalPages));

        return FormatUtils.formatNewLine(ChatColor.translateAlternateColorCodes('&', msg.toString()));
    }
    
    public List<BaseComponent[]> formatKickLookup(final String entity, final List<KickEntry> kicks,
            int page, final boolean staffLookup) throws InvalidModuleException {
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
        msg.append(lookupHeader.replace("{entity}", entity).replace("{module}", "Kick")
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

        msg.append(lookupFooter.replace("{entity}", entity).replace("{module}", "Kick")
                .replace("{page}", page + "/" + totalPages));

        return FormatUtils.formatNewLine(ChatColor.translateAlternateColorCodes('&', msg.toString()));
    }
    
    public List<BaseComponent[]> commentRowLookup(final String entity, final List<CommentEntry> comments,
            int page, final boolean staffLookup) throws InvalidModuleException {{
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
        msg.append(lookupHeader.replace("{entity}", entity).replace("{module}", "Comment")
                .replace("{page}", page + "/" + totalPages));
        
        msg.append("&6&lComment list :");
        
        for(final CommentEntry comm : comments){
            msg.append("\n");
            if(staffLookup){
                msg.append(_("commentStaffRow", new String[]{String.valueOf(comm.getID()), 
                        (comm.getType() == Type.NOTE) ? "&eComment" : "&cWarning", 
                        comm.getEntity(), comm.getContent(), comm.getFormattedDate()}));
            }
            else{
                msg.append(_("commentRow", new String[]{String.valueOf(comm.getID()), 
                    (comm.getType() == Type.NOTE) ? "&eComment" : "&cWarning", comm.getContent(),
                    comm.getFormattedDate(), comm.getAuthor()}));
            }
        }

        msg.append(lookupFooter.replace("{entity}", entity).replace("{module}", "Comment")
                .replace("{page}", page + "/" + totalPages));

        return FormatUtils.formatNewLine(ChatColor.translateAlternateColorCodes('&', msg.toString()));
    }
    
}
}
