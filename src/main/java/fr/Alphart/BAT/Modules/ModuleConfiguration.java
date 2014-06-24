package fr.Alphart.BAT.Modules;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lombok.Getter;
import net.cubespace.Yamler.Config.Config;
import net.cubespace.Yamler.Config.InvalidConfigurationException;
import fr.Alphart.BAT.BAT;

public abstract class ModuleConfiguration extends Config {
	
	// We must use an init method because if we use the super constructor, it doesn't work properly (field of children class are overwritten)
	public void init(final String moduleName){
		CONFIG_HEADER = new String[] { "BungeeAdminTools - " + moduleName + " configuration file" };
		CONFIG_FILE = new File(BAT.getInstance().getDataFolder(), moduleName + ".yml");
		try {
			init();
			load();
		} catch (final InvalidConfigurationException e) {
			e.printStackTrace();
		}
	}

	@Getter
	private boolean enabled = true;

	private Map<String, Boolean> commands = new HashMap<String, Boolean>();

	/**
	 * Get the names of the enabled commands for this module
	 * 
	 * @return list of the enabled commands
	 */
	public List<String> getEnabledCmds() {
		final List<String> enabledCmds = new ArrayList<String>();
		for (final Entry<String, Boolean> entry : commands.entrySet()) {
			if (entry.getValue()) {
				enabledCmds.add(entry.getKey());
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
	public void setProvidedCmds(final List<String> cmds) {
		Collections.sort(cmds);
		// Add new commands if there are
		for (final String cmdName : cmds) {
			if (!commands.containsKey(cmdName)) {
				commands.put(cmdName, true);
			}
		}
		// Iterate through the commands map and remove the ones who don't exist (e.g because of an update)
		for(final Iterator<Map.Entry<String, Boolean>> it = commands.entrySet().iterator(); it.hasNext();){
			final Map.Entry<String, Boolean> cmdEntry = it.next();
			if(!cmds.contains(cmdEntry.getKey())){
				it.remove();
			}
		}
	}
}