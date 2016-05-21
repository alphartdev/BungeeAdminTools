package fr.Alphart.BAT.Modules.Watch;

import static fr.Alphart.BAT.I18n.I18n._;
import static fr.Alphart.BAT.I18n.I18n.__;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import net.cubespace.Yamler.Config.Comment;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.BATCommand;
import fr.Alphart.BAT.Modules.BATCommand.RunAsync;
import fr.Alphart.BAT.Modules.CommandHandler;
import fr.Alphart.BAT.Modules.IModule;
import fr.Alphart.BAT.Modules.ModuleConfiguration;
import fr.Alphart.BAT.Modules.Core.Core;
import fr.Alphart.BAT.Modules.Core.PermissionManager;
import fr.Alphart.BAT.Modules.Core.PermissionManager.Action;
import fr.Alphart.BAT.Utils.FormatUtils;
import fr.Alphart.BAT.Utils.UUIDNotFoundException;
import fr.Alphart.BAT.Utils.Utils;
import fr.Alphart.BAT.database.DataSourceHandler;
import fr.Alphart.BAT.database.SQLQueries;

/**
 * This module handles all the watch list.<br>
 * The watch status of online players are <b>cached</b> in order to avoid lag.
 */
public class Watch implements IModule, Listener {
	private final String name = "watch";
	private ConcurrentHashMap<String, PlayerWatchData> watchedPlayers;
	private CommandHandler commandHandler;
	private ScheduledTask task;
	private final WatchConfig config;

	public Watch() {
		config = new WatchConfig();
	}

	@Override
	public List<BATCommand> getCommands() {
		return commandHandler.getCmds();
	}

	@Override
	public String getMainCommand() {
		return "watch";
	}

	@Override
	public ModuleConfiguration getConfig() {
		return config;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean load() {
		// Init table
		Statement statement = null;
		try (Connection conn = BAT.getConnection()) {
			statement = conn.createStatement();
			if (DataSourceHandler.isSQLite()) {
				for (final String query : SQLQueries.Watch.SQLite.createTable) {
					statement.executeUpdate(query);
				}
			} else {
				statement.executeUpdate(SQLQueries.Watch.createTable);
			}
			statement.close();
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement);
		}

		// Register commands
		commandHandler = new WatchCommand(this);
		commandHandler.loadCmds();

		watchedPlayers = new ConcurrentHashMap<String, PlayerWatchData>();

		final WatchTask watchTask = new WatchTask(this);
		task = ProxyServer.getInstance().getScheduler().schedule(BAT.getInstance(), watchTask, 0, 10, TimeUnit.SECONDS);
		return true;
	}

	@Override
	public boolean unload() {
		task.cancel();
		watchedPlayers.clear();
		return true;
	}

	public class WatchConfig extends ModuleConfiguration {
		public WatchConfig() {
			init(name);
		}
		
		@Comment("Forbidden commands when a player is watched")
		@Getter
		private List<String> forbiddenCmds = new ArrayList<String>(){
			private static final long serialVersionUID = 1L;

		{   // not currently forbidding any commands to watched players
		    //	add("msg");
		}};
	}

