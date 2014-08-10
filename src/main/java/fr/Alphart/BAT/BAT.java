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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import fr.Alphart.BAT.Utils.CallbackUtils.Callback;
import fr.Alphart.BAT.Utils.RedisUtils;
import fr.Alphart.BAT.database.DataSourceHandler;

/**
 * Main class BungeeAdminTools
 * 
 * @author Alphart
 */
public class BAT extends Plugin {
	// This way we can check at runtime if the required BC build (or a higher one) is installed 
	private final int requiredBCBuild = 878;
	private static BAT instance;
	private static DataSourceHandler dsHandler;
	private Configuration config;
	private static String prefix;
	private ModulesManager modules;
	private RedisUtils redis;

	@Override
	public void onEnable() {
		instance = this;
		if(getBCBuild() < requiredBCBuild){
			getLogger().severe("Your BungeeCord build (#" + getBCBuild() + ") is not supported. Please use at least BungeeCord #" + requiredBCBuild);
			getLogger().severe("BAT is going to shutdown ...");
			return;
		}
		config = new Configuration();
		prefix = config.getPrefix();
		loadDB(new Callback<Boolean>(){
			@Override
			public void done(final Boolean dbState) {
				if (dbState) {
					// Try enabling redis support.
					redis = new RedisUtils(config.isRedisSupport());
			        modules = new ModulesManager();
					modules.loadModules();
				} else {
					getLogger().severe("BAT is gonna shutdown because it can't connect to the database.");
					return;
				}
				// Init the I18n module
				I18n.getString("global");
			}
		});
	}
	
	public int getBCBuild(){
		final Pattern p = Pattern.compile(".*?:(.*?:){3}(\\d*)");
		final Matcher m = p.matcher(ProxyServer.getInstance().getVersion());
		int BCBuild;
		try{
			if (m.find()) {
			    BCBuild = Integer.parseInt(m.group(2));
			}else{
				throw new NumberFormatException();
			}
		}catch(final NumberFormatException e){
			// We can't determine BC build, just display a message, and set the build so it doesn't trigger the security
			getLogger().info("BC build can't be detected. If you encounter any problems, please report that message. Otherwise don't take into account");
			BCBuild = requiredBCBuild;
		}
		return BCBuild;
	}

	@Override
	public void onDisable() {
	        getRedis().destroy();
		instance = null;
	}

	public void loadDB(final Callback<Boolean> dbState) {
		if (config.isMysql_enabled()) {
			final String username = config.getMysql_user();
			final String password = config.getMysql_password();
			final String database = config.getMysql_database();
			final String port = config.getMysql_port();
			final String host = config.getMysql_host();
			// BoneCP can accept no database and we want to avoid that
			Preconditions.checkArgument(!"".equals(database), "You must set the database.");
			ProxyServer.getInstance().getScheduler().runAsync(this, new Runnable() {
				@Override
				public void run() {
					dsHandler = new DataSourceHandler(host, port, database, username, password);
					final Connection c = dsHandler.getConnection();
					if (c != null) {
						try {
							c.createStatement().executeQuery("SELECT 1;");
							c.close();
							dbState.done(true);
						} catch (final SQLException e) {
							dbState.done(false);
						}
					}
				}
			});
			return;
		}
		// If MySQL is disabled, we are gonna use SQLite
		// Before initialize the connection, we must download the sqlite driver
		// (if it isn't already in the lib folder) and load it
		else {
			getLogger().warning("It is strongly DISRECOMMENDED to use SQLite with BAT,"
					+ " as the SQLite implementation is less stable and much slower than the MySQL implementation.");
			if(loadSQLiteDriver()){
				dsHandler = new DataSourceHandler();
				dbState.done(true);
			}else{
				dbState.done(false);
			}
		}
	}

	public boolean loadSQLiteDriver(){
		final File driverPath = new File(getDataFolder() + File.separator + "lib" + File.separator
				+ "sqlite_driver.jar");
		new File(getDataFolder() + File.separator + "lib").mkdir();

		// Download the driver if it doesn't exist
		if (!new File(getDataFolder() + File.separator + "lib" + File.separator + "sqlite_driver.jar").exists()) {
			getLogger().info("The SQLLite driver was not found. It is being downloaded, please wait ...");

			final String driverUrl = "https://www.dropbox.com/s/ls7qoddx9m6t4vh/sqlite_driver.jar?dl=1";
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
//			if(!loadDB()){
//				throw new IllegalArgumentException("BAT can't connect to the MySQL database. Please check your login details.");
//			}

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

	/**
	 * Send a broadcast message to everyone with the given perm <br>
	 * Also broadcast through Redis if it's installed that's why this method <strong>should not be called
	 * from a Redis call</strong> otherwise it will broadcast it again and again
	 * @param message
	 * @param perm
	 */
	public static void broadcast(final String message, final String perm) {
		noRedisBroadcast(message, perm);
		if(BAT.getInstance().getRedis().isRedisEnabled()){
			BAT.getInstance().getRedis().sendBroadcast(perm, message);
		}
	}
	
	public static void noRedisBroadcast(final String message, final String perm) {
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

	public RedisUtils getRedis() {
	    return redis;
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