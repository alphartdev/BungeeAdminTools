package fr.Alphart.BAT;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.sql.Connection;
import java.sql.SQLException;

import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import com.google.common.base.Preconditions;

import fr.Alphart.BAT.I18n.I18n;
import fr.Alphart.BAT.Modules.ModulesManager;
import fr.Alphart.BAT.Modules.Core.Core;
import fr.Alphart.BAT.database.DataSourceHandler;

//TODO: Enlever la case sensitivity

//TODO: For the note and warning features, add another table which will be named BAT_comments
// It will have 6 columns : id of the note, uuid or ip of the player which is involved in this note called entity, the note itself, statue : note or warning
// name of the staff member who wrote this and finally date

/**
 * Main class BungeeAdminTools
 * 
 * @author Alphart
 */
public class BAT extends Plugin {
	private static BAT instance;
	private static DataSourceHandler dsHandler;
	private Configuration config;
	private static String prefix;
	private ModulesManager modules;

	@Override
	public void onEnable() {
		instance = this;
		config = new Configuration();
		prefix = config.getPrefix();
		if (loadDB()) {
			modules = new ModulesManager();
			modules.loadModules();
		} else {
			getLogger().severe("BAT is gonna shutdown because it can't connect to the database.");
			return;
		}
		// Init the I18n module
		I18n.getString("GLOBAL");
	}

	@Override
	public void onDisable() {
		instance = null;
	}

	public boolean loadDB() {
		if (config.isMysql_enabled()) {
			final String username = config.getMysql_user();
			final String password = config.getMysql_password();
			final String database = config.getMysql_database();
			final String port = config.getMysql_port();
			final String host = config.getMysql_host();
			// BoneCP can accept no database and we want to avoid that
			Preconditions.checkArgument(!"".equals(database), "You must set the database.");
			dsHandler = new DataSourceHandler(host, port, database, username, password);
			final Connection c = dsHandler.getConnection();
			if (c != null) {
				try {
					c.close();
					return true;
				} catch (final SQLException e) {
					return false;
				}
			}
		}
		// If MySQL is disabled, we are gonna use SQLite
		// Before initialize the connection, we must download the sqlite driver
		// (if it isn't already in the lib folder) and load it
		else {
			if(loadSQLiteDriver()){
				dsHandler = new DataSourceHandler();
				return true;
			}
		}
		return false;
	}

	public boolean loadSQLiteDriver(){
		final File driverPath = new File(getDataFolder() + File.separator + "lib" + File.separator
				+ "sqlite_driver.jar");
		new File(getDataFolder() + File.separator + "lib").mkdir();

		// Download the driver if it doesn't exist
		if (!new File(getDataFolder() + File.separator + "lib" + File.separator + "sqlite_driver.jar").exists()) {
			getLogger().info("The SQLLite driver was not found. It is being downloaded, please wait ...");

			final String driverUrl = "http://bitbucket.org/xerial/sqlite-jdbc/downloads/sqlite-jdbc-3.7.2.jar";
			FileOutputStream fos = null;
			try {
				final ReadableByteChannel rbc = Channels.newChannel(new URL(driverUrl).openStream());
				fos = new FileOutputStream(driverPath);
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			} catch (final IOException e) {
				getLogger()
				.severe("An error occured during the downloading of the SQLite driver. Please report this error : ");
				e.printStackTrace();
				return false;
			} finally {
				DataSourceHandler.close(fos);
			}

			getLogger().info("The driver has been successfully downloaded.");
		}

		// Load the driver
		try {
			URLClassLoader systemClassLoader;
			URL u;
			Class<URLClassLoader> sysclass;
			u = driverPath.toURI().toURL();
			systemClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
			sysclass = URLClassLoader.class;
			final Method method = sysclass.getDeclaredMethod("addURL", new Class[] { URL.class });
			method.setAccessible(true);
			method.invoke(systemClassLoader, new Object[] { u });

			Class.forName("org.sqlite.JDBC");
			return true;
		} catch (final Throwable t) {
			getLogger().severe("The sqlite driver cannot be loaded. Please report this error : ");
			t.printStackTrace();
			return false;
		}
	}

	public void migrate(final String target) throws IllegalArgumentException{
		//TODO: Finish the migrate function
		Preconditions.checkArgument("mysql".equalsIgnoreCase(target) || "sqlite".equalsIgnoreCase(target));
		modules.unloadModules();

		if("mysql".equalsIgnoreCase(target))
		{
			config.setMysql_enabled(false);
			try {
				config.save();
			} catch (final InvalidConfigurationException e) {
				e.printStackTrace();
			}
			if(!loadDB()){
				throw new IllegalArgumentException("BAT can't connect to the MySQL database. Please check your login details.");
			}

			// Load and unload all modules to generate the table in the new DB
			modules.loadModules();
			modules.unloadModules();

			// Move all the data
		}
		else
		{

		}
	}

	public static BAT getInstance() {
		return BAT.instance;
	}

	public static BaseComponent[] __(final String message) {
		return TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', prefix + message));
	}

	public static void broadcast(final String message, final String perm) {
		final BaseComponent[] bsMsg = __(message);
		for (final ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
			if (p.hasPermission(perm) || p.hasPermission("bat.admin")) {
				p.sendMessage(bsMsg);
			}
			// If he has a grantall permission, he will have the broadcast on all the servers
			else{
				for(final String playerPerm : Core.getCommandSenderPermission(p)){
					if(playerPerm.startsWith("bat.grantall.")){
						p.sendMessage(bsMsg);
						break;
					}
				}
			}
		}
		getInstance().getLogger().info(ChatColor.translateAlternateColorCodes('&', message));
	}

	public ModulesManager getModules() {
		return modules;
	}

	public Configuration getConfiguration() {
		return config;
	}

	public static Connection getConnection() {
		return dsHandler.getConnection();
	}

	public DataSourceHandler getDsHandler() {
		return dsHandler;
	}

	/**
	 * Kick a player from the proxy for a specified reason
	 * 
	 * @param player
	 * @param reason
	 */
	public static void kick(final ProxiedPlayer player, final String reason) {
		if (reason == null || reason.equals("")) {
			player.disconnect(TextComponent.fromLegacyText("You have been disconnected of the server."));
		} else {
			player.disconnect(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', reason)));
		}
	}
}