	public void loadWatchMessage(final String pName, final String server){
		if(!watchedPlayers.containsKey(pName)){
			return;
		}
		String reason = "";
		Timestamp expiration = null;
		Timestamp begin = null;
		String staff = null;
		
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try (Connection conn = BAT.getConnection()) {
			statement = conn.prepareStatement(DataSourceHandler.isSQLite()
					? SQLQueries.Watch.SQLite.getWatchMessage
					: SQLQueries.Watch.getWatchMessage);
			try{
				statement.setString(1, Core.getUUID(pName));
				statement.setString(2, Core.getPlayerIP(pName));
				statement.setString(3, server);
			}catch(final UUIDNotFoundException e){
				BAT.getInstance().getLogger().severe("Error during retrieving of the UUID of " + pName + ". Please report this error :");
				e.printStackTrace();
			}
			resultSet = statement.executeQuery();
	
			if(resultSet.next()) {
				if(DataSourceHandler.isSQLite()){
					begin = new Timestamp(resultSet.getLong("strftime('%s',watch_begin)") * 1000);
					String endStr = resultSet.getString("watch_end");
					expiration = (endStr == null) ? null : new Timestamp(Long.parseLong(endStr));
				}else{
					begin = resultSet.getTimestamp("watch_begin");
					expiration = resultSet.getTimestamp("watch_end");
				}
				reason = (resultSet.getString("watch_reason") != null) ? resultSet.getString("watch_reason") : IModule.NO_REASON;
				staff = resultSet.getString("watch_staff");
			}else{
				throw new SQLException("No active watch found.");
			}
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement, resultSet);
		}
		if(expiration != null){
			watchedPlayers.get(pName).setWatchMessage(_("isWatchedTemp", 
					new String[]{ reason , "{expiration}", Core.defaultDF.format(begin), staff }), expiration);
		}else{
			watchedPlayers.get(pName).setWatchMessage(_("isWatched", 
					new String[]{ reason, Core.defaultDF.format(begin), staff }), null);
		}

	}
	
	/**
	 * Check if both ip and name of this player are watched<br>
	 * Use <b>cached data</b>
	 * 
	 * @param player
	 * @param server
	 * @return <ul>
	 *         <li>1 if the player is watched from this server</li>
	 *         <li>0 if he's not watched from this server</li>
	 *         <li>-1 if the data are loading</li>
	 *         </ul>
	 */
	public int isWatched(final ProxiedPlayer player, final String server) {
		final PlayerWatchData pWatchData = watchedPlayers.get(player.getName());
		if (pWatchData != null) {
			if (pWatchData.isWatched(server)) {
				return 1;
			}
			return 0;
		}

		return -1;
	}

	/**
	 * Check if this entity (player or ip) is watched<br>
	 * <b>Use uncached data. Use {@link #isWatched(ProxiedPlayer, String)} instead
	 * of this method if the player is available</b>
	 * 
	 * @param watchdEntity
	 *            | can be an ip or a player name
	 * @param server
	 *            | if server equals to (any) check if the player is watch on a
	 *            server
	 * @param forceUseUncachedData
	 *            | use uncached data, for example to handle player or player's
	 *            ip related watch
	 * @return
	 */
	public boolean isWatched(final String watchdEntity, final String server, final boolean forceUseUncachedData) {
		// Check if the entity is an online player, in this case we're going to
		// use the cached method
		final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(watchdEntity);
		if (!forceUseUncachedData && player != null) {
			final int result = isWatched(player, server);
			// If the data aren't loading
			if (result != -1) {
				return (result == 1);
			}
		}

		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try (Connection conn = BAT.getConnection()) {
			// If this is an ip which may be watchd
			if (Utils.validIP(watchdEntity)) {
				final String ip = watchdEntity;
				statement = conn.prepareStatement((ANY_SERVER.equals(server)) ? SQLQueries.Watch.isWatchedIP
						: SQLQueries.Watch.isWatchedServerIP);
				statement.setString(1, ip);
				if (!ANY_SERVER.equals(server)) {
					statement.setString(2, server);
				}
			}
			// If this is a player which may be watchd
			else {
				final String pName = watchdEntity;
				statement = conn.prepareStatement((ANY_SERVER.equals(server)) ? SQLQueries.Watch.isWatched
						: SQLQueries.Watch.isWatchedServer);
				statement.setString(1, Core.getUUID(pName));
				if (!ANY_SERVER.equals(server)) {
					statement.setString(2, server);
				}
			}
			resultSet = statement.executeQuery();

			// If there are a result
			if (resultSet.next()) {
				return true;
			}

		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement, resultSet);
		}
		return false;
	}

	/**
	 * Watch this entity (player or ip) <br>
	 * 
	 * @param watchdEntity
	 *            | can be an ip or a player name
	 * @param server
	 *            ; set to "(global)", to global watch
	 * @param staff
	 * @param expirationTimestamp
	 *            ; set to 0 for watch def
	 * @param reason
	 *            | optional
	 * @return
	 */
	public String watch(final String watchdEntity, final String server, final String staff,
			final long expirationTimestamp, final String reason) {
		PreparedStatement statement = null;
		try (Connection conn = BAT.getConnection()) {
			if (Utils.validIP(watchdEntity)) {
				final String ip = watchdEntity;
				statement = conn.prepareStatement(SQLQueries.Watch.createWatchIP);
				statement.setString(1, ip);
				statement.setString(2, staff);
				statement.setString(3, server);
				statement.setTimestamp(4, (expirationTimestamp > 0) ? new Timestamp(expirationTimestamp) : null);
				statement.setString(5, (NO_REASON.equals(reason)) ? null : reason);
				statement.executeUpdate();
				statement.close();

				if (BAT.getInstance().getRedis().isRedisEnabled()) {
				    	for (UUID pUUID : RedisBungee.getApi().getPlayersOnline()) {
				    	    	if (RedisBungee.getApi().getPlayerIp(pUUID).equals(ip)) {
				    	    	    	// The watch task timer will add the player to the bungeecord instance's cache if needed.
				    	    	    	if(server.equals(GLOBAL_SERVER) || RedisBungee.getApi().getServerFor(pUUID).getName().equalsIgnoreCase(server)) {
				    	    	    	    	ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pUUID);
				    	    	    	}
				    	    	}
				    	}
				} else {
				    	for (final ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
						if (Utils.getPlayerIP(player).equals(ip)) {
							if (server.equals(GLOBAL_SERVER)) {
								watchedPlayers.get(player.getName()).setGlobal();
							} else {
								watchedPlayers.get(player.getName()).addServer(server);
							}
						}
					}
				}

				if (expirationTimestamp > 0) {
					return _("watchTempBroadcast", new String[] { ip, FormatUtils.getDuration(expirationTimestamp),
							staff, server, reason });
				} else {
					return _("watchBroadcast", new String[] { ip, staff, server, reason });
				}
			}

			// Otherwise it's a player
			else {
			    	final String pName = watchdEntity;
			    	final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
			    	statement = conn.prepareStatement(SQLQueries.Watch.createWatch);
		    	    	statement.setString(1, Core.getUUID(pName));
		    	    	statement.setString(2, staff);
		    	    	statement.setString(3, server);
		    	    	statement.setTimestamp(4, (expirationTimestamp > 0) ? new Timestamp(expirationTimestamp) : null);
		    	    	statement.setString(5, (NO_REASON.equals(reason)) ? null : reason);
		    	    	statement.executeUpdate();
		    	    	statement.close();
			    
			    	if (player != null) {
						updateWatchData(player.getName());
					} else if (BAT.getInstance().getRedis().isRedisEnabled()) {
						//Need to implement a function to get an UUID object instead of a string one.
						final UUID pUUID = Core.getUUIDfromString(Core.getUUID(pName));
						BAT.getInstance().getRedis().sendWatchUpdatePlayer(pUUID, server);
					}
			    	if (expirationTimestamp > 0) {
						return _("watchTempBroadcast", new String[] { pName, FormatUtils.getDuration(expirationTimestamp),
							staff, server, reason });
					} else {
						return _("watchBroadcast", new String[] { pName, staff, server, reason });
					}

			}
		} catch (final SQLException e) {
			return DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement);
		}
	}

	/**
	 * Watch the ip of an online player
	 * 
	 * @param server
	 *            ; set to "(global)", to global watch
	 * @param staff
	 * @param duration
	 *            ; set to 0 for watch def
	 * @param reason
	 *            | optional
	 * @param ip
	 */
	public String watchIP(final ProxiedPlayer player, final String server, final String staff,
			final long expirationTimestamp, final String reason) {
		watch(Utils.getPlayerIP(player), server, staff, expirationTimestamp, reason);
		return _("watchBroadcast", new String[] { player.getName() + "'s IP", staff, server, reason });
	}

	/**
	 * Unwatch an entity (player or ip)
	 * 
	 * @param watchdEntity
	 *            | can be an ip or a player name
	 * @param server
	 *            | if equals to (any), unwatch from all servers | if equals to
	 *            (global), remove global watch
	 * @param staff
	 * @param reason
	 * @param unWatchIP
	 */
	public String unWatch(final String watchdEntity, final String server, final String staff, final String reason) {
		PreparedStatement statement = null;
		try (Connection conn = BAT.getConnection()) {
			// If the watchdEntity is an ip
			if (Utils.validIP(watchdEntity)) {
				final String ip = watchdEntity;
				if (ANY_SERVER.equals(server)) {
					statement = (DataSourceHandler.isSQLite()) ? conn.prepareStatement(SQLQueries.Watch.SQLite.unWatchIP)
							: conn.prepareStatement(SQLQueries.Watch.unWatchIP);
					statement.setString(1, reason);
					statement.setString(2, staff);
					statement.setString(3, ip);
				} else {
					statement = (DataSourceHandler.isSQLite()) ? conn
							.prepareStatement(SQLQueries.Watch.SQLite.unWatchIPServer) : conn
							.prepareStatement(SQLQueries.Watch.unWatchIPServer);
					statement.setString(1, reason);
					statement.setString(2, staff);
					statement.setString(3, ip);
					statement.setString(4, server);
				}
				statement.executeUpdate();
				statement.close();

				return _("unWatchBroadcast", new String[] { ip, staff, server, reason });
			}

			// Otherwise it's a player
			else {
				final String pName = watchdEntity;
				if (ANY_SERVER.equals(server)) {
					statement = (DataSourceHandler.isSQLite()) ? conn.prepareStatement(SQLQueries.Watch.SQLite.unWatch)
							: conn.prepareStatement(SQLQueries.Watch.unWatch);
					statement.setString(1, reason);
					statement.setString(2, staff);
					statement.setString(3, Core.getUUID(pName));
				} else {
					statement = (DataSourceHandler.isSQLite()) ? conn
							.prepareStatement(SQLQueries.Watch.SQLite.unWatchServer) : conn
							.prepareStatement(SQLQueries.Watch.unWatchServer);
					statement.setString(1, reason);
					statement.setString(2, staff);
					statement.setString(3, Core.getUUID(pName));
					statement.setString(4, server);
				}
				statement.executeUpdate();
				statement.close();

				final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
				if (player != null) {
					updateWatchData(player.getName());
						if(ANY_SERVER.equals(server) || GLOBAL_SERVER.equals(server) || player.getServer().getInfo().getName().equalsIgnoreCase(server)){
					//		player.sendMessage(__("wasUnwatchedNotif", new String[] { reason }));
					}
				} else if (BAT.getInstance().getRedis().isRedisEnabled()) {
						final UUID pUUID = Core.getUUIDfromString(Core.getUUID(pName));
				    	ServerInfo pServer = RedisBungee.getApi().getServerFor(pUUID);
				    	if (ANY_SERVER.equals(server) || GLOBAL_SERVER.equals(server) || (pServer != null && pServer.getName().equalsIgnoreCase(server))){
				    	}
				}
				return _("unWatchBroadcast", new String[] { pName, staff, server, reason });
			}
		} catch (final SQLException e) {
			return DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement);
		}
	}

	/**
	 * Unwatch the ip of this entity
	 * 
	 * @param entity
	 * @param server
	 *            | if equals to (any), unwatch from all servers | if equals to
	 *            (global), remove global watch
	 * @param staff
	 * @param reason
	 *            | optional
	 * @param duration
	 *            ; set to 0 for watch def
	 */
	public String unWatchIP(final String entity, final String server, final String staff, final String reason) {
		if (Utils.validIP(entity)) {
			return unWatch(entity, server, staff, reason);
		} else {
			unWatch(Core.getPlayerIP(entity), server, staff, reason);
			updateWatchData(entity);
			return _("unWatchBroadcast", new String[] { entity + "'s IP", staff, server, reason });
		}
	}

	/**
	 * Get all watch data of an entity <br>
	 * <b>Should be run async to optimize performance</b>
	 * 
	 * @param entity
	 *            | can be an ip or a player name
	 * @return List of WatchEntry of the entity
	 */
	public List<WatchEntry> getWatchData(final String entity) {
		final List<WatchEntry> watchList = new ArrayList<WatchEntry>();
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try (Connection conn = BAT.getConnection()) {
			// If the entity is an ip
			if (Utils.validIP(entity)) {
				statement = conn.prepareStatement((DataSourceHandler.isSQLite())
						? SQLQueries.Watch.SQLite.getWatchIP
						: SQLQueries.Watch.getWatchIP);
				statement.setString(1, entity);
				resultSet = statement.executeQuery();
			}
			// Otherwise if it's a player
			else {
				statement = conn.prepareStatement((DataSourceHandler.isSQLite())
						? SQLQueries.Watch.SQLite.getWatch
						: SQLQueries.Watch.getWatch);
				statement.setString(1, Core.getUUID(entity));
				resultSet = statement.executeQuery();
			}
			
			while (resultSet.next()) {
				final Timestamp beginDate;
				final Timestamp endDate;
				final Timestamp unwatchDate;
				if(DataSourceHandler.isSQLite()){
					beginDate = new Timestamp(resultSet.getLong("strftime('%s',watch_begin)") * 1000);
					final String endStr = resultSet.getString("watch_end");
					endDate = (endStr == null) ? null : new Timestamp(Long.parseLong(endStr));
					final long unbanLong = resultSet.getLong("strftime('%s',watch_unwatchdate)") * 1000;
					unwatchDate = (unbanLong == 0) ? null : new Timestamp(unbanLong);
				}else{
					beginDate = resultSet.getTimestamp("watch_begin");
					endDate = resultSet.getTimestamp("watch_end");
					unwatchDate = resultSet.getTimestamp("watch_unwatchdate");
				}
				
				final String server = resultSet.getString("watch_server");
				String reason = resultSet.getString("watch_reason");
				if(reason == null){
					reason = NO_REASON;
				}
				final String staff = resultSet.getString("watch_staff");
				final boolean active = (resultSet.getBoolean("watch_state") ? true : false);
				String unwatchReason = resultSet.getString("watch_unwatchreason");
				if(unwatchReason == null){
					unwatchReason = NO_REASON;
				}
				final String unwatchStaff = resultSet.getString("watch_unwatchstaff");
				watchList.add(new WatchEntry(entity, server, reason, staff, beginDate, endDate, unwatchDate, unwatchReason, unwatchStaff, active));
			}
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement, resultSet);
		}
		return watchList;
	}

	/**
	 * This class is used to cache the watch data of a player.
	 */
	public static class PlayerWatchData {
		private final String pName;
		private final List<String> servers;
		private boolean globalWatch = false;
		private Map.Entry<String, Timestamp> watchMessage;

		public PlayerWatchData(final String pName, final List<String> servers) {
			this.pName = pName;
			// Override the arraylist implementation to make used methods non-case sensitive
			this.servers = new ArrayList<String>(){
			    @Override
			    public void add(int index, String element) {
			        super.add(index, element.toLowerCase());
			    }
			    @Override
			    public boolean add(String e) {
			        return super.add(e.toLowerCase());
			    }
			    @Override
			    public boolean contains(Object o) {
			        if(o instanceof String){
			            return super.contains(((String)o).toLowerCase());
			        }
			        return super.contains(o);
			    }
			};
			for(final String server : servers){
			    servers.add(server);
			}
		}

		public void setGlobal() {
			globalWatch = true;
		}

		public void unsetGlobal() {
			globalWatch = false;
		}

		public void addServer(final String server) {
			if (!servers.contains(server)) {
				servers.add(server);
			}
		}

		public void removeServer(final String server) {
			servers.remove(server);
		}

		public void clearServers() {
			servers.clear();
		}

		public boolean isWatched(final String server) {
			if (globalWatch) {
				return true;
			}else if( (ANY_SERVER.equals(server) && !servers.isEmpty()) ){
				return true;
			}else if(servers.contains(server)){
				return true;
			}
			return false;
		}
	
		public BaseComponent[] getWatchMessage(final Watch module){
			if(watchMessage != null){
				if(watchMessage.getValue() != null){
					if(watchMessage.getValue().getTime() >= System.currentTimeMillis()){
						return BAT.__(watchMessage.getKey().replace("{expiration}", FormatUtils.getDuration(watchMessage.getValue().getTime())));
					}
					// If it's not synchronized with the db, force the update of watch data
					else{
						Statement statement = null;
						try (Connection conn = BAT.getConnection()) {
							statement = conn.createStatement();
							if (DataSourceHandler.isSQLite()) {
								statement.executeUpdate(SQLQueries.Watch.SQLite.updateExpiredWatch);
							} else {
								statement.executeUpdate(SQLQueries.Watch.updateExpiredWatch);
							}
						} catch (final SQLException e) {
							DataSourceHandler.handleException(e);
						} finally {
							DataSourceHandler.close(statement);
						}
						module.updateWatchData(pName);
					}
				}
				else{
					return BAT.__(watchMessage.getKey());
				}
			}
			return __("wasUnwatchedNotif", new String[]{ NO_REASON });
		}
		
		public void setWatchMessage(final String messagePattern, final Timestamp expiration){
			watchMessage = new AbstractMap.SimpleEntry<String, Timestamp>(messagePattern, expiration);
		}
	}

	public void updateWatchData(final String pName) {
		final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
		if (player == null) {
			return;
		}
		PlayerWatchData pWatchData;
		if (watchedPlayers.containsKey(pName)) {
			pWatchData = watchedPlayers.get(pName);
			pWatchData.clearServers();
			pWatchData.unsetGlobal();
		} else {
			pWatchData = new PlayerWatchData(pName, new ArrayList<String>());
		}
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try (Connection conn = BAT.getConnection()) {
			statement = conn.prepareStatement("SELECT watch_server FROM `BAT_watch` WHERE UUID = ? AND watch_state = 1;");
			statement.setString(1, Core.getUUID(pName));
			resultSet = statement.executeQuery();
			while (resultSet.next()) {
				final String server = resultSet.getString("watch_server");
				if (GLOBAL_SERVER.equals(server)) {
					pWatchData.setGlobal();
				} else {
					pWatchData.addServer(server);
				}
			}
			resultSet.close();
			statement.close();

			statement = conn
					.prepareStatement("SELECT watch_server FROM `BAT_watch` WHERE watch_ip = ? AND watch_state = 1;");
			statement.setString(1, Core.getPlayerIP(pName));
			resultSet = statement.executeQuery();
			while (resultSet.next()) {
				final String server = resultSet.getString("watch_server");
				if (GLOBAL_SERVER.equals(server)) {
					pWatchData.setGlobal();
				} else {
					pWatchData.addServer(server);
				}
			}
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement, resultSet);
		}
		watchedPlayers.put(pName, pWatchData);
		if(pWatchData.isWatched(GLOBAL_SERVER)){
			loadWatchMessage(pName, GLOBAL_SERVER);
		}else if(player.getServer() != null && pWatchData.isWatched(player.getServer().getInfo().getName())){
			loadWatchMessage(pName, player.getServer().getInfo().getName());
		}
	}
	
	public List<WatchEntry> getManagedWatch(final String staff){
		final List<WatchEntry> watchList = new ArrayList<WatchEntry>();
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try (Connection conn = BAT.getConnection()) {
			statement = conn.prepareStatement((DataSourceHandler.isSQLite())
					? SQLQueries.Watch.SQLite.getManagedWatch
					: SQLQueries.Watch.getManagedWatch);
			statement.setString(1, staff);
			statement.setString(2, staff);
			resultSet = statement.executeQuery();
			
			while (resultSet.next()) {
				final Timestamp beginDate;
				final Timestamp endDate;
				final Timestamp unwatchDate;
				if(DataSourceHandler.isSQLite()){
					beginDate = new Timestamp(resultSet.getLong("strftime('%s',watch_begin)") * 1000);
					String endStr = resultSet.getString("watch_end");
					endDate = (endStr == null) ? null : new Timestamp(Long.parseLong(endStr));
					long unwatchLong = resultSet.getLong("strftime('%s',watch_unwatchdate)") * 1000;
					unwatchDate = (unwatchLong == 0) ? null : new Timestamp(unwatchLong);
				}else{
					beginDate = resultSet.getTimestamp("watch_begin");
					endDate = resultSet.getTimestamp("watch_end");
					unwatchDate = resultSet.getTimestamp("watch_unwatchdate");
				}

				
				// Make it compatible with sqlite (date: get an int with the sfrt and then construct a tiemstamp)
				final String server = resultSet.getString("watch_server");
				String reason = resultSet.getString("watch_reason");
				if(reason == null){
					reason = NO_REASON;
				}
				String entity = (resultSet.getString("watch_ip") != null) 
						? resultSet.getString("watch_ip")
						: Core.getPlayerName(resultSet.getString("UUID"));
				// If the UUID search failed
				if(entity == null){
					entity = "UUID:" + resultSet.getString("UUID");
				}
				final boolean active = (resultSet.getBoolean("watch_state") ? true : false);
				String unwatchReason = resultSet.getString("watch_unwatchreason");
				if(unwatchReason == null){
					unwatchReason = NO_REASON;
				}
				final String unwatchStaff = resultSet.getString("watch_unwatchstaff");
				watchList.add(new WatchEntry(entity, server, reason, staff, beginDate, endDate, unwatchDate, unwatchReason, unwatchStaff, active));
			}
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement, resultSet);
		}
		return watchList;
	}

    public void unloadWatchData(final ProxiedPlayer player, final int watchState) {
	    if (watchState == 1)
		BAT.broadcast( "Watched player " + player + " left \n", Action.WATCH_BROADCAST.getPermission());
    	    watchedPlayers.remove(player.getName());
	}

    public void postConnect(final ServerConnectedEvent e)
    {
	final ProxiedPlayer player = e.getPlayer();
	final String pName = player.getName();
	//	BAT.getInstance().getLogger().info ("running postConnect for player " +pName);
	final int watchState = isWatched(player, e.getServer().getInfo().getName());
	if (watchState == 1) {
		PlayerWatchData pWatchData = watchedPlayers.get(pName);
		if(pWatchData.isWatched(GLOBAL_SERVER)){
			loadWatchMessage(pName, GLOBAL_SERVER);
		}else if(pWatchData.isWatched(e.getServer().getInfo().getName())){
			loadWatchMessage(pName, e.getServer().getInfo().getName());
		}
		String msg =  _("watchConnectBroadcast", new String[] { pName, e.getServer().getInfo().getName()});
		BAT.broadcast( msg, Action.WATCH_BROADCAST.getPermission());
	}
    }
	// Event Listener
	@EventHandler(priority = EventPriority.HIGHEST) // we want MONITOR but it doens't exist in bungee
	public void onServerConnect(final ServerConnectedEvent e) {
	
	    final ProxiedPlayer player = e.getPlayer();
	    final String pName = player.getName();
	    BAT.getInstance().getLogger().info ("running onServerConnect for player " +pName);

		final int watchState = isWatched(player, e.getServer().getInfo().getName());
		if (watchState == -1) {
			// Load watch data with a little bit of delay to handle server switching operations which may take some time
			BAT.getInstance().getProxy().getScheduler().schedule(BAT.getInstance(), new Runnable() {
				@Override
				public void run() {
					updateWatchData(pName);
				}
			}, 250, TimeUnit.MILLISECONDS);
			// give the update time to run then see if the player is in watched status and print
			BAT.getInstance().getProxy().getScheduler().schedule(BAT.getInstance(), new Runnable() {
				@Override
				public void run() {
				    postConnect(e);
				}
			}, 750, TimeUnit.MILLISECONDS);

		} else if (watchState == 1) {
			PlayerWatchData pWatchData = watchedPlayers.get(pName);
			if(pWatchData.isWatched(GLOBAL_SERVER)){
				loadWatchMessage(pName, GLOBAL_SERVER);
			}else if(pWatchData.isWatched(e.getServer().getInfo().getName())){
				loadWatchMessage(pName, e.getServer().getInfo().getName());
			}
			//player.sendMessage(pWatchData.getWatchMessage(this));  Don't notify the player
			String msg =  _("watchConnectBroadcast", new String[] { pName, e.getServer().getInfo().getName()});
			BAT.broadcast( msg, Action.WATCH_BROADCAST.getPermission());
		}
	}


	@EventHandler
	public void onPlayerDisconnect(final PlayerDisconnectEvent e) {
		final ProxiedPlayer player = e.getPlayer();
		final int watchState = isWatched(player, GLOBAL_SERVER);
		unloadWatchData(player, watchState);
	}


}
