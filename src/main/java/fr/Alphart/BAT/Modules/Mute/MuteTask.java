package fr.Alphart.BAT.Modules.Mute;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.database.DataSourceHandler;
import fr.Alphart.BAT.database.SQLQueries;

/**
 * This task handles the mute related update :<br>
 * - check in the db for every active mute if it is finished
 * if this is the case : set mute_(ip)state to 0<br>
 * - update the PlayerMuteData of every player on the server
 * <br>
 * <b>This task must be runned asynchronously </b>
 */
public class MuteTask implements Runnable{
	private final Mute mute;

	public MuteTask(final Mute muteModule){
		mute = muteModule;
	}

	@Override
	public void run() {
		Statement statement = null;
		try  (Connection conn  = BAT.getConnection()) {
			statement = conn.createStatement();
			if(DataSourceHandler.isSQLite()){
				statement.executeUpdate(SQLQueries.Mute.SQLite.updateExpiredMute);
			}else{
				statement.executeUpdate(SQLQueries.Mute.updateExpiredMute);
			}
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally{
			DataSourceHandler.close(statement);
		}
		// Update player mute data
		for(final ProxiedPlayer player : ProxyServer.getInstance().getPlayers()){
			mute.updateMuteData(player.getName());
		}
	}
}
