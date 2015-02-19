package fr.Alphart.BAT.Modules.Core.Importer;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Arrays;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.IModule;
import fr.Alphart.BAT.Modules.Core.Importer.Importer.ImportStatus;
import fr.Alphart.BAT.Utils.CallbackUtils.ProgressCallback;
import fr.Alphart.BAT.database.DataSourceHandler;
import fr.Alphart.BAT.database.SQLQueries;

public class GeSuiteImporter extends Importer{

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