package fr.Alphart.BAT;

import java.io.File;
import java.util.Locale;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import net.cubespace.Yamler.Config.Comment;
import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.cubespace.Yamler.Config.Path;
import net.cubespace.Yamler.Config.YamlConfig;

import com.google.common.collect.Maps;

@Getter
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
	@Comment("For special setups, leave to false by default")
	private boolean forceOfflineMode = false;
	@Comment("The debug mode enables verbose logging. All the logged message will be in the debug.log file in BAT folder")
	private boolean debugMode = false;
	
	
	@Comment("Set to true to use MySQL. Otherwise SQL Lite will be used")
	@Setter
    @Path(value = "mysql.enabled")
	private boolean mysql_enabled = true;
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
	@Comment("Advanced parameters which should be happened at the end of mysql connection url, leave blank by default")
    @Path(value = "mysql.urlParameters")
    private String mysql_urlParameters = "";
	public Locale getLocale() {
		if (language.length() != 2) {
			BAT.getInstance().getLogger().severe("Incorrect language set ... The language was set to english.");
			return new Locale("en");
		}
		return new Locale(language);
	}
}
