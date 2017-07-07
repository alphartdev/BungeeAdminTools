package fr.Alphart.BAT.database;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;

import net.md_5.bungee.api.ProxyServer;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.varia.NullAppender;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;
import com.mysql.jdbc.exceptions.jdbc4.CommunicationsException;
import com.zaxxer.hikari.HikariDataSource;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.Core.Importer.Importer;
import fr.Alphart.BAT.Modules.Core.Importer.Importer.ImportStatus;
import fr.Alphart.BAT.Utils.CallbackUtils.Callback;
import fr.Alphart.BAT.Utils.CallbackUtils.ProgressCallback;

public class DataSourceHandler {
	// Connection informations
	private HikariDataSource ds;
	private String username;
	private String password;
	private String database;
	private String port;
	private String host;
	
	private static boolean sqlite = false; // If sqlite is used or not
	private Connection SQLiteConn;

	/**
	 * Constructor used for MySQL
	 * 
	 * @param host
	 * @param port
	 * @param database
	 * @param username
	 * @param password
	 * @throws SQLException 
	 */
	public DataSourceHandler(final String host, final String port, final String database, final String username, final String password, final String urlParameters) throws SQLException{
		// Check database's informations and init connection
		this.host = Preconditions.checkNotNull(host);
		this.port = Preconditions.checkNotNull(port);
		this.database = Preconditions.checkNotNull(database);
		this.username = Preconditions.checkNotNull(username);
		this.password = Preconditions.checkNotNull(password);

		BAT.getInstance().getLogger().config("Initialization of HikariCP in progress ...");
		BasicConfigurator.configure(new NullAppender());
		ds = new HikariDataSource();
		String connUrl = "jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + 
            "?useLegacyDatetimeCode=false&characterEncoding=utf8&serverTimezone=" + TimeZone.getDefault().getID();
		if(!urlParameters.isEmpty()){
		  connUrl+= "&" + urlParameters;
		}
		ds.setJdbcUrl(connUrl);
		ds.setUsername(this.username);
		ds.setPassword(this.password);
		ds.addDataSourceProperty("cachePrepStmts", "true");
		ds.setMaximumPoolSize(8);
		try {
			final Connection conn = ds.getConnection();
		    int intOffset = Calendar.getInstance().getTimeZone().getOffset(Calendar.getInstance().getTimeInMillis()) / 1000;
		    String offset = String.format("%02d:%02d", Math.abs(intOffset / 3600), Math.abs((intOffset / 60) % 60));
		    offset = (intOffset >= 0 ? "+" : "-") + offset;
			conn.createStatement().executeQuery("SET time_zone='" + offset + "';");
			conn.close();
			BAT.getInstance().getLogger().config("BoneCP is loaded !");
		} catch (final SQLException e) {
			BAT.getInstance().getLogger().severe("BAT encounters a problem during the initialization of the database connection."
					+ " Please check your logins and database configuration.");
			if(e.getCause() instanceof CommunicationsException){
			    BAT.getInstance().getLogger().severe(e.getCause().getMessage());
			}
			if(BAT.getInstance().getConfiguration().isDebugMode()){
			    BAT.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
			}
			throw e;
		}
		
		// Check for the offline mode migration. Only for MySQL has SQLite is not supported anymore
		try(Connection conn = ds.getConnection()){
	        if(OfflinemodeConverter.isMigrationRequired(conn)){
	          BAT.getInstance().getLogger().info("Hi! An old database scheme has been detected (before you updated)."
	              + "In order to work BAT will convert those datas to the database scheme.");
	          BAT.getInstance().getLogger().info("This operation may result in a loss of data. Therefore we"
	              + " strongly advise you to backup your database (with PhpMyAdmin and dump the data for example).");
	          BAT.getInstance().getLogger().severe("To proceed the migration, please create a file called 'forcemigration'"
	              + "in BAT directory and the migration will begin... Thanks for your understanding :)");
	          
	          if(new File(BAT.getInstance().getDataFolder(), "forcemigration").exists()){
	              BAT.getInstance().getLogger().info("forcemigration file detected. Migration is starting...");
    	          OfflinemodeConverter.convertToNewScheme(new ProgressCallback<Importer.ImportStatus>() {
                    @Override
                    public void done(ImportStatus result, Throwable throwable) {
                      if(throwable == null){
                        BAT.getInstance().getLogger().info("Congratulations, the migration is a success. Enjoy offline mode compatibility");
                      }else{
                        BAT.getInstance().getLogger()
                          .log(Level.SEVERE, "An error was met. Please report it on BAT thread for assistance", throwable);
                      }
                    }
                    @Override
                    public void onProgress(ImportStatus progressStatus) {
                      BAT.getInstance().getLogger().info(progressStatus.getProgressionPercent() + "% done ( "
                          + progressStatus.getTotalEntries() + " total entries to convert).");
                    }
                    @Override
                    public void onMinorError(String errorMessage) {
                      BAT.getInstance().getLogger().warning(errorMessage);
                    }
                  }, conn);
	          }
	        }
		}
		
		sqlite = false;
	}

