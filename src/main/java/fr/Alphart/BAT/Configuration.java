package fr.Alphart.BAT;

import java.io.File;
import java.util.Locale;

import lombok.Getter;
import lombok.Setter;
import net.cubespace.Yamler.Config.Comment;
import net.cubespace.Yamler.Config.Config;
import net.cubespace.Yamler.Config.InvalidConfigurationException;

@Getter
public class Configuration extends Config{
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
	
	@Comment("Enable /bat confirm, to confirm command such as action on unknown player.")
	private boolean confirmCommand = true;
	@Comment("Enable simple aliases to bypass the /bat prefix to core commands")
	private boolean simpleAliases = false;
	@Comment("Make the date more readable."
			+ "If the date correspond to today, tmw or yda, it will replace the date by the corresponding word")
	private boolean litteralDate = true;
	
	@Comment("Set to true to use MySQL. Otherwise SQL Lite will be used")
	@Setter
	private boolean mysql_enabled = true;
	private String mysql_user = "user";
	private String mysql_password = "password";
	private String mysql_database = "database";
	private String mysql_host = "localhost";
	@Comment("If you don't know it, just leave it like this (3306 = default mysql port)")
	private String mysql_port = "3306";
	public Locale getLocale() {
		if (language.length() != 2) {
			BAT.getInstance().getLogger().severe("Incorrect language set ... The language was set to english.");
			return new Locale("en");
		}
		return new Locale(language);
	}
}