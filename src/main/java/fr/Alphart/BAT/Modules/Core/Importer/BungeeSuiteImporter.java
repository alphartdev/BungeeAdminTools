package fr.Alphart.BAT.Modules.Core.Importer;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Arrays;

import com.google.common.util.concurrent.UncheckedExecutionException;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.IModule;
import fr.Alphart.BAT.Modules.Core.Importer.Importer.ImportStatus;
import fr.Alphart.BAT.Utils.UUIDNotFoundException;
import fr.Alphart.BAT.Utils.CallbackUtils.ProgressCallback;
import fr.Alphart.BAT.database.DataSourceHandler;
import fr.Alphart.BAT.database.SQLQueries;

public class BungeeSuiteImporter extends Importer{

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