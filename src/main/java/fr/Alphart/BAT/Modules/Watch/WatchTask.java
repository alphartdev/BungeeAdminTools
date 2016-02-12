package fr.Alphart.BAT.Modules.Watch;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.database.DataSourceHandler;
import fr.Alphart.BAT.database.SQLQueries;

/**
 * This task handles the watch related update :<br>
 * - check in the db for every active watch if it is finished if this is the case
 * : set watch_(ip)state to 0<br>
 * - update the PlayerWatchData of every player on the server <br>
 * <b>This task must be run asynchronously </b>
 */
public class WatchTask implements Runnable {
	private final Watch watch;

	public WatchTask(final Watch watchModule) {
		watch = watchModule;
	}

	@Override
	public void run() {
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
		// Update player watch data
		for (final ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
			watch.updateWatchData(player.getName());
		}
	}
}
