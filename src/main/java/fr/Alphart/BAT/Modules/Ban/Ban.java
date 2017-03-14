package fr.Alphart.BAT.Modules.Ban;

import static fr.Alphart.BAT.I18n.I18n._;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.event.EventHandler;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.BATCommand;
import fr.Alphart.BAT.Modules.IModule;
import fr.Alphart.BAT.Modules.ModuleConfiguration;
import fr.Alphart.BAT.Modules.Core.Core;
import fr.Alphart.BAT.Utils.FormatUtils;
import fr.Alphart.BAT.Utils.UUIDNotFoundException;
import fr.Alphart.BAT.Utils.Utils;
import fr.Alphart.BAT.database.DataSourceHandler;
import fr.Alphart.BAT.database.SQLQueries;

public class Ban implements IModule, Listener {
	private final String name = "ban";
	private ScheduledTask task;
	private BanCommand commandHandler;
	private final BanConfig config;

	public Ban(){
		config = new BanConfig();
	}

	@Override
	public List<BATCommand> getCommands() {
		return commandHandler.getCmds();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getMainCommand() {
		return "ban";
	}

	@Override
	public ModuleConfiguration getConfig() {
		return config;
	}

	@Override
	public boolean load() {
		// Init table
		try (Connection conn = BAT.getConnection()) {
			final Statement statement = conn.createStatement();
			if (DataSourceHandler.isSQLite()) {
				for (final String query : SQLQueries.Ban.SQLite.createTable) {
					statement.executeUpdate(query);
				}
			} else {
				statement.executeUpdate(SQLQueries.Ban.createTable);
			}
			statement.close();
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		}

		// Register commands
		commandHandler = new BanCommand(this);
		commandHandler.loadCmds();

		// Launch tempban task
		final BanExpirationTask banExpirationTask = new BanExpirationTask(this);
		task = ProxyServer.getInstance().getScheduler().schedule(BAT.getInstance(), banExpirationTask, 0, 10, TimeUnit.SECONDS);

		// Check if the online players are banned (if the module has been reloaded)
		for(final ProxiedPlayer player : ProxyServer.getInstance().getPlayers()){
			final List<String> serversToCheck = player.getServer() != null
			        ? Arrays.asList(player.getServer().getInfo().getName(), GLOBAL_SERVER)
	                : Arrays.asList(GLOBAL_SERVER);
			for(final String server : serversToCheck){
				if(isBan(player, server)){
					if (server.equals(player.getPendingConnection().getListener().getDefaultServer()) || server.equals(GLOBAL_SERVER)) {
						player.disconnect(getBanMessage(player.getPendingConnection(), server));
						continue;
					}
					player.sendMessage(getBanMessage(player.getPendingConnection(), server));
					player.connect(ProxyServer.getInstance().getServerInfo(player.getPendingConnection().getListener().getDefaultServer()));
				}
			}
		}

		return true;
	}

	@Override
	public boolean unload() {
		task.cancel();
		return true;
	}

	public class BanConfig extends ModuleConfiguration {
		public BanConfig() {
			init(name);
		}
	}

	public BaseComponent[] getBanMessage(final PendingConnection pConn, final String server){
		String reason = "";
		Timestamp expiration = null;
		Timestamp begin = null;
		String staff = null;
		
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try (Connection conn = BAT.getConnection()) {
			statement = conn.prepareStatement(DataSourceHandler.isSQLite()
					? SQLQueries.Ban.SQLite.getBanMessage
					: SQLQueries.Ban.getBanMessage);
			try{
				final String pUUID;
	        	if(pConn.getUniqueId() != null && ProxyServer.getInstance().getConfig().isOnlineMode()){
	        		pUUID = pConn.getUniqueId().toString().replace("-", "");
	        	}
	        	else{
	        		pUUID = Utils.getOfflineUUID(pConn.getName());
	        	}
				statement.setString(1, pUUID);
				statement.setString(2, pConn.getAddress().getAddress().getHostAddress());
				statement.setString(3, server);
			}catch(final UUIDNotFoundException e){
				BAT.getInstance().getLogger().severe("Error during retrieving of the UUID of " + pConn.getName() + ". Please report this error :");
				e.printStackTrace();
			}
			resultSet = statement.executeQuery();
			
			if(resultSet.next()) {
				if(DataSourceHandler.isSQLite()){
					begin = new Timestamp(resultSet.getLong("strftime('%s',ban_begin)") * 1000);
					String endStr = resultSet.getString("ban_end"); // SQLite see this row as null but it doesn't seem to make the same with ban message though it's almost the same code ...
					expiration = (endStr == null) ? null : new Timestamp(Long.parseLong(endStr));
				}else{
					begin = resultSet.getTimestamp("ban_begin");
					expiration = resultSet.getTimestamp("ban_end");
				}
				reason = (resultSet.getString("ban_reason") != null) ? resultSet.getString("ban_reason") : IModule.NO_REASON;
				staff = resultSet.getString("ban_staff");
			}else{
				throw new SQLException("No active ban found.");
			}
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement, resultSet);
		}
		if(expiration != null){
			return TextComponent.fromLegacyText(_("isBannedTemp", 
					new String[]{ reason, (expiration.getTime() < System.currentTimeMillis()) ? "a few moments" : FormatUtils.getDuration(expiration.getTime()),
							Core.defaultDF.format(begin), staff }));
		}else{
			return TextComponent.fromLegacyText(_("isBanned", new String[]{ reason, Core.defaultDF.format(begin), staff }));
		}
	}
	
