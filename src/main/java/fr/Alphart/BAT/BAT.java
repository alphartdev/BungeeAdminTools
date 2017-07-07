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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;

import fr.Alphart.BAT.I18n.I18n;
import fr.Alphart.BAT.Modules.ModulesManager;
import fr.Alphart.BAT.Modules.Core.Core;
import fr.Alphart.BAT.Utils.CallbackUtils.Callback;
import fr.Alphart.BAT.Utils.RedisUtils;
import fr.Alphart.BAT.Utils.Utils;
import fr.Alphart.BAT.database.DataSourceHandler;

/**
 * Main class BungeeAdminTools
 * 
 * @author Alphart
 */
public class BAT extends Plugin {
	public final static int requiredBCBuild = 878;
	
	private static BAT instance;
	private static DataSourceHandler dsHandler;
	private Configuration config;
	private static String prefix;
	private ModulesManager modules;
	private RedisUtils redis;

	@Override
	public void onEnable() {
		instance = this;
		config = new Configuration();
		prefix = config.getPrefix();
		getLogger().setLevel(Level.INFO);
		
		// Minimal version check
		if (!ProxyServer.getInstance().getName().equals("BungeeCord")) {
		  getLogger().warning("BungeeCord version check disabled because a fork has been detected."
              + " Make sur your fork is based on a BungeeCord build > #" + requiredBCBuild);
		}
		else if(Utils.getBCBuild() < requiredBCBuild && !new File(getDataFolder(), "skipversiontest").exists()){
		  getLogger().severe("Your BungeeCord build (#" + Utils.getBCBuild() + ") is not supported. Please use at least BungeeCord #" + requiredBCBuild);
		  getLogger().severe("If you want to skip that test, create a file named 'skipversiontest' in BAT directory.");
          getLogger().severe("BAT is going to shutdown ...");
          return;
		}
		
		if(config.isDebugMode()){
		    enableDebugMode();
		}
		
		loadDB(new Callback<Boolean>(){
			@Override
			public void done(final Boolean dbEnabled, Throwable throwable) {
				if (dbEnabled) {
				    getLogger().config("Connection to the database established");
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

	@Override
	public void onDisable() {
	    if(redis != null){
	      getRedis().destroy();
	    }
        modules.unloadModules();
		instance = null;
	}

	public void loadDB(final Callback<Boolean> dbState) {
		if (config.isMysql_enabled()) {
		    getLogger().config("Starting connection to the mysql database ...");
			final String username = config.getMysql_user();
			final String password = config.getMysql_password();
			final String database = config.getMysql_database();
			final String port = config.getMysql_port();
			final String host = config.getMysql_host();
			final String urlParameters = config.getMysql_urlParameters();
			// BoneCP can accept no database and we want to avoid that
			Preconditions.checkArgument(!"".equals(database), "You must set the database.");
			ProxyServer.getInstance().getScheduler().runAsync(this, new Runnable() {
				@Override
				public void run() {
				    try{
				        dsHandler = new DataSourceHandler(host, port, database, username, password, urlParameters);
	                    final Connection c = dsHandler.getConnection();
	                    if (c != null) {
                            c.close();
                            dbState.done(true, null);
                            return;
	                    }
				    }catch(final SQLException handledByDatasourceHandler){}
				    getLogger().severe("The connection pool (database connection)"
                            + " wasn't able to be launched !");
                    dbState.done(false, null);
				}
			});
		}
		// If MySQL is disabled, we are gonna use SQLite
		// Before initialize the connection, we must download the sqlite driver
		// (if it isn't already in the lib folder) and load it
		else {
		    getLogger().config("Starting connection to the sqlite database ...");
			getLogger().warning("It is strongly DISRECOMMENDED to use SQLite with BAT,"
					+ " as the SQLite implementation is less stable and much slower than the MySQL implementation.");
			if(loadSQLiteDriver()){
				dsHandler = new DataSourceHandler();
				dbState.done(true, null);
			}else{
				dbState.done(false, null);
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
	    // The IP address won't be displayed to all players (bat.broadcast.displayip)
	    final String DISPLAY_IP_BROADCAST_PERM = "bat.broadcast.displayip";
	    String ipInMessage = Utils.extractIpFromString(message);
	    boolean containsIp = !ipInMessage.isEmpty();
	    final BaseComponent[] ipIncludedMsg = __(message);
		final BaseComponent[] ipFreeMsg = containsIp ?
		    __(message.replace(ipInMessage, "<hiddenIP>")) : ipIncludedMsg;
		
		for (final ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
		    if(p.hasPermission("bat.admin")){
		      p.sendMessage(ipIncludedMsg);
		    }
		    else if (p.hasPermission(perm)) {
				p.sendMessage(p.hasPermission(DISPLAY_IP_BROADCAST_PERM) ? ipIncludedMsg : ipFreeMsg);
			}
			// If he has a grantall permission, he will have the broadcast on all the servers
			else{
				for(final String playerPerm : Core.getCommandSenderPermission(p)){
					if(playerPerm.startsWith("bat.grantall.")){
						p.sendMessage(p.hasPermission(DISPLAY_IP_BROADCAST_PERM) ? ipIncludedMsg : ipFreeMsg);
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
	
	private void enableDebugMode(){
	  try{
        final File debugFile = new File(getDataFolder(), "debug.log");
        if(debugFile.exists()){
            debugFile.delete();
        }
        // Write header into debug log
        Files.asCharSink(debugFile, Charsets.UTF_8).writeLines(Arrays.asList("BAT log debug file"
                + " - If you have an error with BAT, you should post this file on BAT topic on spigotmc",
                "Bungee build : " + ProxyServer.getInstance().getVersion(),
                "BAT version : " + getDescription().getVersion(),
                "Operating System : " + System.getProperty("os.name"),
                "Timezone : " + TimeZone.getDefault().getID(),
                "------------------------------------------------------------"));
        final FileHandler handler = new FileHandler(debugFile.getAbsolutePath(), true);
        handler.setFormatter(new Formatter() {
            private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            private final String pattern = "time [level] message\n";
            @Override
            public String format(LogRecord record) {
                return pattern.replace("level", record.getLevel().getName())
                            .replace("message", record.getMessage())
                            .replace("[BungeeAdminTools]", "")
                            .replace("time", sdf.format(Calendar.getInstance().getTime()));
            }
        });
        getLogger().addHandler(handler);
        getLogger().setLevel(Level.CONFIG);
        getLogger().info("The debug mode is now enabled ! Log are available in debug.log file located in BAT folder");
        getLogger().config("Debug mode enabled ...");
        getLogger().setUseParentHandlers(false);
    }catch(final Exception e){
        getLogger().log(Level.SEVERE, "An exception occured during the initialization of debug logging file", e);
    }
	}

}