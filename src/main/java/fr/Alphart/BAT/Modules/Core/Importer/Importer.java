package fr.Alphart.BAT.Modules.Core.Importer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.ProxyServer;

import com.google.common.base.Charsets;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;

import fr.Alphart.BAT.Utils.CallbackUtils.ProgressCallback;
import fr.Alphart.BAT.Utils.MojangAPIProvider;
import fr.Alphart.BAT.Utils.UUIDNotFoundException;
import fr.Alphart.BAT.database.SQLQueries;

public abstract class Importer {
    protected Gson gson = new Gson();
    protected final LoadingCache<String, String> uuidCache = CacheBuilder.newBuilder().maximumSize(10000)
            .expireAfterAccess(30, TimeUnit.MINUTES).build(new CacheLoader<String, String>() {
                public String load(final String pName) throws UUIDNotFoundException {
                    if (ProxyServer.getInstance().getConfig().isOnlineMode()) {
                        String uuid = MojangAPIProvider.getUUID(pName);
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
        public ImportStatus getImportStatus() { return status; }
    
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
    
    public class ImportStatus{
        // The total number of entries to process (processed and remaining)
        private final int totalEntries;
            public int getTotalEntries() { return totalEntries; }
        private int convertedEntries;
            public int getConvertedEntries() { return convertedEntries; }
        
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
}