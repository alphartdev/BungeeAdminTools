package fr.Alphart.BAT.Modules;

import java.util.List;

public interface IModule {
	// Constants
	// Server groups related
	public static final String NO_REASON = "noreason";
	public static final String GLOBAL_SERVER = "(global)";
	public static final String ANY_SERVER = "(any)";

	// Used for text formatting 
	public static final String STR_GLOBAL = "global";
	public static final String STR_NO_REASON = "not specified";

	// Module part
	public final static Integer ON_STATE = 1;
	public final static Integer OFF_STATE = 0;
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