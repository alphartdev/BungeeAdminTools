package fr.Alphart.BAT.Modules.Mute;

import static fr.Alphart.BAT.BAT.__;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.event.EventHandler;
import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Message_temp;
import fr.Alphart.BAT.Modules.BATCommand;
import fr.Alphart.BAT.Modules.CommandHandler;
import fr.Alphart.BAT.Modules.IModule;
import fr.Alphart.BAT.Modules.ModuleConfiguration;
import fr.Alphart.BAT.Modules.Core.Core;
import fr.Alphart.BAT.Utils.FormatUtils;
import fr.Alphart.BAT.Utils.Utils;
import fr.Alphart.BAT.database.DataSourceHandler;
import fr.Alphart.BAT.database.SQLQueries;

/**
 * This module handles all the mute.<br>
 * The mute data of online players are <b>cached</b> in order to avoid lag.
 */
public class Mute implements IModule, Listener {
	private final String name = "mute";
	private static ConcurrentHashMap<String, PlayerMuteData> mutedPlayers;
	private CommandHandler commandHandler;
	private ScheduledTask task;
	private MuteConfig config;

	@Override
	public List<BATCommand> getCommands() {
		return commandHandler.getCmds();
	}

	@Override
	public String getMainCommand() {
		return "mute";
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
				for (final String query : SQLQueries.Mute.SQLite.createTable) {
					statement.executeUpdate(query);
				}
			} else {
				statement.executeUpdate(SQLQueries.Mute.createTable);
			}
			statement.close();
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement);
		}

		// Load configs
		config = new MuteConfig(this);

		// Register commands
		commandHandler = new MuteCommand(this);
		commandHandler.loadCmds();

		mutedPlayers = new ConcurrentHashMap<String, PlayerMuteData>();

		final MuteTask muteTask = new MuteTask(this);
		task = ProxyServer.getInstance().getScheduler().schedule(BAT.getInstance(), muteTask, 10, 10, TimeUnit.SECONDS);
		return true;
	}

	@Override
	public boolean unload() {
		task.cancel();
		mutedPlayers = null;
		return true;
	}

	public class MuteConfig extends ModuleConfiguration {
		private final List<String> forbiddenCmds;

		public MuteConfig(final IModule module) {
			super(module);
			config.addDefault("forbiddenCommands", Arrays.asList("msg"));
			config.setComment("forbiddenCommands", "List of commands that a player can't execute when he is mute");
			forbiddenCmds = config.getStringList("forbiddenCommands");
		}

		/**
		 * Get the forbidden commands for a mute player
		 * 
		 * @return list of forbidden commands
		 */
		public List<String> getForbiddenCmds() {
			return forbiddenCmds;
		}
	}

	/**
	 * Check if both ip and name of this player are muted<br>
	 * Use <b>cached data</b>
	 * 
	 * @param player
	 * @param server
	 * @return <ul>
	 *         <li>1 if the player is muted from this server</li>
	 *         <li>0 if he's not muted from this server</li>
	 *         <li>-1 if the data are loading</li>
	 *         </ul>
	 */
	public int isMute(final ProxiedPlayer player, final String server) {
		final PlayerMuteData pMuteData = mutedPlayers.get(player.getName());
		if (pMuteData != null) {
			if (pMuteData.isMute(server)) {
				return 1;
			}
			return 0;
		}

		return -1;
	}

	/**
	 * Check if this entity (player or ip) is muted<br>
	 * <b>Use uncached data. Use {@link #isMute(ProxiedPlayer, String)} instead
	 * of this method if the player is available</b>
	 * 
	 * @param mutedEntity
	 *            | can be an ip or a player name
	 * @param server
	 *            | if server equals to (any) check if the player is mute on a
	 *            server
	 * @param forceUseUncachedData
	 *            | use uncached data, for example to handle player or player's
	 *            ip related mute
	 * @return
	 */
	public boolean isMute(final String mutedEntity, final String server, boolean forceUseUncachedData) {
		// Check if the entity is an online player, in this case we're going to
		// use the cached method
		final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(mutedEntity);
		if (!forceUseUncachedData && player != null) {
			final int result = isMute(player, server);
			// If the data aren't loading
			if (result != -1) {
				return (result == 1);
			}
		}

		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try (Connection conn = BAT.getConnection()) {
			// If this is an ip which may be muted
			if (Utils.validIP(mutedEntity)) {
				final String ip = mutedEntity;
				statement = conn.prepareStatement((ANY_SERVER.equals(server)) ? SQLQueries.Mute.isMuteIP
						: SQLQueries.Mute.isMuteServerIP);
				statement.setString(1, ip);
				if (!ANY_SERVER.equals(server)) {
					statement.setString(2, server);
				}
			}
			// If this is a player which may be muted
			else {
				final String pName = mutedEntity;
				statement = conn.prepareStatement((ANY_SERVER.equals(server)) ? SQLQueries.Mute.isMute
						: SQLQueries.Mute.isMuteServer);
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
	 * Mute this entity (player or ip) <br>
	 * 
	 * @param mutedEntity
	 *            | can be an ip or a player name
	 * @param server
	 *            ; set to "(global)", to global mute
	 * @param staff
	 * @param expirationTimestamp
	 *            ; set to 0 for mute def
	 * @param reason
	 *            | optional
	 * @return
	 */
	public String mute(final String mutedEntity, final String server, final String staff,
			final long expirationTimestamp, final String reason) {
		PreparedStatement statement = null;
		try (Connection conn = BAT.getConnection()) {
			if (Utils.validIP(mutedEntity)) {
				final String ip = mutedEntity;
				statement = conn.prepareStatement(SQLQueries.Mute.createMuteIP);
				statement.setString(1, ip);
				statement.setString(2, staff);
				statement.setString(3, server);
				statement.setTimestamp(4, (expirationTimestamp > 0) ? new Timestamp(expirationTimestamp) : null);
				statement.setString(5, (NO_REASON.equals(reason)) ? null : reason);
				statement.executeUpdate();
				statement.close();

				for (final ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
					if (Utils.getPlayerIP(player).equals(ip)) {
						if (server.equals(GLOBAL_SERVER)) {
							mutedPlayers.get(player.getName()).setGlobal();
						} else {
							mutedPlayers.get(player.getName()).addServer(server);
						}
						player.sendMessage(__(Message_temp.WAS_MUTED_NOTIF.replace("%reason%",
								((NO_REASON.equals(reason)) ? Message_temp.NO_REASON : reason))));
					}
				}

				return FormatUtils.formatBroadcastMsg((expirationTimestamp > 0) ? Message_temp.MUTETEMP_BROADCAST
						: Message_temp.MUTE_BROADCAST, ip, staff, server, reason, expirationTimestamp);
			}

			// Otherwise it's a player
			else {
				final String pName = mutedEntity;
				final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
				final String ip = Core.getPlayerIP(pName);
				statement = conn.prepareStatement(SQLQueries.Mute.createMute);
				statement.setString(1, Core.getUUID(pName));
				statement.setString(2, ip);
				statement.setString(3, staff);
				statement.setString(4, server);
				statement.setTimestamp(5, (expirationTimestamp > 0) ? new Timestamp(expirationTimestamp) : null);
				statement.setString(6, (NO_REASON.equals(reason)) ? null : reason);
				statement.executeUpdate();
				statement.close();

				// Update the cached data
				if (player != null
						&& (server.equals(GLOBAL_SERVER) || player.getServer().getInfo().getName().equals(server))) {
					if (server.equals(GLOBAL_SERVER)) {
						mutedPlayers.get(player.getName()).setGlobal();
					} else {
						mutedPlayers.get(player.getName()).addServer(server);
					}
					player.sendMessage(__(Message_temp.WAS_MUTED_NOTIF.replace("%reason%",
							((NO_REASON.equals(reason)) ? Message_temp.NO_REASON : reason))));
				}

				return FormatUtils.formatBroadcastMsg((expirationTimestamp > 0) ? Message_temp.MUTETEMP_BROADCAST
						: Message_temp.MUTE_BROADCAST, pName, staff, server, reason, expirationTimestamp);
			}
		} catch (final SQLException e) {
			return DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement);
		}
	}

	/**
	 * Mute the ip of an online player
	 * 
	 * @param server
	 *            ; set to "(global)", to global mute
	 * @param staff
	 * @param duration
	 *            ; set to 0 for mute def
	 * @param reason
	 *            | optional
	 * @param ip
	 */
	public String muteIP(final ProxiedPlayer player, final String server, final String staff,
			final long expirationTimestamp, final String reason) {
		mute(Utils.getPlayerIP(player), server, staff, expirationTimestamp, reason);
		player.sendMessage(__(Message_temp.WAS_MUTED_NOTIF.replace("%reason%", ((NO_REASON.equals(reason)) ? Message_temp.NO_REASON
				: reason))));
		return FormatUtils.formatBroadcastMsg((expirationTimestamp > 0) ? Message_temp.MUTETEMP_BROADCAST
				: Message_temp.MUTE_BROADCAST, player.getName(), staff, server, reason, expirationTimestamp);
	}

	/**
	 * Unmute an entity (player or ip)
	 * 
	 * @param mutedEntity
	 *            | can be an ip or a player name
	 * @param server
	 *            | if equals to (any), unmute from all servers | if equals to
	 *            (global), remove global mute
	 * @param staff
	 * @param reason
	 * @param unMuteIP
	 */
	public String unMute(final String mutedEntity, final String server, final String staff, final String reason) {
		PreparedStatement statement = null;
		try (Connection conn = BAT.getConnection()) {
			// If the mutedEntity is an ip
			if (Utils.validIP(mutedEntity)) {
				final String ip = mutedEntity;
				if (ANY_SERVER.equals(server)) {
					statement = (DataSourceHandler.isSQLite()) ? conn.prepareStatement(SQLQueries.Mute.SQLite.unMuteIP)
							: conn.prepareStatement(SQLQueries.Mute.unMuteIP);
					statement.setString(1, reason);
					statement.setString(2, staff);
					statement.setString(3, ip);
				} else {
					statement = (DataSourceHandler.isSQLite()) ? conn
							.prepareStatement(SQLQueries.Mute.SQLite.unMuteIPServer) : conn
							.prepareStatement(SQLQueries.Mute.unMuteIPServer);
					statement.setString(1, reason);
					statement.setString(2, staff);
					statement.setString(3, ip);
					statement.setString(4, server);
				}
				statement.executeUpdate();
				statement.close();

				return FormatUtils.formatBroadcastMsg(Message_temp.UNMUTE_BROADCAST, ip, staff, server, reason, 0);
			}

			// Otherwise it's a player
			else {
				final String pName = mutedEntity;
				if (ANY_SERVER.equals(server)) {
					statement = (DataSourceHandler.isSQLite()) ? conn.prepareStatement(SQLQueries.Mute.SQLite.unMute)
							: conn.prepareStatement(SQLQueries.Mute.unMute);
					statement.setString(1, reason);
					statement.setString(2, staff);
					statement.setString(3, Core.getUUID(pName));
				} else {
					statement = (DataSourceHandler.isSQLite()) ? conn
							.prepareStatement(SQLQueries.Mute.SQLite.unMuteServer) : conn
							.prepareStatement(SQLQueries.Mute.unMuteServer);
					statement.setString(1, reason);
					statement.setString(2, staff);
					statement.setString(3, Core.getUUID(pName));
					statement.setString(4, server);
				}
				statement.executeUpdate();
				statement.close();

				final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
				if (player != null) {
					if (ANY_SERVER.equals(server)) {
						final PlayerMuteData pMuteData = mutedPlayers.get(player.getName());
						pMuteData.clearServers();
					} else {
						final PlayerMuteData pma = (mutedPlayers.get(player.getName()));
						if (pma != null) {
							pma.removeServer(server);
						}
					}
					player.sendMessage(__(Message_temp.WAS_UNMUTED_NOTIF.replace("%reason%",
							((NO_REASON.equals(reason)) ? Message_temp.NO_REASON : reason))));
				}

				return FormatUtils.formatBroadcastMsg(Message_temp.UNMUTE_BROADCAST, pName, staff, server, reason, 0);
			}
		} catch (final SQLException e) {
			return DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement);
		}
	}

	/**
	 * Unmute the ip of this entity
	 * 
	 * @param entity
	 * @param server
	 *            | if equals to (any), unmute from all servers | if equals to
	 *            (global), remove global mute
	 * @param staff
	 * @param reason
	 *            | optional
	 * @param duration
	 *            ; set to 0 for mute def
	 */
	public String unMuteIP(final String entity, final String server, final String staff, final String reason) {
		unMute((Utils.validIP(entity)) ? entity : Core.getPlayerIP(entity), server, staff, reason);
		return FormatUtils.formatBroadcastMsg(Message_temp.UNMUTE_BROADCAST, entity, staff, server, reason, 0);
	}

	/**
	 * Get all mute data of an entity <br>
	 * <b>Should be runned async to optimize performance</b>
	 * 
	 * @param entity
	 *            | can be an ip or a player name
	 * @return List of MuteEntry of the entity
	 */
	public List<MuteEntry> getMuteData(final String entity) {
		final List<MuteEntry> banList = new ArrayList<MuteEntry>();
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try (Connection conn = BAT.getConnection()) {
			// If the mutedEntity is an ip
			if (Utils.validIP(entity)) {
				final String ip = entity;
				statement = conn.prepareStatement(SQLQueries.Mute.getMuteIP);
				statement.setString(1, ip);
				resultSet = statement.executeQuery();

				while (resultSet.next()) {
					final String server = resultSet.getString("mute_server");
					final String reason = resultSet.getString("mute_reason");
					final String staff = resultSet.getString("mute_staff");
					final int begin_date = resultSet.getInt("mute_begin");
					final int end_date = resultSet.getInt("mute_end");
					final boolean active = (resultSet.getBoolean("mute_state") ? true : false);
					banList.add(new MuteEntry(ip, server, reason, staff, begin_date, end_date, active));
				}
			}

			// Otherwise if it's a player
			else {
				final String pName = entity;
				statement = conn.prepareStatement(SQLQueries.Mute.getMute);
				statement.setString(1, Core.getUUID(pName));
				statement.setString(2, Core.getPlayerIP(pName));
				resultSet = statement.executeQuery();

				while (resultSet.next()) {
					final String server = resultSet.getString("mute_server");
					final String reason = resultSet.getString("mute_reason");
					final String staff = resultSet.getString("mute_staff");
					final int begin_date = resultSet.getInt("mute_begin");
					final int end_date = resultSet.getInt("mute_end");
					final boolean active = (resultSet.getBoolean("mute_state") ? true : false);
					banList.add(new MuteEntry(pName, server, reason, staff, begin_date, end_date, active));
				}
			}
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement, resultSet);
		}
		return banList;
	}

	/**
	 * This class is used to cache the mute data of a player.
	 */
	public static class PlayerMuteData {
		private final List<String> servers;
		private boolean globalMute = false;

		public PlayerMuteData(final String pName, final List<String> servers) {
			this.servers = new ArrayList<String>(servers);
		}

		public void setGlobal() {
			globalMute = true;
		}

		public void unsetGlobal() {
			globalMute = false;
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

		public boolean isMute(final String server) {
			if (globalMute || (ANY_SERVER.equals(server) && !servers.isEmpty()) || servers.contains(server)) {
				return true;
			}
			return false;
		}
	}

	public void updateMuteData(final String pName) {
		final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
		if (player == null) {
			return;
		}
		PlayerMuteData pMuteData;
		if (mutedPlayers.containsKey(pName)) {
			pMuteData = mutedPlayers.get(pName);
			pMuteData.clearServers();
			pMuteData.unsetGlobal();
		} else {
			pMuteData = new PlayerMuteData(pName, new ArrayList<String>());
		}
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try (Connection conn = BAT.getConnection()) {
			statement = conn.prepareStatement("SELECT mute_server FROM `BAT_mute` WHERE mute_state = 1 AND UUID = ?;");
			statement.setString(1, Core.getUUID(pName));
			resultSet = statement.executeQuery();
			while (resultSet.next()) {
				final String server = resultSet.getString("mute_server");
				if (GLOBAL_SERVER.equals(server)) {
					pMuteData.setGlobal();
				} else {
					pMuteData.addServer(server);
				}
			}
			resultSet.close();
			statement.close();

			statement = conn
					.prepareStatement("SELECT mute_server FROM `BAT_mute` WHERE mute_state = 1 AND mute_ip = ?;");
			statement.setString(1, player.getAddress().getHostName());
			resultSet = statement.executeQuery();
			while (resultSet.next()) {
				final String server = resultSet.getString("mute_server");
				if (GLOBAL_SERVER.equals(server)) {
					pMuteData.setGlobal();
				} else {
					pMuteData.addServer(server);
				}
			}
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement, resultSet);
		}
		mutedPlayers.put(pName, pMuteData);
	}

	public void unloadMuteData(final ProxiedPlayer player) {
		mutedPlayers.remove(player.getName());
	}

	// Event Listener
	@EventHandler
	public void onServerConnect(final ServerConnectedEvent e) {
		final ProxiedPlayer player = e.getPlayer();

		final String pName = player.getName();
		final int muteState = isMute(player, e.getServer().getInfo().getName());
		if (muteState == -1) {
			// Load mute data
			BAT.getInstance().getProxy().getScheduler().runAsync(BAT.getInstance(), new Runnable() {
				@Override
				public void run() {
					updateMuteData(pName);
				}
			});
		} else if (muteState == 1) {
			player.sendMessage(__(Message_temp.ISMUTE));
		}
	}

	@EventHandler
	public void onPlayerDisconnect(final PlayerDisconnectEvent e) {
		unloadMuteData(e.getPlayer());
	}

	@EventHandler
	public void onPlayerChat(final ChatEvent e) {
		final ProxiedPlayer player = (ProxiedPlayer) e.getSender();
		final int muteState = isMute(player, player.getServer().getInfo().getName());
		if (muteState == 0) {
			return;
		}
		if (e.isCommand()) {
			final String command = e.getMessage().replaceAll("/", "").split(" ")[0];
			if (!config.getForbiddenCmds().contains(command)) {
				return;
			}
		}
		if (muteState == 1) {
			player.sendMessage(__(Message_temp.ISMUTE));
			e.setCancelled(true);
		} else if (muteState == -1) {
			player.sendMessage(__(Message_temp.LOADING_MUTEDATA));
			e.setCancelled(true);
		}
	}
}
