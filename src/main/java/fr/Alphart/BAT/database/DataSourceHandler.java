package fr.Alphart.BAT.database;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import com.google.common.base.Preconditions;
import com.jolbox.bonecp.BoneCPDataSource;

import fr.Alphart.BAT.BAT;


public class DataSourceHandler{
	// Connexion informations
	private final BoneCPDataSource ds;
	private static boolean sqlite = false; // If sqlite is used or not

	/**
	 * Constructor used for MySQL
	 * @param host
	 * @param port
	 * @param database
	 * @param username
	 * @param password
	 */
	public DataSourceHandler(String host, String port, String database, String username, String password){
		// Check database's informations and init connection
		host = Preconditions.checkNotNull(host);
		port = Preconditions.checkNotNull(port);
		database = Preconditions.checkNotNull(database);
		username = Preconditions.checkNotNull(username);
		password = Preconditions.checkNotNull(password);

		ds = new BoneCPDataSource();
		ds.setJdbcUrl("jdbc:mysql://" + host + "/" + database);
		ds.setUsername(username);
		ds.setPassword(password);
		ds.close();
		ds.setPartitionCount(2);
		ds.setMinConnectionsPerPartition(3);
		ds.setMaxConnectionsPerPartition(7);
		ds.setConnectionTestStatement("SELECT 1");
		try {
			ds.getConnection();
		} catch (final SQLException e) {
			handleException(e);
		}
	}

	/**
	 * Constructor used for SQLite
	 */
	public DataSourceHandler(){
		sqlite = true;
		ds = new BoneCPDataSource();
		ds.setJdbcUrl("jdbc:sqlite:" + BAT.getInstance().getDataFolder().getAbsolutePath() + File.separator + "bat_database.db");
		ds.close();
		ds.setPartitionCount(2);
		ds.setMinConnectionsPerPartition(3);
		ds.setMaxConnectionsPerPartition(7);
		ds.setConnectionTestStatement("SELECT 1");
		try {
			ds.getConnection();
		} catch (final Throwable t) {
			t.printStackTrace();
		}
	}


	public Connection getConnection(){
		try {
			return ds.getConnection();
		} catch (final SQLException e) {
			BAT.getInstance().getLogger().severe("BAT can't etablish connection with the database. Please report this and include the following lines :");
			e.printStackTrace();
			return null;
		}
	}

	public static boolean isSQLite(){
		return sqlite;
	}

	// Useful methods

	public static String handleException(final SQLException e){
		BAT.getInstance().getLogger().severe("BAT encounters a problem with the database. Please report this and include the following lines :");
		e.printStackTrace();
		return "An error related to the database occured. Please check the log.";
	}

	public static void close(final AutoCloseable... closableList) {
		for(final AutoCloseable closable : closableList){
			if (closable != null) {
				try {
					closable.close();
				} catch (final Throwable ignored) {}
			}
		}
	}
} 