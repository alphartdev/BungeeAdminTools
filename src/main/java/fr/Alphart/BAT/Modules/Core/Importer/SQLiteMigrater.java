package fr.Alphart.BAT.Modules.Core.Importer;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.Core.Importer.Importer.ImportStatus;
import fr.Alphart.BAT.Utils.CallbackUtils.ProgressCallback;
import fr.Alphart.BAT.database.DataSourceHandler;
import fr.Alphart.BAT.database.SQLQueries;

public class SQLiteMigrater extends Importer{
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