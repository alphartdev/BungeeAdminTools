package fr.Alphart.BAT.Modules;

import java.util.List;

public interface IModule {
	public final static Integer ON_STATE = 1;
	public final static Integer OFF_STATE = 0;

	// Server groups related var
	public static final String NO_REASON = "noreason";
	public static final String GLOBAL_SERVER = "(global)";
	public static final String ANY_SERVER = "(any)";

	public String getName();

	/**
	 * Load the module
	 * @return true if everything's ok otherwise false
	 */
	public boolean load();

	/**
	 * Get the configuration section of this module
	 * @return configuration section of this module
	 */
	public ModuleConfiguration getConfig();

	/**
	 * Unload the module
	 * @return true if everything's ok otherwise false
	 */
	public boolean unload();

	/**
	 * Get main command name
	 * @return name of the main command without a slash
	 */
	public String getMainCommand();

	/**
	 * Get commands used by this module
	 * @return list of commands
	 */
	public List<BATCommand> getCommands();
}