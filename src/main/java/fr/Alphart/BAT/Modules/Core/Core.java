package fr.Alphart.BAT.Modules.Core;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.imaginarycode.minecraft.redisbungee.RedisBungee;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.BATCommand;
import fr.Alphart.BAT.Modules.IModule;
import fr.Alphart.BAT.Modules.ModuleConfiguration;
import fr.Alphart.BAT.Utils.EnhancedDateFormat;
import fr.Alphart.BAT.Utils.UUIDNotFoundException;
import fr.Alphart.BAT.Utils.Utils;
import fr.Alphart.BAT.Utils.thirdparty.BPInterfaceFactory;
import fr.Alphart.BAT.Utils.thirdparty.BPInterfaceFactory.PermissionProvider;
import fr.Alphart.BAT.Utils.thirdparty.Metrics;
import fr.Alphart.BAT.Utils.thirdparty.Metrics.Graph;
import fr.Alphart.BAT.Utils.thirdparty.MojangAPIProvider;
import fr.Alphart.BAT.database.DataSourceHandler;
import fr.Alphart.BAT.database.SQLQueries;

public class Core implements IModule, Listener {
	private static LoadingCache<String, String> uuidCache = CacheBuilder.newBuilder()
	       .maximumSize(10000)
	       .expireAfterAccess(30, TimeUnit.MINUTES)
	       .build(
	           new CacheLoader<String, String>() {
	             public String load(final String pName) throws UUIDNotFoundException{
	               // If offline mode, no need to query the UUID just compute it
  	               if(!isOnlineMode()){
  	                 return Utils.getOfflineUUID(pName);
  	               }
	               
	            	final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
	         		if (player != null) {
	         			return player.getUniqueId().toString().replaceAll("-","");
	         		}

                    // Try to get the UUID from the BAT db
	         		PreparedStatement statement = null;
	         		ResultSet resultSet = null;
	         		String UUID = "";
	         		try (Connection conn = BAT.getConnection()) {
	         			statement = conn.prepareStatement(SQLQueries.Core.getUUID);
	         			statement.setString(1, pName);
	         			resultSet = statement.executeQuery();
	         			if (resultSet.next()) {
	         				UUID = resultSet.getString("UUID");
	         			}
	         		} catch (final SQLException e) {
	         			DataSourceHandler.handleException(e);
	         		} finally {
	         			DataSourceHandler.close(statement, resultSet);
	         		}
	         		
	         		// At last try with Mojang servers (slowest method)
	         		if(UUID.isEmpty()){
	         			UUID = MojangAPIProvider.getUUID(pName);
	         			if (UUID == null) {
	         			 throw new UUIDNotFoundException(pName);
	         			}
	         		}

	         		return UUID;
	             }
	           });
	private final String name = "core";
	private List<BATCommand> cmds;
	private Gson gson = new Gson();
	private static PermissionProvider bungeePerms;
	public static EnhancedDateFormat defaultDF = new EnhancedDateFormat(false);

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ModuleConfiguration getConfig() {
		return null;
	}

	@Override
	public boolean load() {
		// Init players table
		Statement statement = null;
		try (Connection conn = BAT.getConnection()) {
			statement = conn.createStatement();
			if (DataSourceHandler.isSQLite()) {
				for(final String coreQuery : SQLQueries.Core.SQLite.createTable){
					statement.executeUpdate(coreQuery);
				}
			} else {
				statement.executeUpdate(SQLQueries.Core.createTable);
			}
			statement.close();
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement);
		}

		// Register commands
		cmds = new ArrayList<>();
		cmds.add(new CoreCommand(this)); // Most of the job is done in the constructor of CoreCommand
		
		// Try to hook into BungeePerms
		if(ProxyServer.getInstance().getPluginManager().getPlugin("BungeePerms") != null){
			bungeePerms = BPInterfaceFactory.getBPInterface(ProxyServer.getInstance().getPluginManager().getPlugin("BungeePerms"));
		}
		
		// Update the date format (if translation has been changed)
		defaultDF = new EnhancedDateFormat(BAT.getInstance().getConfiguration().isLitteralDate());
		
