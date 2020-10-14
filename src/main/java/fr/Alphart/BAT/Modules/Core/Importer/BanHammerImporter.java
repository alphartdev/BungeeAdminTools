package fr.Alphart.BAT.Modules.Core.Importer;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Arrays;

import com.google.common.util.concurrent.UncheckedExecutionException;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.IModule;
import fr.Alphart.BAT.Utils.CallbackUtils.ProgressCallback;
import fr.Alphart.BAT.Utils.UUIDNotFoundException;
import fr.Alphart.BAT.database.DataSourceHandler;
import fr.Alphart.BAT.database.SQLQueries;

public class BanHammerImporter extends Importer{

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