	/**
	 * Check if both ip and name of this player are banned
	 * 
	 * @param player
	 * @param server
	 * @return true if name or ip is banned
	 */
	public boolean isBan(final ProxiedPlayer player, final String server) {
		final String ip = Core.getPlayerIP(player.getName());
		if (isBan(player.getName(), server) || isBan(ip, server)) {
			return true;
		}
		return false;
	}

	/**
	 * Check if this entity (player or ip) is banned
	 * 
	 * @param bannedEntity
	 *            | can be an ip or a player name
	 * @param server
	 *            | if server equals to (any) check if the player is ban on a
	 *            server
	 * @return
	 */
	public boolean isBan(final String bannedEntity, final String server) {
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try (Connection conn = BAT.getConnection()) {
			// If this is an ip which may be banned
			if (Utils.validIP(bannedEntity)) {
				final String ip = bannedEntity;
				statement = conn.prepareStatement((ANY_SERVER.equals(server)) ? SQLQueries.Ban.isBanIP
						: SQLQueries.Ban.isBanServerIP);
				statement.setString(1, ip);
				if (!ANY_SERVER.equals(server)) {
					statement.setString(2, server);
				}
			}
			// If this is a player which may be banned
			else {
				final String pName = bannedEntity;
				final String uuid = Core.getUUID(pName);
				statement = conn.prepareStatement((ANY_SERVER.equals(server)) ? SQLQueries.Ban.isBan
						: SQLQueries.Ban.isBanServer);
				statement.setString(1, uuid);
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
	 * Ban this entity (player or ip) <br>
	 * 
	 * @param bannedEntity
	 *            | can be an ip or a player name
	 * @param server
	 *            ; set to "(global)", to global ban
	 * @param staff
	 * @param duration
	 *            ; set to 0 for ban def
	 * @param reason
	 *            | optional
	 * @return
	 */
	public String ban(final String bannedEntity, final String server, final String staff,
			final long expirationTimestamp, final String reason) {
		try (Connection conn = BAT.getConnection()) {
			// If the bannedEntity is an ip
			if (Utils.validIP(bannedEntity)) {
				final String ip = bannedEntity;

				final PreparedStatement statement = conn.prepareStatement(SQLQueries.Ban.createBanIP);
				statement.setString(1, ip);
				statement.setString(2, staff);
				statement.setString(3, server);
				statement.setTimestamp(4, (expirationTimestamp > 0) ? new Timestamp(expirationTimestamp) : null);
				statement.setString(5, (NO_REASON.equals(reason)) ? null : reason);
				statement.executeUpdate();
				statement.close();

				for (final ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
					if (Utils.getPlayerIP(player).equals(ip) && (GLOBAL_SERVER.equals(server) || server.equalsIgnoreCase(player.getServer().getInfo().getName())) ) {
						BAT.kick(player, _("wasBannedNotif", new String[] { reason }));
					}
				}
				
				if (BAT.getInstance().getRedis().isRedisEnabled()) {
				    	for (final UUID pUUID : RedisBungee.getApi().getPlayersOnline()) {
				    	    // Though they're provided by RedisBungee itself, sometimes Redis can't return PlayerIP or Server of this player so we just skip it
				    	    if(RedisBungee.getApi().getPlayerIp(pUUID) == null || RedisBungee.getApi().getServerFor(pUUID)==null){
				    	      BAT.getInstance().getLogger().config("Skipping UUID " + pUUID + " while iterating through players @ Redis support banip");
				    	      continue;
				    	    }
				    	    if (ip.equals(RedisBungee.getApi().getPlayerIp(pUUID)) && (GLOBAL_SERVER.equals(server) || server.equalsIgnoreCase(RedisBungee.getApi().getServerFor(pUUID).getName()))) {
				    	      BAT.getInstance().getRedis().sendGKickPlayer(pUUID, _("wasBannedNotif", new String[] { reason }));
				    	    }
				    	}
				}

				if (expirationTimestamp > 0) {
					return _("banTempBroadcast", new String[] { ip, FormatUtils.getDuration(expirationTimestamp),
							staff, server, reason });
				} else {
					return _("banBroadcast", new String[] { ip, staff, server, reason });
				}
			}

			// Otherwise it's a player
			else {
				final String pName = bannedEntity;
				final String sUUID = Core.getUUID(pName);
				final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
				final PreparedStatement statement = conn.prepareStatement(SQLQueries.Ban.createBan);
				statement.setString(1, sUUID);
				statement.setString(2, staff);
				statement.setString(3, server);
				statement.setTimestamp(4, (expirationTimestamp > 0) ? new Timestamp(expirationTimestamp) : null);
				statement.setString(5, (NO_REASON.equals(reason)) ? null : reason);
				statement.executeUpdate();
				statement.close();

				// Kick player if he's online and on the server where he's
				// banned
				if (player != null
						&& (server.equals(GLOBAL_SERVER) || player.getServer().getInfo().getName().equalsIgnoreCase(server))) {
					BAT.kick(player, _("wasBannedNotif", new String[] { reason }));
				} else if (BAT.getInstance().getRedis().isRedisEnabled()) {
				    	UUID pUUID = RedisBungee.getApi().getUuidFromName(pName);
				    	if (RedisBungee.getApi().isPlayerOnline(pUUID)
				    		&& ((server.equals(GLOBAL_SERVER) || RedisBungee.getApi().getServerFor(pUUID).getName().equalsIgnoreCase(server)))) {
				    	    	BAT.getInstance().getRedis().sendGKickPlayer(pUUID, _("wasBannedNotif", new String[] { reason }));
				    	}
				}

				if (expirationTimestamp > 0) {
					return _("banTempBroadcast", new String[] { pName, FormatUtils.getDuration(expirationTimestamp),
							staff, server, reason });
				} else {
					return _("banBroadcast", new String[] { pName, staff, server, reason });
				}
			}
		} catch (final SQLException e) {
			return DataSourceHandler.handleException(e);
		}
	}

	/**
	 * Ban the ip of an online player
	 * 
	 * @param server
	 *            ; set to "(global)", to global ban
	 * @param staff
	 * @param duration
	 *            ; set to 0 for ban def
	 * @param reason
	 *            | optional
	 * @param ip
	 */
	public String banIP(final ProxiedPlayer player, final String server, final String staff,
			final long expirationTimestamp, final String reason) {
		ban(Utils.getPlayerIP(player), server, staff, expirationTimestamp, reason);
		return _("banBroadcast", new String[] { player.getName() + "'s IP", staff, server, reason });
	}
	
	
	public String banRedisIP(final UUID pUUID, final String server, final String staff,
			final long expirationTimestamp, final String reason) {
	    	if (BAT.getInstance().getRedis().isRedisEnabled() && RedisBungee.getApi().isPlayerOnline(pUUID)) {
	    	    	ban(RedisBungee.getApi().getPlayerIp(pUUID).getHostAddress(), server, staff, expirationTimestamp, reason);
			return _("banBroadcast", new String[] { RedisBungee.getApi().getNameFromUuid(pUUID) + "'s IP", staff, server, reason });
	    	} else {
	    	    	return null;
	    	}
	    	    
	}

	/**
	 * Unban an entity (player or ip)
	 * 
	 * @param bannedEntity
	 *            | can be an ip or a player name
	 * @param server
	 *            | if equals to (any), unban from all servers | if equals to
	 *            (global), remove global ban
	 * @param staff
	 * @param reason
	 * @param unBanIP
	 */
	public String unBan(final String bannedEntity, final String server, final String staff, final String reason) {
		PreparedStatement statement = null;
		try (Connection conn = BAT.getConnection()) {
			// If the bannedEntity is an ip
			if (Utils.validIP(bannedEntity)) {
				final String ip = bannedEntity;
				if (ANY_SERVER.equals(server)) {
					statement = (DataSourceHandler.isSQLite()) ? conn.prepareStatement(SQLQueries.Ban.SQLite.unBanIP)
							: conn.prepareStatement(SQLQueries.Ban.unBanIP);
					statement.setString(1, reason);
					statement.setString(2, staff);
					statement.setString(3, ip);
				} else {
					statement = (DataSourceHandler.isSQLite()) ? conn
							.prepareStatement(SQLQueries.Ban.SQLite.unBanIPServer) : conn
							.prepareStatement(SQLQueries.Ban.unBanIPServer);
							statement.setString(1, reason);
							statement.setString(2, staff);
							statement.setString(3, ip);
							statement.setString(4, server);
				}
				statement.executeUpdate();

				return _("unbanBroadcast", new String[] { ip, staff, server, reason });
			}

			// Otherwise it's a player
			else {
				final String pName = bannedEntity;
				final String UUID = Core.getUUID(pName);
				if (ANY_SERVER.equals(server)) {
					statement = (DataSourceHandler.isSQLite()) ? conn.prepareStatement(SQLQueries.Ban.SQLite.unBan)
							: conn.prepareStatement(SQLQueries.Ban.unBan);
					statement.setString(1, reason);
					statement.setString(2, staff);
					statement.setString(3, UUID);
				} else {
					statement = (DataSourceHandler.isSQLite()) ? conn
							.prepareStatement(SQLQueries.Ban.SQLite.unBanServer) : conn
							.prepareStatement(SQLQueries.Ban.unBanServer);
							statement.setString(1, reason);
							statement.setString(2, staff);
							statement.setString(3, UUID);
							statement.setString(4, server);
				}
				statement.executeUpdate();

				return _("unbanBroadcast", new String[] { pName, staff, server, reason });
			}
		} catch (final SQLException e) {
			return DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement);
		}

	}

	/**
	 * Unban the ip of this entity
	 * 
	 * @param entity
	 * @param server
	 *            | if equals to (any), unban from all servers | if equals to
	 *            (global), remove global ban
	 * @param staff
	 * @param reason
	 *            | optional
	 * @param duration
	 *            ; set to 0 for ban def
	 */
	public String unBanIP(final String entity, final String server, final String staff, final String reason) {
		if (Utils.validIP(entity)) {
			return unBan(entity, server, staff, reason);
		} else {
			unBan(Core.getPlayerIP(entity), server, staff, reason);
			return _("unbanBroadcast", new String[] { entity + "'s IP", staff, server, reason });
		}
	}

	/**
	 * Get all ban data of an entity <br>
	 * <b>Should be runned async to optimize performance</b>
	 * 
	 * @param entity
	 * @return List of BanEntry of the player
	 */
	public List<BanEntry> getBanData(final String entity) {
		final List<BanEntry> banList = new ArrayList<BanEntry>();
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try (Connection conn = BAT.getConnection()) {
			// If the entity is an ip
			if (Utils.validIP(entity)) {
				statement = conn.prepareStatement((DataSourceHandler.isSQLite())
						? SQLQueries.Ban.SQLite.getBanIP
						: SQLQueries.Ban.getBanIP);
				statement.setString(1, entity);
				resultSet = statement.executeQuery();
			}
			// Otherwise if it's a player
			else {
				statement = conn.prepareStatement((DataSourceHandler.isSQLite())
						? SQLQueries.Ban.SQLite.getBan
						: SQLQueries.Ban.getBan);
				statement.setString(1, Core.getUUID(entity));
				resultSet = statement.executeQuery();
			}
			
			while (resultSet.next()) {
				final Timestamp beginDate;
				final Timestamp endDate;
				final Timestamp unbanDate;
				if(DataSourceHandler.isSQLite()){
					beginDate = new Timestamp(resultSet.getLong("strftime('%s',ban_begin)") * 1000);
					String endStr = resultSet.getString("ban_end");
					endDate = (endStr == null) ? null : new Timestamp(Long.parseLong(endStr));
					long unbanLong = resultSet.getLong("strftime('%s',ban_unbandate)") * 1000;
					unbanDate = (unbanLong == 0) ? null : new Timestamp(unbanLong);
				}else{
					beginDate = resultSet.getTimestamp("ban_begin");
					endDate = resultSet.getTimestamp("ban_end");
					unbanDate = resultSet.getTimestamp("ban_unbandate");
				}

				
				// Make it compatible with sqlite (date: get an int with the sfrt and then construct a tiemstamp)
				final String server = resultSet.getString("ban_server");
				String reason = resultSet.getString("ban_reason");
				if(reason == null){
					reason = NO_REASON;
				}
				final String staff = resultSet.getString("ban_staff");
				final boolean active = (resultSet.getBoolean("ban_state") ? true : false);
				String unbanReason = resultSet.getString("ban_unbanreason");
				if(unbanReason == null){
					unbanReason = NO_REASON;
				}
				final String unbanStaff = resultSet.getString("ban_unbanstaff");
				banList.add(new BanEntry(entity, server, reason, staff, beginDate, endDate, unbanDate, unbanReason, unbanStaff, active));
			}
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement, resultSet);
		}
		return banList;
	}

	public List<BanEntry> getManagedBan(final String staff){
		final List<BanEntry> banList = new ArrayList<BanEntry>();
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try (Connection conn = BAT.getConnection()) {
			statement = conn.prepareStatement((DataSourceHandler.isSQLite())
					? SQLQueries.Ban.SQLite.getManagedBan
					: SQLQueries.Ban.getManagedBan);
			statement.setString(1, staff);
			statement.setString(2, staff);
			resultSet = statement.executeQuery();
			
			while (resultSet.next()) {
				final Timestamp beginDate;
				final Timestamp endDate;
				final Timestamp unbanDate;
				if(DataSourceHandler.isSQLite()){
					beginDate = new Timestamp(resultSet.getLong("strftime('%s',ban_begin)") * 1000);
					String endStr = resultSet.getString("ban_end");
					endDate = (endStr == null) ? null : new Timestamp(Long.parseLong(endStr));
					long unbanLong = resultSet.getLong("strftime('%s',ban_unbandate)") * 1000;
					unbanDate = (unbanLong == 0) ? null : new Timestamp(unbanLong);
				}else{
					beginDate = resultSet.getTimestamp("ban_begin");
					endDate = resultSet.getTimestamp("ban_end");
					unbanDate = resultSet.getTimestamp("ban_unbandate");
				}

				
				// Make it compatible with sqlite (date: get an int with the sfrt and then construct a tiemstamp)
				final String server = resultSet.getString("ban_server");
				String reason = resultSet.getString("ban_reason");
				if(reason == null){
					reason = NO_REASON;
				}
				String entity = (resultSet.getString("ban_ip") != null) 
						? resultSet.getString("ban_ip")
						: Core.getPlayerName(resultSet.getString("UUID"));
				// If the UUID search failed
				if(entity == null){
					entity = "UUID:" + resultSet.getString("UUID");
				}
				final boolean active = (resultSet.getBoolean("ban_state") ? true : false);
				String unbanReason = resultSet.getString("ban_unbanreason");
				if(unbanReason == null){
					unbanReason = NO_REASON;
				}
				final String unbanStaff = resultSet.getString("ban_unbanstaff");
				banList.add(new BanEntry(entity, server, reason, staff, beginDate, endDate, unbanDate, unbanReason, unbanStaff, active));
			}
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement, resultSet);
		}
		return banList;
	}
	
