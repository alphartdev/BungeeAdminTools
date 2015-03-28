package fr.Alphart.BAT.Modules.Core.Importer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.IModule;
import fr.Alphart.BAT.Utils.CallbackUtils.ProgressCallback;
import fr.Alphart.BAT.database.SQLQueries;

public class MinecraftUUIDImporter extends Importer{
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
                    if(staffBan.length() > 20){
                        staffBan = staffBan.substring(0, 20);
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