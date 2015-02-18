package fr.Alphart.BAT.Modules.Core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;

import com.google.common.base.Charsets;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.gson.Gson;
import com.mojang.api.profiles.Profile;
import com.mojang.api.profiles.ProfileCriteria;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.IModule;
import fr.Alphart.BAT.Utils.CallbackUtils.ProgressCallback;
import fr.Alphart.BAT.Utils.UUIDNotFoundException;
import fr.Alphart.BAT.Utils.Utils;
import fr.Alphart.BAT.database.DataSourceHandler;
import fr.Alphart.BAT.database.SQLQueries;

public abstract class Importer {
    protected Gson gson = new Gson();
    protected final LoadingCache<String, String> uuidCache = CacheBuilder.newBuilder().maximumSize(10000)
            .expireAfterAccess(30, TimeUnit.MINUTES).build(new CacheLoader<String, String>() {
                public String load(final String pName) throws UUIDNotFoundException {
                    if (ProxyServer.getInstance().getConfig().isOnlineMode()) {
                        String uuid = getUUIDusingMojangAPI(pName);
                        if (uuid != null) {
                            return uuid;
                        } else {
                            throw new UUIDNotFoundException(pName);
                        }
                    } else {
                        return java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + pName).getBytes(Charsets.UTF_8))
                                .toString().replaceAll("-", "");
                    }

                }
            });
    protected ImportStatus status;
    
    
    private String getUUIDusingMojangAPI(final String pName) {
        final Profile[] profiles = Core.getProfileRepository().findProfilesByCriteria(new ProfileCriteria(pName, "minecraft"));

        if (profiles.length > 0) {
            return profiles[0].getId();
        } else {
            return null;
        }
    }
    
    protected abstract void importData(final ProgressCallback<ImportStatus> progressionCallback, final String... additionnalsArgs) throws Exception;
    
    public void startImport(final ProgressCallback<ImportStatus> progressionCallback, final String... additionnalsArgs){
        try {
            importData(progressionCallback, additionnalsArgs);
        }catch (final Throwable t) {
            progressionCallback.done(null, t);
        }
    }
    
    /**
     * Create a row for this player in the table BAT_player though some informations are unknown
     * this will avoid a lot of errors
     * @param conn | sql connection to use
     * @param pName
     * @param UUID
     */
    public void initPlayerRowInBatPlayer(final Connection conn, final String pName, final String UUID) throws SQLException{
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO `" + SQLQueries.Core.table + "` (BAT_player, UUID, lastip, firstlogin, lastlogin)"
                + " VALUES (?, ?, '0.0.0.0', null, null) ON DUPLICATE KEY UPDATE BAT_player = BAT_player;");
        stmt.setString(1, pName);
        stmt.setString(2, UUID);
        stmt.executeUpdate();
    }
    
    @Getter
    class ImportStatus{
        // The total number of entries to process (processed and remaining)
        private final int totalEntries;
        private int convertedEntries;
        
        public ImportStatus(final int totalEntries){
            if(totalEntries < 1){
                throw new IllegalArgumentException("There is no entry to convert.");
            }
            this.totalEntries = totalEntries;
            convertedEntries = 0;
        }
        
        public int incrementConvertedEntries(final int incrementValue){
            return convertedEntries = convertedEntries + incrementValue;
        }
        
        public double getProgressionPercent(){
            return (((double)convertedEntries / (double)totalEntries) * 100);
        }
        
        public int getRemainingEntries(){
            return totalEntries - convertedEntries;
        }
    }
    
    public static class BungeeSuiteImporter extends Importer{

        @Override
        protected void importData(final ProgressCallback<ImportStatus> progressionCallback, String... additionalsArgs) throws Exception{
            ResultSet res = null;
            try (Connection conn = BAT.getConnection()) {
                // Check if the bungee suite tables are here
                final DatabaseMetaData dbm = conn.getMetaData();
                for(final String table : Arrays.asList("BungeeBans", "BungeePlayers")){
                    final ResultSet tables = dbm.getTables(null, null, table, null);
                    if (!tables.next()) {
                        throw new IllegalArgumentException("The table " + table + " wasn't found. Import aborted ...");
                    }
                }

                // Count the number of entries (use to show the progression)
                final ResultSet resCount = conn.prepareStatement("SELECT COUNT(*) FROM BungeeBans;").executeQuery();
                if(resCount.next()){
                    status = new ImportStatus(resCount.getInt("COUNT(*)"));
                }

                final PreparedStatement insertBans = conn.prepareStatement("INSERT INTO `" + SQLQueries.Ban.table
                        + "`(UUID, ban_ip, ban_staff, ban_server, ban_begin, ban_end, ban_reason) VALUES (?, ?, ?, ?, ?, ?, ?);");
                final PreparedStatement getIP = conn.prepareStatement("SELECT ipaddress FROM BungeePlayers WHERE playername = ?;");

                res = conn.createStatement().executeQuery("SELECT * FROM BungeeBans;");
                int uncomittedEntries = 0;
                conn.setAutoCommit(false);
                
                while (res.next()) {
                    final boolean ipBan = "ipban".equals(res.getString("type"));

                    final String pName = res.getString("player");
                    final String server = IModule.GLOBAL_SERVER;
                    final String staff = res.getString("banned_by");
                    final String reason = res.getString("reason");
                    final Timestamp ban_begin = res.getTimestamp("banned_on");
                    Timestamp ban_end = res.getTimestamp("banned_until");
                    
                    /* For unknown reason BungeeBans table contained (hardly ever but it did) date with a year > 3000,
                     * not sure if that was some kind of joke from a staff member ... Anyways this code convert long-duration
                       tempban to definitive ban */
                    if(ban_end == null || ban_end.getTime() > System.currentTimeMillis() + 10 * (365 * (24 * 3600))){
                        ban_end = null;
                    }

                    // Get the ip
                    String ip = null;
                    getIP.setString(1, pName);  
                    final ResultSet resIP = getIP.executeQuery();
                    if(resIP.next()){
                        ip = resIP.getString("ipaddress");
                    }
                    resIP.close();
                    if(ipBan && ip == null){
                        continue;
                    }

                    // Get UUID
                    String UUID = null;
                    try{
                        UUID = uuidCache.get(pName);
                    } catch (UncheckedExecutionException e) {
                        if(e.getCause() instanceof UUIDNotFoundException){
                            continue;
                        }else{
                            throw e;
                        }
                    }

                    // Insert the ban
                    insertBans.setString(1, (ipBan) ? null : UUID);
                    insertBans.setString(2, (ipBan) ? ip : null);
                    insertBans.setString(3, staff);
                    insertBans.setString(4, server);
                    insertBans.setTimestamp(5, ban_begin);
                    insertBans.setTimestamp(6, ban_end);
                    insertBans.setString(7, reason);
                    insertBans.execute();
                    insertBans.clearParameters();
                    getIP.clearParameters();
                    uncomittedEntries++;
                    
                    initPlayerRowInBatPlayer(conn, pName, UUID);
                    if(uncomittedEntries % 100 == 0){
                        conn.commit();
                        status.incrementConvertedEntries(uncomittedEntries);
                        uncomittedEntries = 0;
                        progressionCallback.onProgress(status);
                    }
                }

                conn.commit();
                status.incrementConvertedEntries(uncomittedEntries);
                progressionCallback.done(status, null);
            }finally{
                if(res != null){
                    DataSourceHandler.close(res);
                }
            }
        }
        
    }

    public static class GeSuiteImporter extends Importer{

        @Override
        protected void importData(final ProgressCallback<ImportStatus> progressionCallback, String... additionalsArgs) throws Exception {
            ResultSet res = null;
            try (Connection conn = BAT.getConnection()) {
                // Check if the bungee suite tables are here
                final DatabaseMetaData dbm = conn.getMetaData();
                for(final String table : Arrays.asList("bans", "players")){
                    final ResultSet tables = dbm.getTables(null, null, table, null);
                    if (!tables.next()) {
                        throw new IllegalArgumentException("The table " + table + " wasn't found. Import aborted ...");
                    }
                }

                // Count the number of entries (use to show the progression)
                final ResultSet resCount = conn.prepareStatement("SELECT COUNT(*) FROM bans;").executeQuery();
                if(resCount.next()){
                    status = new ImportStatus(resCount.getInt("COUNT(*)"));
                }

                final PreparedStatement insertBans = conn.prepareStatement("INSERT INTO `" + SQLQueries.Ban.table
                        + "`(UUID, ban_ip, ban_staff, ban_server, ban_begin, ban_end, ban_reason) VALUES (?, ?, ?, ?, ?, ?, ?);");
                final PreparedStatement getIP = conn.prepareStatement("SELECT ipaddress FROM players WHERE playername = ?;");
                res = conn.createStatement().executeQuery("SELECT * bans;");
                
                int uncomittedEntries = 0;
                conn.setAutoCommit(false);
                while (res.next()) {
                    final boolean ipBan = "ipban".equals(res.getString("type"));

                    final String pName = res.getString("banned_playername");
                    final String UUID = res.getString("banned_uuid");
                    final String server = IModule.GLOBAL_SERVER;
                    final String staff = res.getString("banned_by");
                    final String reason = res.getString("reason");
                    final Timestamp ban_begin = res.getTimestamp("banned_on");
                    final Timestamp ban_end = res.getTimestamp("banned_until");

                    // Get the ip
                    String ip = null;
                    getIP.setString(1, pName);  
                    final ResultSet resIP = getIP.executeQuery();
                    if(resIP.next()){
                        ip = resIP.getString("ipaddress");
                    }
                    resIP.close();
                    if(ipBan && ip == null){
                        continue;
                    }

                    // Insert the ban
                    insertBans.setString(1, (ipBan) ? null : UUID);
                    insertBans.setString(2, (ipBan) ? ip : null);
                    insertBans.setString(3, staff);
                    insertBans.setString(4, server);
                    insertBans.setTimestamp(5, ban_begin);
                    insertBans.setTimestamp(6, ban_end);
                    insertBans.setString(7, reason);
                    insertBans.execute();
                    insertBans.clearParameters();
                    getIP.clearParameters();
                    uncomittedEntries++;

                    if(!ipBan){
                        initPlayerRowInBatPlayer(conn, pName, UUID);
                    }
                    if(uncomittedEntries % 100 == 0){
                        conn.commit();
                        status.incrementConvertedEntries(uncomittedEntries);
                        uncomittedEntries = 0;
                        progressionCallback.onProgress(status);
                    }
                }

                conn.commit();
                status.incrementConvertedEntries(uncomittedEntries);
                progressionCallback.done(status, null);
            }finally{
                if(res != null){
                    DataSourceHandler.close(res);
                }
            }
        }
        
    }
    
    public static class MinecraftPreUUIDImporter extends Importer{
        private static final DateFormat dfMc1v6 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        
        @Override
        protected void importData(ProgressCallback<ImportStatus> progressionCallback, final String... additionalsArgs) throws Exception {
            try (Connection conn = BAT.getConnection()) {
                // Check if either the banned-players.txt or the banned-ips.txt file exists
                if(!new File(BAT.getInstance().getDataFolder(), "banned-players.txt").exists() 
                    && !new File(BAT.getInstance().getDataFolder(), "banned-ips.txt").exists()){
                    throw new IllegalArgumentException("You must put either banned-players.txt or banned-ips.txt file into BAT folder to "
                            + "import your datas.");
                }
                
                // Count the totalEntries which need to be converted
                int totalEntries = 0;
                final List<File> filesToConvert = new ArrayList<>();
                if(new File(BAT.getInstance().getDataFolder(), "banned-players.txt").exists()){
                    filesToConvert.add(new File(BAT.getInstance().getDataFolder(), "banned-players.txt"));
                }
                if(new File(BAT.getInstance().getDataFolder(), "banned-ips.txt").exists()){
                    filesToConvert.add(new File(BAT.getInstance().getDataFolder(), "banned-ips.txt"));
                }
                for(final File file : filesToConvert){
                    final BufferedReader br = new BufferedReader(new FileReader(file));
                    try{
                    while (br.readLine() != null) {
                        totalEntries++;
                    }
                    totalEntries -= 3; // Number of lines which are comment or blank in default mc ban file
                    } finally{
                        if(br != null){
                            br.close();
                        }
                    }
                }
                status = new ImportStatus(totalEntries);
                
                // Init the reader and some code
                final PreparedStatement insertBans = conn.prepareStatement("INSERT INTO `" + SQLQueries.Ban.table
                        + "`(UUID, ban_ip, ban_staff, ban_server, ban_begin, ban_end, ban_reason) VALUES (?, ?, ?, ?, ?, ?, ?);");
                BufferedReader brPlayer = null;
                if(new File(BAT.getInstance().getDataFolder(), "banned-players.txt").exists()){
                    brPlayer = new BufferedReader(new FileReader(new File(BAT.getInstance().getDataFolder(), "banned-players.txt")));
                }
                BufferedReader brIPs = null;
                if(new File(BAT.getInstance().getDataFolder(), "banned-ips.txt").exists()){
                    brIPs = new BufferedReader(new FileReader(new File(BAT.getInstance().getDataFolder(), "banned-ips.txt")));
                }
                
                // Proccess the import
                String line = null;
                conn.setAutoCommit(false);
                int uncomittedEntries = 0;
                while((brPlayer != null && (line = brPlayer.readLine()) != null) 
                        || (brIPs != null && (line = brIPs.readLine()) != null)){
                    try{
                        final Minecraft1v6_BanRecord banRecord = new Minecraft1v6_BanRecord(line);
                        insertBans.setString(1, banRecord.getUuid());
                        insertBans.setString(2, banRecord.getIp());
                        insertBans.setString(3, banRecord.getStaffBan());
                        insertBans.setString(4, IModule.GLOBAL_SERVER);
                        insertBans.setTimestamp(5, banRecord.getBeginBan());
                        insertBans.setTimestamp(6, banRecord.getExpirationBan());
                        insertBans.setString(7, banRecord.getReason());
                        insertBans.executeUpdate();
                        insertBans.clearParameters();
                        uncomittedEntries++;
                        
                        if(banRecord.getUuid() != null){
                            initPlayerRowInBatPlayer(conn, banRecord.getPName(), banRecord.getUuid());
                        }
                    }catch(final RuntimeException e){
                        if(!"commentline".equals(e.getMessage())){
                            progressionCallback.onMinorError(e.getMessage());
                        }
                    }
                    if(uncomittedEntries % 100 == 0){
                        conn.commit();
                        status.incrementConvertedEntries(uncomittedEntries);
                        uncomittedEntries = 0;
                        progressionCallback.onProgress(status);
                    }
                }
                
                conn.commit();
                status.incrementConvertedEntries(uncomittedEntries);
                progressionCallback.done(status, null);
            }catch (final IOException e){
                BAT.getInstance().getLogger().severe("An error related to files occured during the import of Minecraft v1.6 ban records :");
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        
        @Getter
        private class Minecraft1v6_BanRecord{
            private String uuid = null;
            private String pName = null;
            private String ip = null;
            private String reason;
            private String staffBan;
            private Timestamp beginBan;
            private Timestamp expirationBan;
            
            /**
             * Parse a line of banned-xx.txt file
             * @param line
             */
            public Minecraft1v6_BanRecord(final String line) throws RuntimeException{
                if(line.startsWith("#") || line.isEmpty()){
                    throw new RuntimeException("commentline");
                }
                final String[] splittedLine = line.split("\\|");
                if(splittedLine.length != 5){
                    throw new RuntimeException("Invalid ban format. The import process will continue ...");
                }
                if(Utils.validIP(splittedLine[0])){
                    ip = splittedLine[0];
                }else{
                    try {
                        pName = splittedLine[0];
                        uuid = uuidCache.get(splittedLine[0]);
                    } catch (ExecutionException e) {
                        if(e.getCause() instanceof UUIDNotFoundException){
                            throw new RuntimeException("The uuid of " + splittedLine[0] + " wasn't found. The import process will continue...");
                        }
                    }
                }
                try {
                    beginBan = new Timestamp(dfMc1v6.parse(splittedLine[1]).getTime());
                    expirationBan = (splittedLine[3].equals("Forever")) ? null : new Timestamp(dfMc1v6.parse(splittedLine[3]).getTime());
                } catch (final ParseException e) {
                    throw new RuntimeException("Invalid ban format. The import process will continue ...");
                }
                staffBan = (splittedLine[2].equals("(Unknown)")) ? "CONSOLE" : splittedLine[2];
                reason = splittedLine[4];
            }
        }
    }
    
    public static class MinecraftUUIDImporter extends Importer{
        private static final DateFormat dfMc = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        private final File bannedPlayersFile = new File(BAT.getInstance().getDataFolder(), "banned-players.json");
        private final File bannedIpsFile = new File(BAT.getInstance().getDataFolder(), "banned-ips.json");
        
        @Override
        protected void importData(ProgressCallback<ImportStatus> progressionCallback, final String... additionalsArgs) throws Exception {
            try (Connection conn = BAT.getConnection()) {
                // Check if either the banned-players.txt or the banned-ips.txt file exists
                if(!bannedPlayersFile.exists() 
                    && !bannedIpsFile.exists()){
                    throw new IllegalArgumentException("You must put either banned-players.json or banned-ips.json file into BAT folder to "
                            + "import your datas.");
                }
                
                // Init the mysql request, load the JSON entries from file and initialize the import status
                final PreparedStatement insertBans = conn.prepareStatement("INSERT INTO `" + SQLQueries.Ban.table
                        + "`(UUID, ban_ip, ban_staff, ban_server, ban_begin, ban_end, ban_reason) VALUES (?, ?, ?, ?, ?, ?, ?);");
                StringBuilder sb = new StringBuilder();
                Set<Map<String, String>> playerBanEntries = Sets.newHashSet();
                if(bannedPlayersFile.exists()){
                    BufferedReader brPlayer = new BufferedReader(new FileReader(bannedPlayersFile));
                    String temp;
                    while((temp = brPlayer.readLine()) != null){
                        sb.append(temp);
                    }
                    playerBanEntries = (Set<Map<String, String>>) gson.fromJson(sb.toString(), new TypeToken<Set<Map<String, Object>>>() {}.getType());
                    sb.setLength(0);
                    brPlayer.close();
                }
                Set<Map<String, String>> ipBanEntries = Sets.newHashSet();
                if(bannedIpsFile.exists()){
                    BufferedReader brIps = new BufferedReader(new FileReader(bannedIpsFile));
                    String temp;
                    while((temp = brIps.readLine()) != null){
                        sb.append(temp);
                    }
                    ipBanEntries = (Set<Map<String, String>>) gson.fromJson(sb.toString(), new TypeToken<Set<Map<String, Object>>>() {}.getType());
                    brIps.close();
                }
                status = new ImportStatus(playerBanEntries.size() + ipBanEntries.size());
                
                // Proccess the import
                conn.setAutoCommit(false);
                int uncomittedEntries = 0;
                for(final Map<String, String> banEntry : Iterables.concat(playerBanEntries, ipBanEntries)){
                    try{
                        final String UUID = (banEntry.containsKey("uuid"))
                                ? banEntry.get("uuid").replace("-","")
                                : null;
                        final String ip = (banEntry.containsKey("uuid"))
                                ? null
                                : banEntry.get("ip");
                        String staffBan = banEntry.get("source");
                        if(staffBan.equalsIgnoreCase("(Unknwown)") || staffBan.equalsIgnoreCase("console")){
                            staffBan = "CONSOLE";
                        }
                        final String reason = banEntry.get("reason");
                        final Timestamp beginBan = new Timestamp(dfMc.parse(banEntry.get("created")).getTime());
                        Timestamp expirationBan = null;
                        if(!banEntry.get("expires").equalsIgnoreCase("forever")){
                            expirationBan = new Timestamp(dfMc.parse(banEntry.get("expires")).getTime());
                        }
                        insertBans.setString(1, UUID);
                        insertBans.setString(2, ip);
                        insertBans.setString(3, staffBan);
                        insertBans.setString(4, IModule.GLOBAL_SERVER);
                        insertBans.setTimestamp(5, beginBan);
                        insertBans.setTimestamp(6, expirationBan);
                        insertBans.setString(7, reason);
                        insertBans.executeUpdate();
                        insertBans.clearParameters();
                        uncomittedEntries++;
                        
                        if(UUID != null){
                            initPlayerRowInBatPlayer(conn, banEntry.get("name"), UUID);
                        }
                    }catch(final RuntimeException e){
                        progressionCallback.onMinorError(e.getMessage());
                    }
                    if(uncomittedEntries % 100 == 0){
                        conn.commit();
                        status.incrementConvertedEntries(uncomittedEntries);
                        uncomittedEntries = 0;
                        progressionCallback.onProgress(status);
                    }
                }
                
                conn.commit();
                status.incrementConvertedEntries(uncomittedEntries);
                progressionCallback.done(status, null);
            }catch (final IOException e){
                BAT.getInstance().getLogger().severe("An error related to files occured during the import of Minecraft v1.6 ban records :");
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }
    
    public static class BanHammerImporter extends Importer{

        @Override
        protected void importData(final ProgressCallback<ImportStatus> progressionCallback, String... additionalsArgs) throws Exception {
            ResultSet res = null;
            boolean tableFound = true;
            boolean banHammerOnMysql = true;
            try (Connection conn = BAT.getConnection()) {
                // Check if the bungee suite tables are here
                final DatabaseMetaData dbm = conn.getMetaData();
                for(final String table : Arrays.asList("banhammer_bans", "banhammer_players")){
                    final ResultSet tables = dbm.getTables(null, null, table, null);
                    if (!tables.next()) {
                        tableFound = false;
                    }
                }
                // If mysql is on and the table weren't found, try to look for a banhammer.db file
                if(tableFound == false){
                    banHammerOnMysql = false;
                    if(new File(BAT.getInstance().getDataFolder(), "banhammer.db").exists()){
                        progressionCallback.onMinorError("The SQLite Driver must be downloaded. The server may freeze during the download.");
                        if(BAT.getInstance().loadSQLiteDriver()){
                            tableFound = true;
                        }
                    }else{
                        throw new RuntimeException("No BanHammer tables was found in the MySQL database. "
                                + "If you used a .db file with BanHammer, please put your file into BAT folder and rename it 'banhammer.db'");
                    }
                }
                
                progressionCallback.onMinorError("Starting importing data from " 
                        + ((banHammerOnMysql) ? "mysql" : "sqlite") + " database banhammer.");
                
                if(tableFound){
                    try(Connection connBH = (banHammerOnMysql) 
                            ? conn
                            : DriverManager.getConnection("jdbc:sqlite:" + BAT.getInstance().getDataFolder().getAbsolutePath() + File.separator
                                    + "banhammer.db");){
                        // Count the number of entries (use to show the progression)
                        final ResultSet resCount = connBH.prepareStatement("SELECT  " + (banHammerOnMysql ? "count()" : "COUNT(*)") + " FROM banhammer_bans;"
                            ).executeQuery();
                        if(resCount.next()){
                            status = new ImportStatus(resCount.getInt(banHammerOnMysql ? "count()" : "COUNT(*)"));
                        }

                        final PreparedStatement insertBans = conn.prepareStatement("INSERT INTO `" + SQLQueries.Ban.table
                            + "`(UUID, ban_ip, ban_staff, ban_server, ban_begin, ban_end, ban_reason, ban_state,"
                            + "ban_unbandate, ban_unbanstaff, ban_unbanreason) "
                            + "VALUES (?, null, ?, ?, ?, ?, ?, ?, ?, "
                            + "'Unspecified:BanHammer import', 'Unspecified:BanHammer import');");

                        res = banHammerOnMysql 
                                ? connBH.createStatement().executeQuery("SELECT bans.*, (SELECT players.name FROM banhammer_players players " +
                                    "WHERE bans.player_id = players.id) as player, (SELECT players.name FROM banhammer_players players " +
                                    "WHERE bans.creator_id = players.id) as staff FROM banhammer_bans bans;")
                                : connBH.createStatement().executeQuery("SELECT *, strftime('%s',created_at), strftime('%s',expires_at), " +
                                    "(SELECT players.name FROM banhammer_players players WHERE bans.player_id = players.id) as player, " +
                                    "(SELECT players.name FROM banhammer_players players WHERE bans.creator_id = players.id) as staff " +
                                    " FROM banhammer_bans bans;");
                        int uncomittedEntries = 0;
                        conn.setAutoCommit(false);
                        
                        while (res.next()) {
                            final String pName = res.getString("player");
                            final String server = IModule.GLOBAL_SERVER;
                            final String staff = res.getString("staff");
                            final String reason = res.getString("reason");
                            final Timestamp ban_begin = res.getTimestamp("created_at");
                            final Timestamp ban_end = res.getTimestamp("expires_at");
                            final int bhState = res.getInt("state");
                            
                            // Sometimes for unknown reason a timestamp get a wrong value (year < 1970)
                            if(ban_end != null && ban_end.getTime() < 0){
                                continue;
                            }
                            
                            // Get UUID
                            String UUID;
                            try{
                                UUID = uuidCache.get(pName);
                            }catch (final UncheckedExecutionException e) {
                                if(e.getCause() instanceof UUIDNotFoundException){
                                    continue;
                                }else{
                                    throw e;
                                }
                            }

                            // Insert the ban
                            insertBans.setString(1, UUID);
                            insertBans.setString(2, staff);
                            insertBans.setString(3, server);
                            insertBans.setTimestamp(4, ban_begin);
                            insertBans.setTimestamp(5, ban_end);
                            insertBans.setString(6, reason);
                            boolean state;
                            if(ban_end != null){
                                if(ban_end.getTime() > System.currentTimeMillis() && bhState == 0){
                                    state = true;
                                    insertBans.setTimestamp(8, null);
                                }else{
                                    state = false;
                                    insertBans.setTimestamp(8, ban_end);
                                }
                            }else{
                                state = (bhState == 0) ? true : false;
                                insertBans.setTimestamp(8, null);
                            }
                            insertBans.setBoolean(7, state);
                            insertBans.execute();
                            insertBans.clearParameters();
                            uncomittedEntries++;
                            
                            if(UUID != null){
                                initPlayerRowInBatPlayer(conn, pName, UUID);
                            }
                            if(uncomittedEntries % 100 == 0){
                                conn.commit();
                                status.incrementConvertedEntries(uncomittedEntries);
                                uncomittedEntries = 0;
                                progressionCallback.onProgress(status);
                            }
                        }

                        conn.commit();
                        status.incrementConvertedEntries(uncomittedEntries);
                        progressionCallback.done(status, null);
                    }finally{
                        if(res != null){
                            DataSourceHandler.close(res);
                        }
                    }
            }
            }
        }
        
    }
    
    public static class SQLiteMigrater extends Importer{
        @Override
        protected void importData(final ProgressCallback<ImportStatus> progressionCallback, String... additionalsArgs) throws Exception {
            ResultSet res = null;
            if(new File(BAT.getInstance().getDataFolder(), "bat_database.db").exists()){
                progressionCallback.onMinorError("The SQLite Driver must be downloaded. The server may freeze during the download.");
                if(!BAT.getInstance().loadSQLiteDriver()){
                    throw new RuntimeException("The SQLite driver can't be loaded, please check the logs.");
                }
            }else{
                throw new RuntimeException("The sqlite BAT database wasn't found ... The bat database must be named 'bat_database.db'");
            }
            
            Connection mysqlConn;
            try (Connection sqliteConn = DriverManager.getConnection("jdbc:sqlite:" + BAT.getInstance().getDataFolder().getAbsolutePath() 
                    + File.separator + "bat_database.db");){
                mysqlConn = BAT.getConnection();
                // Pattern : TableName, Entry<readInstruction, writeInstruction> 
                final Map<String, Entry<String, String>> moduleImportQueries = new HashMap<>();
                moduleImportQueries.put(SQLQueries.Ban.table, new AbstractMap.SimpleEntry<String, String>(
                        "SELECT * FROM " + SQLQueries.Ban.table + ";", 
                        "INSERT INTO " + SQLQueries.Ban.table + " VALUES(NULL,?,?,?,?,?,?,?,?,?,?,?);"));
                moduleImportQueries.put(SQLQueries.Mute.table, new AbstractMap.SimpleEntry<String, String>(
                        "SELECT * FROM " + SQLQueries.Mute.table + ";", 
                        "INSERT INTO " + SQLQueries.Mute.table + " VALUES(NULL,?,?,?,?,?,?,?,?,?,?,?);"));
                moduleImportQueries.put(SQLQueries.Comments.table, new AbstractMap.SimpleEntry<String, String>(
                        "SELECT * FROM " + SQLQueries.Comments.table + ";", 
                        "INSERT INTO " + SQLQueries.Comments.table + " VALUES(NULL,?,?,?,?,?);"));
                moduleImportQueries.put(SQLQueries.Kick.table, new AbstractMap.SimpleEntry<String, String>(
                        "SELECT * FROM " + SQLQueries.Kick.table + ";", 
                        "INSERT INTO " + SQLQueries.Kick.table + " VALUES(NULL,?,?,?,?,?);"));
                moduleImportQueries.put(SQLQueries.Core.table, new AbstractMap.SimpleEntry<String, String>(
                        "SELECT * FROM " + SQLQueries.Core.table + ";", 
                        "INSERT INTO " + SQLQueries.Core.table + " VALUES(?,?,?,?,?);"));
                
                // List tables in SQLite db
                final DatabaseMetaData dbMetadata = sqliteConn.getMetaData();
                String[] absentTables = new String[5];
                int i = 0;
                for(final String table :  Arrays.asList(SQLQueries.Kick.table, SQLQueries.Mute.table, SQLQueries.Ban.table, 
                        SQLQueries.Core.table, SQLQueries.Comments.table)){
                    final ResultSet tables = dbMetadata.getTables(null, null, table, null);
                    if (!tables.next()) {
                        absentTables[i] = table;
                    }
                    i++;
                }
                for(final String absentTable : absentTables){
                    moduleImportQueries.remove(absentTable);
                }
                
                if(!moduleImportQueries.isEmpty()){
                        // Count the number of entries (use to show the progression)
                        int entryCount = 0;
                        for(final String table : moduleImportQueries.keySet()){
                            final ResultSet resCount = sqliteConn.createStatement().executeQuery("SELECT count() FROM " + table + ";");
                            if(resCount.next()){
                                entryCount += resCount.getInt("count()");
                            }
                            resCount.close();
                        }
                        status = new ImportStatus(entryCount);
                        
                        int uncomittedEntries = 0;
                        mysqlConn.setAutoCommit(false);
                        for(final String table :  moduleImportQueries.keySet()){
                            res = sqliteConn.createStatement().executeQuery(moduleImportQueries.get(table).getKey());
                            final PreparedStatement insertStatement = 
                                    mysqlConn.prepareStatement(moduleImportQueries.get(table).getValue());
                            int columnCount = res.getMetaData().getColumnCount();
                            while (res.next()) {
                                // If there is an id, we will ignore it (start from columnIndex 2)
                                boolean ignoreFirstColumn = (moduleImportQueries.get(table).getValue().contains("NULL"))
                                        ? true : false;
                                // SOme parameters error "No value specified for parameter 1" need to find the good formula to delimite the start and the end
                                for(i=(ignoreFirstColumn) ? 2 : 1; i < (columnCount + 1); i++){
                                    Object obj = res.getObject(i);
                                    if(obj instanceof Long){
                                        obj = new Timestamp((Long) obj);
                                    }
                                    insertStatement.setObject((ignoreFirstColumn) ? i-1 : i, obj);
                                }
                                try{
                                    insertStatement.execute();
                                }catch(final SQLException exception){
                                    // If that's an duplicated entry error, we don't care we continue the import ...
                                    if(exception.getErrorCode() != 1062){
                                        throw exception;
                                    }
                                }
                                uncomittedEntries++;
                                insertStatement.clearParameters();
                                if(uncomittedEntries % 100 == 0){
                                    mysqlConn.commit();
                                    status.incrementConvertedEntries(uncomittedEntries);
                                    uncomittedEntries = 0;
                                    progressionCallback.onProgress(status);
                                }
                            }
                            res.close();
                            insertStatement.close();
                        }
        
                        mysqlConn.commit();
                        status.incrementConvertedEntries(uncomittedEntries);
                        progressionCallback.done(status, null);
                }else{
                    throw new RuntimeException("No tables of BAT were found in the bat_database.db file, therefore no data were imported ...");
                }
        }finally{
            DataSourceHandler.close(res);
            }
        }
    }
}