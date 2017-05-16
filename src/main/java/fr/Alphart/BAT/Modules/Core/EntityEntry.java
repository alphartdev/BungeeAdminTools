package fr.Alphart.BAT.Modules.Core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.InvalidModuleException;
import fr.Alphart.BAT.Modules.ModulesManager;
import fr.Alphart.BAT.Modules.Ban.BanEntry;
import fr.Alphart.BAT.Modules.Comment.CommentEntry;
import fr.Alphart.BAT.Modules.Kick.KickEntry;
import fr.Alphart.BAT.Modules.Mute.MuteEntry;
import fr.Alphart.BAT.Modules.Watch.WatchEntry;
import fr.Alphart.BAT.Utils.UUIDNotFoundException;
import fr.Alphart.BAT.Utils.Utils;
import fr.Alphart.BAT.database.DataSourceHandler;
import fr.Alphart.BAT.database.SQLQueries;

/**
 * Summit all type of informations available with the plugin about an specific
 * entity.
 */
public class EntityEntry {
	public static final Timestamp noDateFound = new Timestamp(877478400); 
	// No use of the standard timestamp because the standard one is a good way to find when error happens (as 1970 isn't an usual date on mcserver)
	
	private final String entity;

	private final List<BanEntry> bans = new ArrayList<BanEntry>();
	private final List<MuteEntry> mutes = new ArrayList<MuteEntry>();
	private final List<WatchEntry> watches = new ArrayList<WatchEntry>();
	private final List<KickEntry> kicks = new ArrayList<KickEntry>();
	private final List<CommentEntry> comments = new ArrayList<CommentEntry>();

	private Timestamp firstLogin;
	private Timestamp lastLogin;
	private String lastIP = "0.0.0.0";

	private final List<String> ipUsers = new ArrayList<String>();

	private boolean exist = true;
	private boolean player = false;

	public EntityEntry(final String entity) {
		this.entity = entity;

		// This is a player
		if (!Utils.validIP(entity)) {
			// Get players basic information (first/last login, last ip)
			player = true;
			PreparedStatement statement = null;
			ResultSet resultSet = null;
			try (Connection conn = BAT.getConnection()) {
				statement = (DataSourceHandler.isSQLite()) ? conn
						.prepareStatement(SQLQueries.Core.SQLite.getPlayerData) : conn
						.prepareStatement(SQLQueries.Core.getPlayerData);
						statement.setString(1, Core.getUUID(entity));

						resultSet = statement.executeQuery();

						if (resultSet.next()) {
							if (DataSourceHandler.isSQLite()) {
								firstLogin = new Timestamp(resultSet.getLong("strftime('%s',firstlogin)") * 1000);
								lastLogin = new Timestamp(resultSet.getLong("strftime('%s',lastlogin)") * 1000);
							} else {
								firstLogin = resultSet.getTimestamp("firstlogin");
								lastLogin = resultSet.getTimestamp("lastlogin");
							}
							final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(entity);
							if (player != null) {
								lastIP = Utils.getPlayerIP(player);
							} else {
								lastIP = resultSet.getString("lastip");
							}
						}
						if(firstLogin == null){
						    firstLogin = noDateFound;
						}
						if(lastLogin == null){
						    lastLogin = noDateFound;
						}
			} catch (final SQLException e) {
				DataSourceHandler.handleException(e);
			} finally {
				DataSourceHandler.close(statement, resultSet);
			}
		}

		// This is an ip
		else {
			// Get users from this ip
			PreparedStatement statement = null;
			ResultSet resultSet = null;
			try (Connection conn = BAT.getConnection()) {
				statement = conn.prepareStatement(SQLQueries.Core.getIpUsers);
				statement.setString(1, entity);

				resultSet = statement.executeQuery();

				while (resultSet.next()) {
					ipUsers.add(resultSet.getString("BAT_player"));
				}
			} catch (final SQLException e) {
				DataSourceHandler.handleException(e);
			} finally {
				DataSourceHandler.close(statement, resultSet);
			}
		}
		
		// Load the data related to this entity of each modules
		final ModulesManager modules = BAT.getInstance().getModules();
		try {
			if (modules.isLoaded("ban")) {
				bans.addAll(modules.getBanModule().getBanData(entity));
			}
			if (modules.isLoaded("mute")) {
				mutes.addAll(modules.getMuteModule().getMuteData(entity));
			}

			if (modules.isLoaded("watch")) {
				watches.addAll(modules.getWatchModule().getWatchData(entity));
			}
			// No ip kick
			if (modules.isLoaded("kick") && ipUsers.isEmpty()) {
				kicks.addAll(modules.getKickModule().getKickData(entity));
			}
			if(modules.isLoaded("comment")){
				comments.addAll(modules.getCommentModule().getComments(entity));
			}		
		} catch (final InvalidModuleException | UUIDNotFoundException e) {
			if(e instanceof UUIDNotFoundException){
				exist = false;
			}
		}

	}

	public String getEntity() {
		return entity;
	}

	public List<BanEntry> getBans() {
		return bans;
	}

	public List<MuteEntry> getMutes() {
		return mutes;
	}

	public List<WatchEntry> getWatches() {
		return watches;
	}

	public List<KickEntry> getKicks() {
		return kicks;
	}

	public List<CommentEntry> getComments(){
		return comments;
	}
	
	public boolean exist() {
		return exist;
	}

	public boolean isPlayer() {
		return player;
	}

	public Timestamp getFirstLogin() {
		return firstLogin;
	}

	public Timestamp getLastLogin() {
		return lastLogin;
	}

	public String getLastIP() {
		return lastIP;
	}

	/**
	 * Get the players who have this ip as last ip used <br>
	 * Only works if the <b>entity is an adress ip</b>
	 * 
	 * @return list of players name
	 */
	public List<String> getUsers() {
		return ipUsers;
	}
}