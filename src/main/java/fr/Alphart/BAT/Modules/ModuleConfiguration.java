package fr.Alphart.BAT.Modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.craftminecraft.bungee.bungeeyaml.bukkitapi.ConfigurationSection;
import fr.Alphart.BAT.BAT;

public abstract class ModuleConfiguration {
	protected ConfigurationSection config;

	public ModuleConfiguration(final IModule module) {
		config = BAT.getInstance().getConfiguration().getRootConfig().getConfigurationSection(module.getName());
		if (config.getConfigurationSection("commands") == null) {
			config.createSection("commands");
		}
	}

	/**
	 * Get the names of the enabled commands for this module
	 * 
	 * @return list of the enabled commands
	 */
	public List<String> getEnabledCmds() {
		final List<String> enabledCmds = new ArrayList<String>();
		final Set<String> commandsSet = config.getConfigurationSection("commands").getKeys(false);
		for (final String command : commandsSet) {
			if (config.getConfigurationSection("commands").getBoolean(command)) {
				enabledCmds.add(command);
			}
		}
		return enabledCmds;
	}

	/**
	 * Add commands provided by this module into the configuration file
	 * 
	 * @param commands
	 *            list
	 */
	public void addDefaultCmds(final List<String> cmds) {
		Collections.sort(cmds);
		for (final String command : cmds) {
			config.getConfigurationSection("commands").addDefault(command, true);
		}
	}
}