	/**
	 * @param amount
	 * @param startIndex
	 * @return Return <i>amount</i> ban entries starting from the <i>startIndex</i>th one, sorted by date
	 */
	public List<BanEntry> getBans(int amount, int startIndex){
	  final List<BanEntry> banList = new ArrayList<BanEntry>();
      PreparedStatement statement = null;
      ResultSet resultSet = null;
      try (Connection conn = BAT.getConnection()) {
          statement = conn.prepareStatement("SELECT * FROM `BAT_ban` ORDER BY ban_begin DESC LIMIT ? OFFSET ?;");
          statement.setInt(1, amount);
          statement.setInt(2, startIndex);
          resultSet = statement.executeQuery();
          
          while (resultSet.next()) {
              final String staff = resultSet.getString("ban_staff");
              final Timestamp beginDate = resultSet.getTimestamp("ban_begin");
              final Timestamp endDate = resultSet.getTimestamp("ban_end");
              final Timestamp unbanDate = resultSet.getTimestamp("ban_unbandate");

              final String server = resultSet.getString("ban_server");
              String reason = resultSet.getString("ban_reason");
              if(reason == null){
                  reason = NO_REASON;
              }
              String entity = (resultSet.getString("ban_ip") != null) 
                      ? resultSet.getString("ban_ip")
                      : Core.getPlayerName(resultSet.getString("UUID"));
              // If the UUID search failed
              if(entity == null){
                  entity = "UUID:" + resultSet.getString("UUID");
              }
              final boolean active = (resultSet.getBoolean("ban_state") ? true : false);
              String unbanReason = resultSet.getString("ban_unbanreason");
              if(unbanReason == null){
                  unbanReason = NO_REASON;
              }
              final String unbanStaff = resultSet.getString("ban_unbanstaff");
              banList.add(new BanEntry(entity, server, reason, staff, beginDate, endDate, unbanDate, unbanReason, unbanStaff, active));
          }
      } catch (final SQLException e) {
          DataSourceHandler.handleException(e);
      } finally {
          DataSourceHandler.close(statement, resultSet);
      }
      return banList;
	}
	