	/**
	 * <b>NOTE: SQLite support isn't provided ANYMORE!</b>
	 * Constructor used for SQLite
	 */
	public DataSourceHandler() {
		/*
		 * As SQLite supports concurrency pretty badly (locked database which causes problem), we're gonna get a connection from the DriverManager each time
		 * we need to acces to the database. In the contrary of BoneCP with mysql in which we saved connection to optimize perfomance, it's not necessary with SQLite.
		 * FYI, here are the results of test : execute 1000 insert request using SQLite, with or without using the same connection :
		 * - Using the same connection it took : 22820 ms
		 * - Getting another connection each time (DriverManager.getConnection), it took : 24186 ms
		 * The difference is only 1366 ms for 1000 request, that means on average additional 1.3 ms, which is insignificant as we are executing almost every query async.
		 * To the people who read that, all these calculations can seem a little overrated, but I really like to improve perfomance at the most and I'm pretty curious :p
		 */
		sqlite = true;
		try {
			SQLiteConn = DriverManager.getConnection("jdbc:sqlite:" + BAT.getInstance().getDataFolder().getAbsolutePath() + File.separator
					+ "bat_database.db");
			SQLiteConn.close();
		} catch (SQLException e) {
			BAT.getInstance().getLogger().severe("BAT encounters a problem during the initialization of the sqlite database connection.");
			if(e.getMessage() != null){
				BAT.getInstance().getLogger().severe("Error message : " + e.getMessage());
			}
		}
	}

	public Connection getConnection() {
		try {
			if(sqlite){
				// To avoid concurrency problem with SQLite, we will just use one connection. Cf : constructor above for SQLite
				synchronized (SQLiteConn) {
					SQLiteConn = DriverManager.getConnection("jdbc:sqlite:" + BAT.getInstance().getDataFolder().getAbsolutePath() + File.separator
								+ "bat_database.db");
					return SQLiteConn;
				}
			}
			return ds.getConnection();
		} catch (final SQLException e) {
			BAT.getInstance().getLogger().severe(
			        "BAT can't etablish connection with the database. Please report this and include the following lines :");
			if(e.getCause() instanceof CommunicationsException){
			    BAT.getInstance().getLogger().severe(e.getCause().getMessage());
			}
            if (BAT.getInstance().getConfiguration().isDebugMode()) {
                e.printStackTrace();
            }
			return null;
		}
	}

	public boolean getSQLite() {
		return sqlite;
	}
	
	public static boolean isSQLite() {
		return sqlite;
	}

