package fr.Alphart.BAT.Modules;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fr.Alphart.BAT.BAT;

public abstract class CommandHandler{
	private final IModule module;
	private final List<BATCommand> commands;

	protected CommandHandler(final IModule module){
		this.module = module;
		commands = new ArrayList<BATCommand>();
	}

	public List<BATCommand> getCmds() {
		return commands;
	}

	public void loadCmds() {
		// Get all commands and put them in a list
		final List<String> cmdName = new ArrayList<String>();
		for(final Class<?> subClass : getClass().getDeclaredClasses()){
			try {
				final BATCommand command = (BATCommand)subClass.getConstructors()[0].newInstance();
				commands.add(command);
				cmdName.add(command.getName());
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
				BAT.getInstance().getLogger().severe("An error happend during loading of " + module.getName() + " commands please report this :");
				e.printStackTrace();
			}
		}

		// Add as default in the config file
		module.getConfig().addDefaultCmds(cmdName);

		// Sort the commands list and remove unused command
		final List<String> enabledCmds = module.getConfig().getEnabledCmds();
		final Iterator<BATCommand> it = commands.iterator();
		while(it.hasNext()){
			final BATCommand cmd = it.next();
			if(!enabledCmds.contains(cmd.getName())){
				it.remove();
			}
		}
	}
}
