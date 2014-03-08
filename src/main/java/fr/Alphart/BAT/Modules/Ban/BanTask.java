package fr.Alphart.BAT.Modules.Ban;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.database.DataSourceHandler;
import fr.Alphart.BAT.database.SQLQueries;

/**
 * This task handle the tempban's state update.
 */
public class BanTask implements Runnable {
	@Override
	public void run() {
		Statement statement = null;
		try (Connection conn = BAT.getConnection()) {
			statement = conn.createStatement();
			if (DataSourceHandler.isSQLite()) {
				statement.executeUpdate(SQLQueries.Ban.SQLite.updateExpiredBan);
			} else {
				statement.executeUpdate(SQLQueries.Ban.updateExpiredBan);
			}
			statement.close();
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement);
		}
	}
}