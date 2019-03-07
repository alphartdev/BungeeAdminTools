package fr.Alphart.BAT.Modules.Kick;

import static fr.Alphart.BAT.I18n.I18n._;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.BATCommand;
import fr.Alphart.BAT.Modules.IModule;
import fr.Alphart.BAT.Modules.ModuleConfiguration;
import fr.Alphart.BAT.Modules.Core.Core;
import fr.Alphart.BAT.database.DataSourceHandler;
import fr.Alphart.BAT.database.SQLQueries;

public class Kick implements IModule {
	private final String name = "kick";
	private KickCommand commandHandler;
	private final KickConfig config;

	public Kick(){
		config = new KickConfig();
	}

	@Override
	public List<BATCommand> getCommands() {
		return commandHandler.getCmds();
	}

	@Override
	public String getMainCommand() {
		return "kick";
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ModuleConfiguration getConfig() {
		return config;
	}

	@Override
	public boolean load() {
		// Init table
		Statement statement = null;
		try (Connection conn = BAT.getConnection()) {
			statement = conn.createStatement();
			if (DataSourceHandler.isSQLite()) {
				for (final String query : SQLQueries.Kick.SQLite.createTable) {
					statement.executeUpdate(query);
				}
			} else {
				statement.executeUpdate(SQLQueries.Kick.createTable);
			}
			statement.close();
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement);
		}

		// Register commands
		commandHandler = new KickCommand(this);
		commandHandler.loadCmds();

		return true;
	}

	@Override
	public boolean unload() {

		return false;
	}

	public class KickConfig extends ModuleConfiguration {
		public KickConfig() {
			init(name);
		}
	}

	/**
	 * Kick a player and tp him to the default server
	 * 
	 * @param player
	 * @param reason
	 */
	public String kick(final ProxiedPlayer player, final String staff, final String reason) {
		player.connect(ProxyServer.getInstance().getServerInfo(
				player.getPendingConnection().getListener().getDefaultServer()));
		player.sendMessage(TextComponent.fromLegacyText(_("wasKickedNotif", new String[] { reason })));
		return kickSQL(player.getUniqueId(), player.getServer().getInfo().getName(), staff, reason);
	}
	public String kickSQL(final UUID pUUID, final String server, final String staff, final String reason) {
		PreparedStatement statement = null;
		try (Connection conn = BAT.getConnection()) {
			if (DataSourceHandler.isSQLite()) {
				statement = conn.prepareStatement(SQLQueries.Kick.SQLite.kickPlayer);
			} else {
				statement = conn.prepareStatement(SQLQueries.Kick.kickPlayer);
			}
			statement.setString(1, pUUID.toString().replace("-", ""));
			statement.setString(2, staff);
			statement.setString(3, reason);
			statement.setString(4, server);
			statement.executeUpdate();
			statement.close();

			return _("kickBroadcast", new String[] { Core.getPlayerName(pUUID.toString().replace("-", "")), staff, server, reason });
		} catch (final SQLException e) {
			return DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement);
		}
	}

	/**
	 * Kick a player from the network
	 * 
	 * @param player
	 * @param reason
	 */
	public String gKick(final ProxiedPlayer player, final String staff, final String reason) {
		final String message = gKickSQL(player.getUniqueId(), staff, reason);
		player.disconnect(TextComponent.fromLegacyText(_("wasKickedNotif", new String[] { reason })));
		return message;
	}
	public String gKickSQL(final UUID pUUID, final String staff, final String reason) {
		PreparedStatement statement = null;
		try (Connection conn = BAT.getConnection()) {
			if (DataSourceHandler.isSQLite()) {
				statement = conn.prepareStatement(fr.Alphart.BAT.database.SQLQueries.Kick.SQLite.kickPlayer);
			} else {
				statement = conn.prepareStatement(SQLQueries.Kick.kickPlayer);
			}
			statement.setString(1, pUUID.toString().replace("-", ""));
			statement.setString(2, staff);
			statement.setString(3, reason);
			statement.setString(4, GLOBAL_SERVER);
			statement.executeUpdate();
			statement.close();

			if (BAT.getInstance().getRedis().isRedisEnabled()) {
			    	return _("gKickBroadcast", new String[] { RedisBungee.getApi().getNameFromUuid(pUUID), staff, reason });
			} else {
				return _("gKickBroadcast", new String[] { BAT.getInstance().getProxy().getPlayer(pUUID).getName(), staff, reason });
			}
		} catch (final SQLException e) {
			return DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement);
		}
	}

	/**
	 * Get all kick data of a player <br>
	 * <b>Should be runned async to optimize performance</b>
	 * 
	 * @param player
	 *            's name
	 * @return List of KickEntry of the player
	 */
	public List<KickEntry> getKickData(final String pName) {
		final List<KickEntry> kickList = new ArrayList<KickEntry>();
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try (Connection conn = BAT.getConnection()) {
			statement = conn.prepareStatement(DataSourceHandler.isSQLite()
					? SQLQueries.Kick.SQLite.getKick
					: SQLQueries.Kick.getKick);
			statement.setString(1, Core.getUUID(pName));
			resultSet = statement.executeQuery();

			while (resultSet.next()) {
				final String server = resultSet.getString("kick_server");
				String reason = resultSet.getString("kick_reason");
				if(reason == null){
					reason = NO_REASON;
				}
				final String staff = resultSet.getString("kick_staff");
				final Timestamp date;
				if(DataSourceHandler.isSQLite()){
					date = new Timestamp(resultSet.getLong("strftime('%s',kick_date)") * 1000);
				}else{
					date = resultSet.getTimestamp("kick_date");
				}
				kickList.add(new KickEntry(pName, server, reason, staff, date));
			}
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement, resultSet);
		}
		return kickList;
	}
	
	public List<KickEntry> getManagedKick(final String staff) {
		final List<KickEntry> kickList = new ArrayList<KickEntry>();
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try (Connection conn = BAT.getConnection()) {
			statement = conn.prepareStatement(DataSourceHandler.isSQLite()
					? SQLQueries.Kick.SQLite.getManagedKick
					: SQLQueries.Kick.getManagedKick);
			statement.setString(1, staff);
			resultSet = statement.executeQuery();

			while (resultSet.next()) {
				final String server = resultSet.getString("kick_server");
				String reason = resultSet.getString("kick_reason");
				if(reason == null){
					reason = NO_REASON;
				}
				String pName = Core.getPlayerName(resultSet.getString("UUID"));
				if(pName == null){
					pName = "UUID:" + resultSet.getString("UUID");
				}
				final Timestamp date;
				if(DataSourceHandler.isSQLite()){
					date = new Timestamp(resultSet.getLong("strftime('%s',kick_date)") * 1000);
				}else{
					date = resultSet.getTimestamp("kick_date");
				}
				kickList.add(new KickEntry(pName, server, reason, staff, date));
			}
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement, resultSet);
		}
		return kickList;
	}
}