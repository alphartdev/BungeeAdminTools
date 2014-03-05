package fr.Alphart.BAT;

import net.craftminecraft.bungee.bungeeyaml.bukkitapi.ConfigurationSection;
import net.craftminecraft.bungee.bungeeyaml.bukkitapi.file.FileConfiguration;

public class Configuration {
	private FileConfiguration config;
	private final static String HEADER = "Bungee Admin Tools - Configuration file";

	public void load(){
		config = BAT.getInstance().getConfig();
		config.options().copyDefaults(true);

		// Add defaults
		config.options().header(HEADER);

		config.addDefault("prefix", "&6[&4BAT&6]&e");
		config.addDefault("language", "en");

		config.addDefault("storage.mysql.enabled", true);
		config.setComment("storage.mysql.enabled", "Set to true to use MySQL. Otherwise SQL Lite will be used");
		config.addDefault("storage.mysql.user", "<user>");
		config.addDefault("storage.mysql.password", "<password>");
		config.addDefault("storage.mysql.database", "<databaseName>");
		config.addDefault("storage.mysql.host", "<host>");
		config.addDefault("storage.mysql.port", 3306);

		config.addDefault("mute.enabled", true);
		config.addDefault("ban.enabled", true);
		config.addDefault("kick.enabled", true);

		BAT.getInstance().saveConfig();
		config.options().copyDefaults(true);
	}

	public ConfigurationSection getRootConfig(){ return config;}

	public ConfigurationSection getStorageConfig(){ return config.getConfigurationSection("storage");}
}