	/**
	 * Generate a backup of the BAT data in mysql database.
	 * @param path
	 * @param onComplete
	 * @throws RuntimeException if MySQL is not used or if the creation of the backup file failed
	 */
	public void generateMysqlBackup(final Callback<String> onComplete) throws RuntimeException{
		ProxyServer.getInstance().getScheduler().runAsync(BAT.getInstance(), new Runnable(){
			@Override
			public void run() {
				try {
					Process testProcess = Runtime.getRuntime().exec("mysqldump --help");
					new StreamPumper(testProcess.getErrorStream()).pump();
					new StreamPumper(testProcess.getInputStream()).pump();
					int returnValue = testProcess.waitFor();
					if(returnValue != 0){
					    throw new Exception();
					}
				} catch (final Exception e) {
					onComplete.done("The backup can't be achieved because mysqldump is nowhere to be found.", null);
					return;
				}
				final File backupDirectory = new File(BAT.getInstance().getDataFolder().getAbsolutePath() 
						+ File.separator + "databaseBackups");
				backupDirectory.mkdir();
				File backupFile = new File(backupDirectory.getAbsolutePath() + File.separator + "backup" +
						new SimpleDateFormat("dd-MMM-yyyy_HH'h'mm").format(Calendar.getInstance().getTime()) + ".sql");
				for(int i = 0;;i++){
					if(!backupFile.exists()){
						break;
					}else{
						if(i == 0){
							backupFile = new File(backupFile.getAbsolutePath().replace(".sql",  "#" + i + ".sql"));
						}
						else{
							backupFile = new File(backupFile.getAbsolutePath().replaceAll("#\\d+\\.sql$", "#" + i + ".sql"));
						}
					}
				}
				String backupCmd = "mysqldump -u {user} -p --add-drop-database -r {path} {database} {tables}";
				final String tables = Joiner.on(' ').join(Arrays.asList(SQLQueries.Ban.table, SQLQueries.Mute.table,
						SQLQueries.Kick.table, SQLQueries.Comments.table, SQLQueries.Core.table));
				String backupPath = backupFile.getAbsolutePath();
				if(backupPath.contains(" ")){
				    backupPath = "\"" + backupPath + "\"";
				}
				backupCmd = backupCmd.replace("{user}", username).replace("{database}", database)
						.replace("{path}", backupPath).replace("{tables}", tables);
				if(password.equals("")){
					backupCmd = backupCmd.replace("-p", "");
				}else{
					backupCmd = backupCmd.replace("-p", "--password=" + password);
				}
				try {
					Process backupProcess = Runtime.getRuntime().exec(backupCmd);
	                final StreamPumper errorPumper = new StreamPumper(backupProcess.getErrorStream());
	                errorPumper.pump();
	                new StreamPumper(backupProcess.getInputStream()).pump();;
					int exitValue = backupProcess.waitFor();
					if(exitValue == 0){
						final String[] splittedPath = backupFile.getAbsolutePath().split((File.separator.equals("\\") ? "\\\\" : File.separator));
						final String fileName = splittedPath[splittedPath.length - 1];
						onComplete.done(format("The backup file (%s) has been sucessfully generated.", fileName), null);
					}else{
						onComplete.done("An error happens during the creation of the mysql backup. Please check the logs", null);
						BAT.getInstance().getLogger().severe("An error happens during the creation of the mysql backup. Please report :");
						for(final String message : errorPumper.getLines()){
						    BAT.getInstance().getLogger().severe(message);
						}
					}
				} catch (final Exception e) {
					onComplete.done("An error happens during the creation of the mysql backup.", e);
					e.printStackTrace();
				}
			}
		});
	}
	
	// Useful methods
	public static String handleException(final SQLException e) {
		BAT.getInstance()
		.getLogger()
		.severe("BAT encounters a problem with the database. Please report this and include the following lines :");
		e.printStackTrace();
		return "An error related to the database occured. Please check the log.";
	}

	public static void close(final AutoCloseable... closableList) {
		for (final AutoCloseable closable : closableList) {
			if (closable != null) {
				try {
					closable.close();
				} catch (final Throwable ignored) {
				}
			}
		}
	}
	
	public class StreamPumper{
	    private final InputStreamReader reader;
	    private List<String> pumpedLines = null;
	    
	    public StreamPumper(final InputStream is){
	        reader = new InputStreamReader(is);
	    }
	    
	    /**
	     * Starts a new async task and pump the inputstream
	     */
	    public void pump(){
	        ProxyServer.getInstance().getScheduler().runAsync(BAT.getInstance(), new Runnable() {
                @Override
                public void run() {
                    try {
                        pumpedLines = CharStreams.readLines(reader);
                        reader.close();
                    } catch (final IOException e) {
                        BAT.getInstance().getLogger().severe("BAT encounter an error while reading the stream of subprocess. Please report this :");
                        e.printStackTrace();
                    }
                }
            });
	    }
	    
	    public List<String> getLines(){
	        if(pumpedLines == null){
	            return new ArrayList<String>();
	        }
	        return pumpedLines;
	    }
	}
}