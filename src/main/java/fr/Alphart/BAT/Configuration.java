package fr.Alphart.BAT;

import com.google.common.collect.Maps;
import net.cubespace.Yamler.Config.Comment;
import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.cubespace.Yamler.Config.Path;
import net.cubespace.Yamler.Config.YamlConfig;

import java.io.File;
import java.util.Locale;
import java.util.Map;

public class Configuration extends YamlConfig {
	public Configuration(){
		CONFIG_HEADER = new String[]{"Bungee Admin Tools - Configuration file"};
		CONFIG_FILE = new File(BAT.getInstance().getDataFolder(), "config.yml");
		try {
			init();
			save();
		} catch (final InvalidConfigurationException e) {
			e.printStackTrace();
		}
	}

	private String language = "en";
	private String prefix = "&6[&4BAT&6]&e ";

	public String getLanguage() {
		return this.language;
	}

	public String getPrefix() {
		return this.prefix;
	}

	public boolean getMustGiveReason() {
		return this.mustGiveReason;
	}

	public boolean isMustGiveReason() {
		return this.mustGiveReason;
	}

	public boolean getConfirmCommand() {
		return this.confirmCommand;
	}

	public boolean isConfirmCommand() {
		return this.confirmCommand;
	}

	public Map<String,Boolean> getSimpleAliasesCommands() {
		return this.simpleAliasesCommands;
	}

	public boolean getLitteralDate() {
		return this.litteralDate;
	}

	public boolean isLitteralDate() {
		return this.litteralDate;
	}

	public boolean getRedisSupport() {
		return this.redisSupport;
	}

	public boolean isRedisSupport() {
		return this.redisSupport;
	}

	public boolean getDebugMode() {
		return this.debugMode;
	}

	public boolean isDebugMode() {
		return this.debugMode;
	}

	public boolean getMysql_enabled() {
		return this.mysql_enabled;
	}

	public boolean isMysql_enabled() {
		return this.mysql_enabled;
	}

	public String getMysql_user() {
		return this.mysql_user;
	}

	public String getMysql_password() {
		return this.mysql_password;
	}

	public String getMysql_database() {
		return this.mysql_database;
	}

	public String getMysql_host() {
		return this.mysql_host;
	}

	public String getMysql_port() {
		return this.mysql_port;
	}
	
    @Comment("Force players to give reason when /ban /unban /kick /mute /unmute etc.")
	private boolean mustGiveReason= false;
	@Comment("Enable /bat confirm, to confirm command such as action on unknown player.")
	private boolean confirmCommand = true;
	@Comment("Enable or disable simple aliases to bypass the /bat prefix for core commands")
	private Map<String, Boolean> simpleAliasesCommands = Maps.newHashMap();
	@Comment("Make the date more readable."
			+ "If the date correspond to today, tmw or yda, it will replace the date by the corresponding word")
	private boolean litteralDate = true;
	@Comment("Enable BETA (experimental) Redis support, requires RedisBungee")
	private boolean redisSupport = false;
	@Comment("The debug mode enables verbose logging. All the logged message will be in the debug.log file in BAT folder")
	private boolean debugMode = false;
	
	
	@Comment("Set to true to use MySQL. Otherwise SQL Lite will be used")
    @Path(value = "mysql.enabled")
	private boolean mysql_enabled = true;
		public void mysql_enable(boolean enabled) { mysql_enabled = enabled; }
    @Path(value = "mysql.user")
	private String mysql_user = "user";
    @Path(value = "mysql.password")
	private String mysql_password = "password";
    @Path(value = "mysql.database")
	private String mysql_database = "database";
    @Path(value = "mysql.host")
	private String mysql_host = "localhost";
	@Comment("If you don't know it, just leave it like this (3306 = default mysql port)")
    @Path(value = "mysql.port")
	private String mysql_port = "3306";
	public Locale getLocale() {
		if (language.length() != 2) {
			BAT.getInstance().getLogger().severe("Incorrect language set ... The language was set to english.");
			return new Locale("en");
		}
		return new Locale(language);
	}
}
