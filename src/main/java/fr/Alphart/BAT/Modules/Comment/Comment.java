package fr.Alphart.BAT.Modules.Comment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import com.google.common.collect.Lists;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.I18n.I18n;
import fr.Alphart.BAT.Modules.BATCommand;
import fr.Alphart.BAT.Modules.IModule;
import fr.Alphart.BAT.Modules.ModuleConfiguration;
import fr.Alphart.BAT.Modules.Comment.CommentObject.Type;
import fr.Alphart.BAT.Modules.Core.Core;
import fr.Alphart.BAT.Utils.Utils;
import fr.Alphart.BAT.database.DataSourceHandler;
import fr.Alphart.BAT.database.SQLQueries;

public class Comment implements IModule{
	private final String name = "comment";
	private CommentCommand commandHandler;
	private final CommentConfig config;

	public Comment(){
		config = new CommentConfig();
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
		commandHandler = new CommentCommand(this);
		commandHandler.loadCmds();
		
		return true;
	}

	@Override
	public boolean unload() {
		
		return true;
	}

	public class CommentConfig extends ModuleConfiguration {
		public CommentConfig() {
			init(name);
		}
	}
	
	/**
	 * Get the notes relative to an entity
	 * @param entity | can be an ip or a player name
	 * @return
	 */
	public List<CommentObject> getComments(final String entity){
		List<CommentObject> notes = Lists.newArrayList();
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try (Connection conn = BAT.getConnection()) {
			statement = conn.prepareStatement(DataSourceHandler.isSQLite() 
					? SQLQueries.Comments.SQLite.getEntries
					: SQLQueries.Comments.getEntries);
			if(Utils.validIP(entity)){
				statement.setString(1, entity);
			}else{
				statement.setString(1, Core.getUUID(entity));
			}
			resultSet = statement.executeQuery();
			while(resultSet.next()){
				final long date;
				if(DataSourceHandler.isSQLite()){
					date = resultSet.getLong("strftime('%s',date)") * 1000;
				}else{
					date = resultSet.getTimestamp("date").getTime();
				}
				notes.add(new CommentObject(resultSet.getInt("id"), entity, resultSet.getString("note"), 
						resultSet.getString("staff"), CommentObject.Type.valueOf(resultSet.getString("type")), 
						date));
			}
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement, resultSet);
		}
		return notes;
	}

	public void insertComment(final String entity, final String comment, final Type type, final String author){
		PreparedStatement statement = null;
		try (Connection conn = BAT.getConnection()) {
			statement = conn.prepareStatement(SQLQueries.Comments.insertEntry);
			statement.setString(1, (Utils.validIP(entity)) ? entity : Core.getUUID(entity));
			statement.setString(2, comment);
			statement.setString(3, type.name());
			statement.setString(4, author);
			statement.executeUpdate();
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement);
		}
	}
	
	public String clearComments(final String entity){
		PreparedStatement statement = null;
		try (Connection conn = BAT.getConnection()) {
			statement = conn.prepareStatement(SQLQueries.Comments.clearEntries);
			statement.setString(1, (Utils.validIP(entity)) ? entity : Core.getUUID(entity));
			statement.executeUpdate();
			return I18n._("commentsCleared", new String[] {entity});
		} catch (final SQLException e) {
			return DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement);
		}
	}
}
