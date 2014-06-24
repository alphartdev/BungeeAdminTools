package fr.Alphart.BAT.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.TimeZone;

import com.google.common.base.Preconditions;
import com.jolbox.bonecp.BoneCPDataSource;

import fr.Alphart.BAT.BAT;

public class DataSourceHandler {
	// Connexion informations
	private BoneCPDataSource ds;
	private boolean sqlite = false; // If sqlite is used or not
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

	public DataSourceHandler(String host, String port, String database, String username, String password) {
		// Check database's informations and init connection
		host = Preconditions.checkNotNull(host);
		port = Preconditions.checkNotNull(port);
		database = Preconditions.checkNotNull(database);
		username = Preconditions.checkNotNull(username);
		password = Preconditions.checkNotNull(password);

		ds = new BoneCPDataSource();
		ds.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useLegacyDatetimeCode=false&serverTimezone=" + TimeZone.getDefault().getID());
		ds.setUsername(username);
		ds.setPassword(password);
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
		return BAT.getInstance().getDsHandler().getSQLite();
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