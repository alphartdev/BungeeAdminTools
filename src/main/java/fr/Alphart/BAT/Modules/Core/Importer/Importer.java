package fr.Alphart.BAT.Modules.Core.Importer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import lombok.Getter;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;

import fr.Alphart.BAT.Modules.Core.Core;
import fr.Alphart.BAT.Utils.CallbackUtils.ProgressCallback;
import fr.Alphart.BAT.Utils.UUIDNotFoundException;
import fr.Alphart.BAT.Utils.Utils;
import fr.Alphart.BAT.Utils.thirdparty.MojangAPIProvider;
import fr.Alphart.BAT.database.SQLQueries;

public abstract class Importer {
    protected Gson gson = new Gson();
    protected final LoadingCache<String, String> uuidCache = CacheBuilder.newBuilder().maximumSize(10000)
            .expireAfterAccess(30, TimeUnit.MINUTES).build(new CacheLoader<String, String>() {
                public String load(final String pName) throws UUIDNotFoundException {
                    if (Core.isOnlineMode()) {
                        String uuid = MojangAPIProvider.getUUID(pName);
                        if (uuid != null) {
                            return uuid;
                        } else {
                            throw new UUIDNotFoundException(pName);
                        }
                    } else {
                        return Utils.getOfflineUUID(pName);
                    }

                }
            });
    protected ImportStatus status;
    
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
    public static class ImportStatus{
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
        
        public void setDone(){
          convertedEntries = totalEntries;
        }
        
        public double getProgressionPercent(){
          double percent = (((double)convertedEntries / (double)totalEntries) * 100);  
          return Math.round(percent * 100) / (double)100;// round to 2 decimal places
        }
        
        public int getRemainingEntries(){
            return totalEntries - convertedEntries;
        }
    }
}