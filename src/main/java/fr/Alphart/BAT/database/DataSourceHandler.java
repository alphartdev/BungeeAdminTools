package fr.Alphart.BAT.database;

import static java.lang.String.format;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

import net.md_5.bungee.api.ProxyServer;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.varia.NullAppender;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.jolbox.bonecp.BoneCPDataSource;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Utils.CallbackUtils.Callback;

public class DataSourceHandler {
	// Connection informations
	private BoneCPDataSource ds;
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
	 */
	public DataSourceHandler(final String host, final String port, final String database, final String username, final String password) {
		// Check database's informations and init connection
		this.host = Preconditions.checkNotNull(host);
		this.port = Preconditions.checkNotNull(port);
		this.database = Preconditions.checkNotNull(database);
		this.username = Preconditions.checkNotNull(username);
		this.password = Preconditions.checkNotNull(password);

		BasicConfigurator.configure(new NullAppender());
		ds = new BoneCPDataSource();
		ds.setJdbcUrl("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + 
				"?useLegacyDatetimeCode=false&serverTimezone=" + TimeZone.getDefault().getID());
		ds.setUsername(this.username);
		ds.setPassword(this.password);
		ds.close();
		ds.setPartitionCount(2);
		ds.setMinConnectionsPerPartition(3);
		ds.setMaxConnectionsPerPartition(7);
		ds.setConnectionTestStatement("SELECT 1");
		try {
			final Connection conn = ds.getConnection();
		    int intOffset = Calendar.getInstance().getTimeZone().getOffset(Calendar.getInstance().getTimeInMillis()) / 1000;
		    String offset = String.format("%02d:%02d", Math.abs(intOffset / 3600), Math.abs((intOffset / 60) % 60));
		    offset = (intOffset >= 0 ? "+" : "-") + offset;
			conn.createStatement().executeQuery("SET time_zone='" + offset + "';");
			conn.close();
		} catch (final SQLException e) {
			BAT.getInstance().getLogger().severe("BAT encounters a problem during the initialization of the database connection."
					+ " Please check your logins and database configuration.");
			if(e.getMessage() != null){
				BAT.getInstance().getLogger().severe("Error message : " + e.getMessage());
			}
		}
		sqlite = false;
	}

	/**
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
			BAT.getInstance()
			.getLogger()
			.severe("BAT can't etablish connection with the database. Please report this and include the following lines :");
			e.printStackTrace();
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
					Process backupProcess = Runtime.getRuntime().exec("mysqldump");
					backupProcess.waitFor();
				} catch (final Exception e) {
					onComplete.done("The backup can't be achieved because mysqldump is nowhere to be found.");
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
				String backupCmd = "mysqldump -u {user} -p --add-drop-database -r \"{path}\" {database} {tables}";
				final String tables = Joiner.on(' ').join(Arrays.asList(SQLQueries.Ban.table, SQLQueries.Mute.table,
						SQLQueries.Kick.table, SQLQueries.Comments.table, SQLQueries.Core.table));
				backupCmd = backupCmd.replace("{user}", username).replace("{database}", database)
						.replace("{path}", backupFile.getAbsolutePath()).replace("{tables}", tables);
				if(password.equals("")){
					backupCmd = backupCmd.replace("-p", "");
				}else{
					backupCmd = backupCmd.replace("-p", "-p " + password);
				}
				try {
					Process backupProcess = Runtime.getRuntime().exec(backupCmd);
					int exitValue = backupProcess.waitFor();
					if(exitValue == 0){
						final String[] splittedPath = backupFile.getAbsolutePath().split((File.separator.equals("\\") ? "\\\\" : File.separator));
						final String fileName = splittedPath[splittedPath.length - 1];
						onComplete.done(format("The backup file (%s) has been sucessfully generated.", fileName));
					}else{
						onComplete.done("An error happens during the creation of the mysql backup.");
					}
				} catch (final Exception e) {
					onComplete.done("An error happens during the creation of the mysql backup.");
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
}