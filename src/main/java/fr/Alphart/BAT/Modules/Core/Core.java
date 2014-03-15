package fr.Alphart.BAT.Modules.Core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import com.google.common.base.Charsets;
import com.mojang.api.profiles.HttpProfileRepository;
import com.mojang.api.profiles.Profile;
import com.mojang.api.profiles.ProfileCriteria;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.BATCommand;
import fr.Alphart.BAT.Modules.IModule;
import fr.Alphart.BAT.Modules.ModuleConfiguration;
import fr.Alphart.BAT.Utils.UUIDNotFoundException;
import fr.Alphart.BAT.Utils.Utils;
import fr.Alphart.BAT.database.DataSourceHandler;
import fr.Alphart.BAT.database.SQLQueries;

public class Core implements IModule, Listener {
	private final String name = "core";
	private List<BATCommand> cmds;
	private static final HttpProfileRepository profileRepository = new HttpProfileRepository();

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
				for (final String query : SQLQueries.Core.SQLite.createTable) {
					statement.executeUpdate(query);
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
		cmds.add(new CoreCommand.CommandHandler());
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
	public static String getUUID(final String pName){
		final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
		if (player != null) {
			// Note: if it's an offline server, the UUID will be generated using
			// this
			// function java.util.UUID.nameUUIDFromBytes, however it's an
			// prenium or cracked account
			// Online server : bungee handle great the UUID
			return player.getUUID();
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
			DataSourceHandler.close(statement);
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
			UUID = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + pName).getBytes(Charsets.UTF_8)).toString();
		}

		return UUID;
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
			final String UUID = player.getUUID();
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
		final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
		if (player != null) {
			return Utils.getPlayerIP(player);
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