        // Init metrics
        try{
            initMetrics();
        }catch(final IOException e){
            BAT.getInstance().getLogger().severe("BAT met an error while trying to connect to Metrics :");
            e.printStackTrace();
        }
		return true;
	}

	@Override
	public boolean unload() {

		return true;
	}

	@Override
	public List<BATCommand> getCommands() {
		return cmds;
	}

	@Override
	public String getMainCommand() {
		return "bat";
	}
	
	public void addCommand(final BATCommand cmd){
	    cmds.add(cmd);
	}
	
	/**
	 * Get the UUID of the specified player
	 * @param pName
	 * @throws UUIDNotFoundException
	 * @return String which is the UUID
	 */
	public static String getUUID(final String pName){
		try {
			return uuidCache.get(pName.toLowerCase());
		} catch (final Exception e) {
			if(e.getCause() instanceof UUIDNotFoundException){
				throw (UUIDNotFoundException)e.getCause();
			}
		}
		return null;
	}
	
	/**
	 * Convert an string uuid into an UUID object
	 * @param strUUID
	 * @return UUID
	 */
	public static UUID getUUIDfromString(final String strUUID){
		final String dashesUUID = strUUID.replaceFirst(
				"([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)", "$1-$2-$3-$4-$5");
		return UUID.fromString(dashesUUID);
	}

	/**
	 * Get the player name from a UUID using the BAT database
	 * @param UUID
	 * @return player name with this UUID or "unknowName"
	 */
	public static String getPlayerName(final String UUID){
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try (Connection conn = BAT.getConnection()) {
			statement = conn.prepareStatement(SQLQueries.Core.getPlayerName);
			statement.setString(1, UUID);
			resultSet = statement.executeQuery();
			if (resultSet.next()) {
				return resultSet.getString("BAT_player");
			}
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement, resultSet);
		}
		return null;
	}
	
	/**
	 * Update the IP and UUID of a player in the database
	 * 
	 * @param player
	 */
	public void updatePlayerIPandUUID(final ProxiedPlayer player) {
		PreparedStatement statement = null;
		try (Connection conn = BAT.getConnection()) {
			final String ip = Utils.getPlayerIP(player);
			final String UUID = getUUID(player.getName());
			statement = (DataSourceHandler.isSQLite()) ? conn.prepareStatement(SQLQueries.Core.SQLite.updateIPUUID)
					: conn.prepareStatement(SQLQueries.Core.updateIPUUID);
			statement.setString(1, player.getName());
			statement.setString(2, ip);
			statement.setString(3, UUID);
			statement.setString(4, (DataSourceHandler.isSQLite()) ? UUID : ip);
			if (!DataSourceHandler.isSQLite()) {
				statement.setString(5, player.getName());
			}
			statement.executeUpdate();
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement);
		}

	}

	public static String getPlayerIP(final String pName) {
	        if (BAT.getInstance().getRedis().isRedisEnabled()) {
	            try {
	            	final UUID pUUID = RedisBungee.getApi().getUuidFromName(pName, true);
	            	if (pUUID != null && RedisBungee.getApi().isPlayerOnline(pUUID))
	            	    return RedisBungee.getApi().getPlayerIp(pUUID).getHostAddress();
	            } catch (Exception exp) {
	        	exp.printStackTrace();
	            }
	        } else {
	            	final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
	            	if (player != null) return Utils.getPlayerIP(player);
	        }

		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try (Connection conn = BAT.getConnection()) {
			statement = conn.prepareStatement(SQLQueries.Core.getIP);
			statement.setString(1, getUUID(pName));
			resultSet = statement.executeQuery();
			if (resultSet.next()) {
				return resultSet.getString("lastip");
			}
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement, resultSet);
		}
		return "0.0.0.0";
	}

	/**
	 * Get the command sender permission list using bungee api or bungeeperms api if it installed
	 * @param sender
	 * @return permission in a collection of strings
	 */
	public static Collection<String> getCommandSenderPermission(final CommandSender sender){
		if(bungeePerms != null){
			if(sender.equals(ProxyServer.getInstance().getConsole())){
				return sender.getPermissions();	
			}
			try{
				return bungeePerms.getPermissions(sender);
			}catch(final NullPointerException e){
				return new ArrayList<String>();
			}
		}else{
			return sender.getPermissions();	
		}
	}
	
	public static boolean isOnlineMode(){
	  if(BAT.getInstance().getConfiguration().isForceOfflineMode()){
	    return false;
	  }
	  
	  return ProxyServer.getInstance().getConfig().isOnlineMode();
	}
	
	public void initMetrics() throws IOException{
        Metrics metrics = new Metrics(BAT.getInstance());
        final Graph locale = metrics.createGraph("Locale");
        locale.addPlotter(new Metrics.Plotter(BAT.getInstance().getConfiguration().getLocale().getLanguage()) {
            @Override
            public int getValue() {
                return 1;
            }
        });
        final Graph RDBMS = metrics.createGraph("RDBMS");
        RDBMS.addPlotter(new Metrics.Plotter("MySQL") {
            @Override
            public int getValue() {
                return !DataSourceHandler.isSQLite() ? 1 : 0;
            }
        });
        RDBMS.addPlotter(new Metrics.Plotter("SQLite") {
            @Override
            public int getValue() {
                return DataSourceHandler.isSQLite() ? 1 : 0;
            }
        });
        metrics.start();
	}
	
	// Event listener
	@EventHandler
	public void onPlayerJoin(final PostLoginEvent ev) {
		BAT.getInstance().getProxy().getScheduler().runAsync(BAT.getInstance(), new Runnable() {
			@Override
			public void run() {
				updatePlayerIPandUUID(ev.getPlayer());
			}
		});
	}

	@EventHandler
	public void onPlayerLeft(final PlayerDisconnectEvent ev) {
		CommandQueue.clearQueuedCommand(ev.getPlayer());
	}
}