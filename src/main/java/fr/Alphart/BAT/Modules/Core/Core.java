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
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.BATCommand;
import fr.Alphart.BAT.Modules.IModule;
import fr.Alphart.BAT.Modules.ModuleConfiguration;
import fr.Alphart.BAT.Utils.Utils;
import fr.Alphart.BAT.database.DataSourceHandler;
import fr.Alphart.BAT.database.SQLQueries;

public class Core implements IModule, Listener{
	private final String name = "core";
	private List<BATCommand> cmds;

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
		try  (Connection conn  = BAT.getConnection()) {
			statement = conn.createStatement();
			if(DataSourceHandler.isSQLite()){
				statement.executeUpdate(SQLQueries.Core.SQLite.createTable);
			}
			else{
				statement.executeUpdate(SQLQueries.Core.createTable);
			}
			statement.close();
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally{
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

	public void updatePlayerIP(final ProxiedPlayer player){
		PreparedStatement statement = null; 
		try  (Connection conn  = BAT.getConnection()) {
			statement = (DataSourceHandler.isSQLite())
					? conn.prepareStatement(SQLQueries.Core.SQLite.updateIP)
							: conn.prepareStatement(SQLQueries.Core.updateIP);
					statement.setString(1, player.getName());
					final String ip = Utils.getPlayerIP(player);
					statement.setString(2, ip);
					statement.setString(3, (DataSourceHandler.isSQLite()) ? player.getName() : ip);
					statement.executeUpdate();
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally{
			DataSourceHandler.close(statement);
		}

	}

	public static String getPlayerIP(final String pName){
		final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
		if(player != null) {
			return Utils.getPlayerIP(player);
		}

		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try  (Connection conn  = BAT.getConnection()) {
			statement = conn.prepareStatement(SQLQueries.Core.getIP);
			statement.setString(1, pName);
			resultSet = statement.executeQuery();
			if(resultSet.next()) {
				return resultSet.getString("lastip");
			}
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally{
			DataSourceHandler.close(statement, resultSet);
		}
		return "0.0.0.0";
	}

	@EventHandler
	public void onPlayerJoin(final PostLoginEvent e){
		BAT.getInstance().getProxy().getScheduler().runAsync(BAT.getInstance(), new Runnable() {

			@Override
			public void run() {
				updatePlayerIP(e.getPlayer());
			}
		});
	}
}