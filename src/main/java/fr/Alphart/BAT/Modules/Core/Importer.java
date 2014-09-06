package fr.Alphart.BAT.Modules.Core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;

import com.google.common.base.Charsets;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
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
    
    protected abstract void importData(final ProgressCallback<ImportStatus> progressionCallback) throws Exception;
    
    public void startImport(final ProgressCallback<ImportStatus> progressionCallback){
        try {
            importData(progressionCallback);
        }catch (final Exception e) {
            progressionCallback.done(null, e);
        }
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
        protected void importData(final ProgressCallback<ImportStatus> progressionCallback) throws Exception{
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

                    // Get UUID
                    String UUID = uuidCache.get(pName);

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

                    if(uncomittedEntries % 100 == 0){
                        conn.commit();
                        status.incrementConvertedEntries(uncomittedEntries);
                        progressionCallback.onProgress(status);
                    }
                }

                conn.commit();
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
        protected void importData(final ProgressCallback<ImportStatus> progressionCallback) throws Exception {
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

                    if(uncomittedEntries % 100 == 0){
                        status.incrementConvertedEntries(100);
                        conn.commit();
                        progressionCallback.onProgress(status);
                    }
                }

                conn.commit();
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
        protected void importData(ProgressCallback<ImportStatus> progressionCallback) throws Exception {
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
                    }catch(final RuntimeException e){
                        if(!"commentline".equals(e.getMessage())){
                            progressionCallback.onMinorError(e.getMessage());
                        }
                    }
                    if(uncomittedEntries % 100 == 0){
                        conn.commit();
                        status.incrementConvertedEntries(uncomittedEntries);
                        progressionCallback.onProgress(status);
                    }
                }
                
                conn.commit();
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
}