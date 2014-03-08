package fr.Alphart.BAT.Modules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Listener;
import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.Ban.Ban;
import fr.Alphart.BAT.Modules.Core.Core;
import fr.Alphart.BAT.Modules.Kick.Kick;
import fr.Alphart.BAT.Modules.Mute.Mute;

public class ModulesManager {
	private final Logger log;
	private String helpMessage;
	private final StringBuilder sb;
	private final Map<IModule, Integer> modules;
	private final Map<String, IModule> modulesNames;
	private Map<String, IModule> cmdsModules;

	public ModulesManager() {
		log = BAT.getInstance().getLogger();
		sb = new StringBuilder();
		modules = new HashMap<IModule, Integer>();
		modulesNames = new HashMap<String, IModule>();

		// The core module MUST NOT be disabled.
		modules.put(new Core(), IModule.OFF_STATE);
		modules.put(new Ban(), IModule.OFF_STATE);
		modules.put(new Mute(), IModule.OFF_STATE);
		modules.put(new Kick(), IModule.OFF_STATE);
	}

	public void showHelp(final CommandSender sender) {
		if (helpMessage == null) {
			sb.append("&2---- &1Bungee&fAdmin&cTools&2 - AIDE ----\n");
			for (final Entry<IModule, Integer> entry : modules.entrySet()) {
				if (entry.getValue() == IModule.ON_STATE) {
					sb.append("- &B/");
					sb.append(entry.getKey().getName());
					sb.append(" help&2 : Afficher l'aide relative au ");
					sb.append(entry.getKey().getName());
				} else {
					sb.append("- &MHors-Service : /");
					sb.append(entry.getKey().getName());
					sb.append(" help&2 : Afficher l'aide relative au ");
					sb.append(entry.getKey().getName());
				}
				sb.append("\n");
			}
			sb.append("&2-----------------------------------------");
			helpMessage = ChatColor.translateAlternateColorCodes('&', sb.toString());
			sb.setLength(0);
		}
		sender.sendMessage(new TextComponent(helpMessage));
	}

	public void loadModules() {
		cmdsModules = new HashMap<String, IModule>();
		for (final IModule module : modules.keySet()) {
			// The core doesn't have settings to enable or disable it
			if (!module.getName().equals("core")) {
				final Boolean isEnabled = BAT.getInstance().getConfiguration().getRootConfig()
						.getBoolean(module.getName() + ".enabled");
				if (isEnabled == null || !isEnabled) {
					continue;
				}
			}
			if (module.load()) {
				modulesNames.put(module.getName(), module);
				modules.put(module, IModule.ON_STATE);

				if (module instanceof Listener) {
					ProxyServer.getInstance().getPluginManager().registerListener(BAT.getInstance(), (Listener) module);
				}

				for (final BATCommand cmd : module.getCommands()) {
					cmdsModules.put(cmd.getName(), module);
					ProxyServer.getInstance().getPluginManager().registerCommand(BAT.getInstance(), cmd);
				}
			} else {
				log.severe("Le module de " + module.getName() + " a rencontre une erreur lors du chargement.");
			}
		}
		BAT.getInstance().saveConfig(); // Save the eventual changements made by
										// the different module
	}

	public Set<IModule> getLoadedModules() {
		final Set<IModule> modulesList = new HashSet<IModule>();
		for (final Entry<IModule, Integer> entry : modules.entrySet()) {
			if (entry.getValue() == IModule.ON_STATE) {
				modulesList.add(entry.getKey());
			}
		}
		return modulesList;
	}

	public boolean isLoaded(final String name) {
		try {
			if (getModule(name) != null) {
				return true;
			}
		} catch (final InvalidModuleException e) {
		}
		return false;
	}

	public Core getCore() {
		try {
			final IModule module = getModule("core");
			if (module != null) {
				return (Core) module;
			}
		} catch (final InvalidModuleException e) {
			BAT.getInstance().getLogger()
					.severe("The core module encountered a problem. Please report this to the developper :");
			e.printStackTrace();
		}
		return null;
	}

	public Ban getBanModule() throws InvalidModuleException {
		final IModule module = getModule("ban");
		if (module != null) {
			return (Ban) module;
		}
		return null;
	}

	public Mute getMuteModule() throws InvalidModuleException {
		final IModule module = getModule("mute");
		if (module != null) {
			return (Mute) module;
		}
		return null;
	}

	public Kick getKickModule() throws InvalidModuleException {
		final IModule module = getModule("kick");
		if (module != null) {
			return (Kick) module;
		}
		return null;
	}

	public IModule getModule(final String name) throws InvalidModuleException {
		final IModule module = modulesNames.get(name);
		if (module != null) {
			return module;
		}
		throw new InvalidModuleException();
	}
}