	// Event listener
	
	@EventHandler
	public void onServerConnect(final ServerConnectEvent e) {
		final ProxiedPlayer player = e.getPlayer();
		final String target = e.getTarget().getName();

		if (isBan(player, target)) {
			if (target.equals(player.getPendingConnection().getListener().getDefaultServer())) {
				// If it's player's join server kick him
				if(e.getPlayer().getServer() == null){
					e.setCancelled(true);
					// Need to delay for avoiding the "bit cannot be cast to fm exception" and to annoy the banned player :p
					ProxyServer.getInstance().getScheduler().schedule(BAT.getInstance(), new Runnable() {
						@Override
						public void run() {
							e.getPlayer().disconnect(getBanMessage(player.getPendingConnection(), target));
						}
					}, 500, TimeUnit.MILLISECONDS);
				}else{
					e.setCancelled(true);
					e.getPlayer().sendMessage(getBanMessage(player.getPendingConnection(), target));
				}
				return;
			}
			player.sendMessage(getBanMessage(player.getPendingConnection(), target));
			if (player.getServer() == null) {
				player.connect(ProxyServer.getInstance().getServerInfo(
						player.getPendingConnection().getListener().getDefaultServer()));
			}
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerLogin(final LoginEvent ev) {
		ev.registerIntent(BAT.getInstance());
	    BAT.getInstance().getProxy().getScheduler().runAsync(BAT.getInstance(), new Runnable()
	    {
	      public void run() {
	        boolean isBanPlayer = false;

	        PreparedStatement statement = null;
	        ResultSet resultSet = null;
	        String uuid = null;
	        try(Connection conn = BAT.getConnection()){ 
	        	statement = conn.prepareStatement("SELECT ban_id FROM `BAT_ban` WHERE ban_state = 1 AND UUID = ? AND ban_server = '" + GLOBAL_SERVER + "';");
	        	// If this is an online mode server, the uuid will be already set
	        	if(ev.getConnection().getUniqueId() != null  && ProxyServer.getInstance().getConfig().isOnlineMode()){
	        		uuid = ev.getConnection().getUniqueId().toString().replaceAll( "-", "" );
	        	}
	        	// Otherwise it's an offline mode server, so we're gonna generate the UUID using player name (hashing)
	        	else{
	        		uuid = Utils.getOfflineUUID(ev.getConnection().getName());
	        	}
	            statement.setString(1, uuid.toString());
	        	
	            resultSet = statement.executeQuery();
	            if (resultSet.next()){
	              isBanPlayer = true;
	            }
	        } catch (SQLException e) { 
	        	DataSourceHandler.handleException(e);
	        } finally {
	          DataSourceHandler.close(statement, resultSet);
	        }

	        if ((isBanPlayer) || (isBan(ev.getConnection().getAddress().getAddress().getHostAddress(), GLOBAL_SERVER))) {
	          BaseComponent[] bM = getBanMessage(ev.getConnection(), GLOBAL_SERVER);
	          ev.setCancelReason(TextComponent.toLegacyText(bM));
	          ev.setCancelled(true);
	        }
	        ev.completeIntent(BAT.getInstance());
	      }
	    });
	}
}