package fr.Alphart.BAT.Modules.Core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import net.alpenblock.bungeeperms.BungeePerms;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import com.google.common.base.Charsets;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import com.mojang.api.profiles.HttpProfileRepository;
import com.mojang.api.profiles.Profile;
import com.mojang.api.profiles.ProfileCriteria;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.I18n.I18n;
import fr.Alphart.BAT.Modules.BATCommand;
import fr.Alphart.BAT.Modules.IModule;
import fr.Alphart.BAT.Modules.ModuleConfiguration;
import fr.Alphart.BAT.Utils.UUIDNotFoundException;
import fr.Alphart.BAT.Utils.Utils;
import fr.Alphart.BAT.database.DataSourceHandler;
import fr.Alphart.BAT.database.SQLQueries;

public class Core implements IModule, Listener {
	private static LoadingCache<String, String> uuidCache = CacheBuilder.newBuilder()
	       .maximumSize(10000)
	       .expireAfterAccess(30, TimeUnit.MINUTES)
	       .build(
	           new CacheLoader<String, String>() {
	             public String load(final String pName) throws UUIDNotFoundException{
	            	final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
	         		if (player != null) {
	         			// Note: if it's an offline server, the UUID will be generated using
	         			// this
	         			// function java.util.UUID.nameUUIDFromBytes, however it's an
	         			// prenium or cracked account
	         			// Online server : bungee handle great the UUID
	         			return player.getUniqueId().toString().replaceAll("-","");
	         		}

	         		PreparedStatement statement = null;
	         		ResultSet resultSet = null;
	         		String UUID = "";
	         		// Try to get the UUID from the BAT db
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
	         		
	         		// If online server, retrieve the UUID from the mojang server
	         		if(UUID.isEmpty() && ProxyServer.getInstance().getConfig().isOnlineMode()){
	         			final Profile[] profiles = profileRepository.findProfilesByCriteria(new ProfileCriteria(pName, "minecraft"));

	         			if (profiles.length > 0) {
	         				UUID = profiles[0].getId();
	         			} else{
	         				throw new UUIDNotFoundException(pName);
	         			}
	         		}
	         		// If offline server, generate the UUID
	         		else if(UUID.isEmpty()){
	         			UUID = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + pName).getBytes(Charsets.UTF_8)).toString().replaceAll( "-", "" );
	         		}

	         		return UUID;
	             }
	           });
	private final String name = "core";
	private List<BATCommand> cmds;
	private static final HttpProfileRepository profileRepository = new HttpProfileRepository();
	private static BungeePerms bungeePerms;
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
		cmds = new ArrayList<BATCommand>();
		final CoreCommand cCmd = new CoreCommand(BAT.getInstance().getConfiguration().isSimpleAliases());
		cmds.add(cCmd);
		if(BAT.getInstance().getConfiguration().isSimpleAliases()){
			cmds.addAll(cCmd.getSubCmd());
		}
		
		// Try to hook into BungeePerms
		if(ProxyServer.getInstance().getPluginManager().getPlugin("BungeePerms") != null){
			bungeePerms = (BungeePerms) ProxyServer.getInstance().getPluginManager().getPlugin("BungeePerms");
		}
		
		// Update the date format (if translation has been changed)
		defaultDF = new EnhancedDateFormat(BAT.getInstance().getConfiguration().isLitteralDate());
		
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

	/**
	 * Get the UUID of the specified player
	 * @param pName
	 * @throws UUIDNotFoundException
	 * @return String which is the UUID
	 */
	@SuppressWarnings("deprecation")
	public static String getUUID(final String pName){
		try {
			return uuidCache.get(pName);
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
	
	public static HttpProfileRepository getProfileRepository() {
		return profileRepository;
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
			final String UUID = player.getUniqueId().toString().replaceAll("-","");
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
				return bungeePerms.getPermissionsManager().getUser(sender.getName()).getEffectivePerms();
			}catch(final NullPointerException e){
				return new ArrayList<String>();
			}
		}else{
			return sender.getPermissions();	
		}
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

	public static class EnhancedDateFormat{
		private final Calendar currDate = Calendar.getInstance();
		private final boolean litteralDate;
		private final DateFormat defaultDF;
		private DateFormat tdaDF;
		private DateFormat tmwDF;
		private DateFormat ydaDF;
		
		/**
		 * @param litteralDate if it's true, use tda, tmw or yda instead of the defautl date format
		 */
		public EnhancedDateFormat(final boolean litteralDate){
			this.litteralDate = litteralDate;
			final String at = I18n._("at");
			defaultDF = new SimpleDateFormat("dd-MM-yyyy '" + at + "' HH:mm");
			if(litteralDate){
				tdaDF = new SimpleDateFormat("'" + I18n._("today").replace("'", "''") + " " + at + "' HH:mm");
				tmwDF = new SimpleDateFormat("'" + I18n._("tomorrow").replace("'", "''") + " " + at + "' HH:mm");
				ydaDF = new SimpleDateFormat("'" + I18n._("yesterday").replace("'", "''") + " " + at + "' HH:mm");
			}
		}
		
		public String format(final Date date){
			if(litteralDate){
				final Calendar calDate = Calendar.getInstance();
				calDate.setTime(date);
				final int dateDoY = calDate.get(Calendar.DAY_OF_YEAR);
				final int currDoY = currDate.get(Calendar.DAY_OF_YEAR);
				
				if(calDate.get(Calendar.YEAR) == currDate.get(Calendar.YEAR)){
					if(dateDoY == currDoY){
						return tdaDF.format(date);
					}else if(dateDoY == currDoY - 1){
						return ydaDF.format(date);
					}else if(dateDoY == currDoY + 1){
						return tmwDF.format(date);
					}
				}
			}

			return defaultDF.format(date);
		}
	}
}