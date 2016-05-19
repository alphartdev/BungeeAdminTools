package fr.Alphart.BAT.Modules.Ban;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.IModule;
import fr.Alphart.BAT.database.DataSourceHandler;
import fr.Alphart.BAT.database.SQLQueries;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

/**
 * This task handle the tempban's state update.
 */
public class BanExpirationTask implements Runnable {
	private final Ban ban;

	public BanExpirationTask(final Ban ban) {
		this.ban = ban;
	}
	
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
		
		// Check if the online players are banned (if modifications have been made from the WebInterface)
		for(final ProxiedPlayer player : ProxyServer.getInstance().getPlayers()){
			final List<String> serversToCheck;
			if(player.getServer() != null){
			    serversToCheck = Arrays.asList(player.getServer().getInfo().getName(), IModule.GLOBAL_SERVER);
			}else{
			    serversToCheck = Arrays.asList(IModule.GLOBAL_SERVER);
			}
			for(final String server : serversToCheck){
				if(ban.isBan(player, server)){
					if (server.equals(player.getPendingConnection().getListener().getDefaultServer()) || server.equals(IModule.GLOBAL_SERVER)) {
						player.disconnect(ban.getBanMessage(player.getPendingConnection(), server));
						continue;
					}
					player.sendMessage(ban.getBanMessage(player.getPendingConnection(), server));
					player.connect(ProxyServer.getInstance().getServerInfo(player.getPendingConnection().getListener().getDefaultServer()));
				}
			}
		}
	}
}