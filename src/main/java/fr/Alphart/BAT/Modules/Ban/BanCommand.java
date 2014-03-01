package fr.Alphart.BAT.Modules.Ban;

import static com.google.common.base.Preconditions.checkArgument;

import java.math.BigInteger;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.BATCommand;
import fr.Alphart.BAT.Modules.BATCommand.RunAsync;
import fr.Alphart.BAT.Modules.CommandHandler;
import fr.Alphart.BAT.Modules.IModule;
import fr.Alphart.BAT.Modules.InvalidModuleException;
import fr.Alphart.BAT.Utils.FormatUtils;
import fr.Alphart.BAT.Utils.Utils;
import fr.Alphart.BAT.database.DataSourceHandler;

public class BanCommand extends CommandHandler {
	private static final String BAN_PERM = Ban.BAN_PERM;

	private static final String ALREADY_BAN = "&cCe joueur est déjà banni de ce serveur!";
	private static final String NOT_BAN = "&c%entity% n'est pas banni de ce serveur.";
	private static final String NOT_BANIP = "&c%entity% n'est pas banni IP de ce serveur.";
	private static final String NOT_BAN_ANY = "&c%entity% n'est banni d'aucun serveur !";

	private static final String SPECIFY_SERVER = "&cVous devez spécifier un serveur !";
	private static final String INVALID_SERVER = "&cLe serveur spécifié est invalide!";
	private static final String IP_OFFLINE_PLAYER = "&cLe joueur doit être connecté pour bannir son IP!";

	public BanCommand(final Ban banModule){
		super(banModule);
	}

	@RunAsync
	public static class BanCmd extends BATCommand{
		public BanCmd() {super("ban", "<nom> [serveur(serveur actuel par defaut)] [raison] - Bannit definitivement le joueur du serveur specifié", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			checkArgument(args.length >= 1);
			if(args[0].equals("help")){
				try {
					FormatUtils.showFormattedHelp(BAT.getInstance().getModules().getModule("ban").getCommands(), sender, "BAN");
				} catch (final InvalidModuleException e) {
					e.printStackTrace();
				}
				return;
			}
			final String pName = args[0];
			String returnedMsg = null;

			// Command pattern : /ban <name>
			if(args.length == 1){
				// If the sender isn't a player, he has to specify a server
				checkArgument(isPlayer(sender), SPECIFY_SERVER);

				final String server = ((ProxiedPlayer)sender).getServer().getInfo().getName();
				// If the player is already ban of this server, the command is gonna be cancelled
				checkArgument(!Ban.isBan(pName, server), ALREADY_BAN);

				returnedMsg = Ban.ban(pName, server, sender.getName(), 0, IModule.NO_REASON);
			}
			else{
				final String server = args[1];
				// Check if the server is an valid server
				checkArgument(Utils.isServer(server), INVALID_SERVER);
				// Check if the player isn't already banned from this server
				checkArgument(!Ban.isBan(pName, server), ALREADY_BAN);

				// Command pattern : /ban <name> <server>
				if(args.length == 2) {
					returnedMsg = Ban.ban(pName, server, sender.getName(), 0, IModule.NO_REASON);
				} else{
					final String reason = Utils.getFinalArg(args, 2);
					returnedMsg = Ban.ban(pName, server, sender.getName(), 0, reason);
				}
			}


			BAT.broadcast(returnedMsg, BAN_PERM);
		}
	}
	@RunAsync
	public static class BanIPCmd extends BATCommand{
		public BanIPCmd() {super("banip", "<nom/ip> [serveur(serveur actuel par defaut)] [raison] - Bannit définitivement l'IP du joueur du serveur specifié", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			checkArgument(args.length >= 1);

			final String entity = args[0];
			final boolean isIP = Utils.validIP(entity);
			final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(entity);
			if(!isIP) {
				checkArgument(player != null, IP_OFFLINE_PLAYER);
			}
			String returnedMsg = null;

			// Command pattern : /ban <name>
			if(args.length == 1){
				checkArgument(isPlayer(sender), SPECIFY_SERVER);

				final String server = ((ProxiedPlayer)sender).getServer().getInfo().getName();
				checkArgument(!Ban.isBan(entity, server), ALREADY_BAN);

				if(isIP) {
					returnedMsg = Ban.ban(entity, server, sender.getName(), 0, IModule.NO_REASON);
				} else {
					returnedMsg = Ban.banIP(player, server, sender.getName(), 0, IModule.NO_REASON);
				}
			}
			else{
				final String server = args[1];
				checkArgument(Utils.isServer(server), INVALID_SERVER);
				checkArgument(!Ban.isBan(entity, server), ALREADY_BAN);

				// Command pattern : /ban <name> <server>
				if(args.length == 2){
					if(isIP) {
						returnedMsg = Ban.ban(entity, server, sender.getName(), 0, IModule.NO_REASON);
					} else {
						returnedMsg = Ban.banIP(player, server, sender.getName(), 0, IModule.NO_REASON);
					}
				}

				// Command pattern : /ban <name> <server> <reason>
				else{
					final String reason = Utils.getFinalArg(args, 2);

					if(isIP) {
						returnedMsg = Ban.ban(entity, server, sender.getName(), 0, reason);
					} else {
						returnedMsg = Ban.banIP(player, server, sender.getName(), 0, reason);
					}
				}
			}


			BAT.broadcast(returnedMsg, BAN_PERM);
		}
	}
	@RunAsync
	public static class GBanCmd extends BATCommand{
		public GBanCmd() {super("gban", "<nom> [raison] - Bannit definitivement le joueur de tous les serveurs", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			checkArgument(args.length >= 1);

			final String pName = args[0];
			String returnedMsg;

			checkArgument(!Ban.isBan(pName, IModule.GLOBAL_SERVER), ALREADY_BAN);

			if(args.length == 1){
				returnedMsg = Ban.ban(pName, IModule.GLOBAL_SERVER, sender.getName(), 0, IModule.NO_REASON);
			}
			else{
				final String reason = Utils.getFinalArg(args, 1);
				returnedMsg = Ban.ban(pName, IModule.GLOBAL_SERVER, sender.getName(), 0, reason);
			}

			BAT.broadcast(returnedMsg, BAN_PERM);
		}
	}
	@RunAsync
	public static class GBanIPCmd extends BATCommand{
		public GBanIPCmd() {super("gbanip", "<nom/ip> [raison] - Bannit définitivement l'IP du joueur de tous les serveurs", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			checkArgument(args.length >= 1);

			final String entity = args[0];
			final boolean isIP = Utils.validIP(entity);
			final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(entity);
			if(!isIP) {
				checkArgument(player != null, IP_OFFLINE_PLAYER);
			}
			String returnedMsg;

			checkArgument(!Ban.isBan(entity, IModule.GLOBAL_SERVER), ALREADY_BAN);

			if(args.length == 1){
				if(isIP) {
					returnedMsg = Ban.ban(entity, IModule.GLOBAL_SERVER, sender.getName(), 0, IModule.NO_REASON);
				} else {
					returnedMsg = Ban.banIP(player, IModule.GLOBAL_SERVER, sender.getName(), 0, IModule.NO_REASON);
				}
			}
			else{
				final String reason = Utils.getFinalArg(args, 1);
				if(isIP) {
					returnedMsg = Ban.ban(entity, IModule.GLOBAL_SERVER, sender.getName(), 0, reason);
				} else {
					returnedMsg = Ban.banIP(player, IModule.GLOBAL_SERVER, sender.getName(), 0, reason);
				}
			}

			BAT.broadcast(returnedMsg, BAN_PERM);
		}
	}

	@RunAsync
	public static class TempBanCmd extends BATCommand{
		public TempBanCmd() {super("tempban", "<nom> <durée> [serveur(serveur actuel par defaut)] [raison] - Bannit temporairement le joueur du serveur specifié", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			checkArgument(args.length >= 2);
			final String pName = args[0];
			String returnedMsg;
			final int durate = Utils.parseDateDiff(args[1], true) - DataSourceHandler.getTimestamp();

			// Command pattern : /ban <name> <durate>
			if(args.length == 2){
				checkArgument(isPlayer(sender), SPECIFY_SERVER);
				final String server = ((ProxiedPlayer)sender).getServer().getInfo().getName();
				checkArgument(!Ban.isBan(pName, server), ALREADY_BAN);

				returnedMsg = Ban.ban(pName, server, sender.getName(), durate, IModule.NO_REASON);
			}
			else{
				final String server = args[2];
				checkArgument(Utils.isServer(server), INVALID_SERVER);
				checkArgument(!Ban.isBan(pName, server), ALREADY_BAN);

				// Command pattern: /ban <name> <durate> <server>
				if(args.length == 3) {
					returnedMsg = Ban.ban(pName, server, sender.getName(), durate, IModule.NO_REASON);
				} else{
					final String reason = Utils.getFinalArg(args, 3);
					returnedMsg = Ban.ban(pName, server, sender.getName(), durate, reason);
				}
			}

			BAT.broadcast(returnedMsg, BAN_PERM); 
		}
	}
	@RunAsync
	public static class TempBanIPCmd extends BATCommand{
		public TempBanIPCmd() {super("tempbanip", "<nom/ip> <durée> [serveur(serveur actuel par defaut)] [raison] - Bannit temporairement l'IP du joueur du serveur specifié", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			checkArgument(args.length >= 2);

			final String entity = args[0];
			final boolean isIP = Utils.validIP(entity);
			final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(entity);
			if(!isIP) {
				checkArgument(player != null, IP_OFFLINE_PLAYER);
			}
			String returnedMsg;
			final int durate = Utils.parseDateDiff(args[1], true) - DataSourceHandler.getTimestamp();

			// Command pattern : /ban <name> <durate>
			if(args.length == 2){
				checkArgument(isPlayer(sender), SPECIFY_SERVER);
				final String server = ((ProxiedPlayer)sender).getServer().getInfo().getName();
				checkArgument(!Ban.isBan(entity, server), ALREADY_BAN);

				if(isIP) {
					returnedMsg = Ban.ban(entity, server, sender.getName(), durate, IModule.NO_REASON);
				} else {
					returnedMsg = Ban.banIP(player, server, sender.getName(), durate, IModule.NO_REASON);
				}
			}
			else{
				final String server = args[2];
				checkArgument(Utils.isServer(server), INVALID_SERVER);
				checkArgument(!Ban.isBan(entity, server), ALREADY_BAN);

				// Command pattern: /ban <name> <durate> <server>
				if(args.length == 3){
					if(isIP) {
						returnedMsg = Ban.ban(entity, server, sender.getName(), durate, IModule.NO_REASON);
					} else {
						returnedMsg = Ban.banIP(player, server, sender.getName(), durate, IModule.NO_REASON);
					}
				}

				// Command pattern: /ban <name> <durate> <server> <reason>
				else{
					final String reason = Utils.getFinalArg(args, 3);
					if(isIP) {
						returnedMsg = Ban.ban(entity, server, sender.getName(), durate, reason);
					} else {
						returnedMsg = Ban.banIP(player, server, sender.getName(), durate, reason);
					}
				}
			}

			BAT.broadcast(returnedMsg, BAN_PERM); 

		}
	}
	@RunAsync
	public static class GTempBanCmd extends BATCommand{
		public GTempBanCmd() {super("gtempban", "<nom> <durée> [raison] - Bannit temporairement le joueur de tous les serveurs", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			checkArgument(args.length >= 2);

			final String pName = args[0];
			final int durate = Utils.parseDateDiff(args[1], true) - DataSourceHandler.getTimestamp();
			String returnedMsg;

			checkArgument(!Ban.isBan(pName, IModule.GLOBAL_SERVER), ALREADY_BAN);

			if(args.length == 2){
				returnedMsg = Ban.ban(pName, IModule.GLOBAL_SERVER, sender.getName(), durate, IModule.NO_REASON);
			}
			else{
				final String reason = Utils.getFinalArg(args, 2);
				returnedMsg = Ban.ban(pName, IModule.GLOBAL_SERVER, sender.getName(), durate, reason);
			}

			BAT.broadcast(returnedMsg, BAN_PERM);
		}
	}
	@RunAsync
	public static class GTempBanIPCmd extends BATCommand{
		public GTempBanIPCmd() {super("gtempbanip", "<nom/ip> <durée> [raison] - Bannit temporairement l'IP du joueur de tous les serveurs", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			checkArgument(args.length >= 2);

			final String entity = args[0];
			final boolean isIP = Utils.validIP(entity);
			final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(entity);
			if(!isIP) {
				checkArgument(player != null, IP_OFFLINE_PLAYER);
			}
			final int durate = Utils.parseDateDiff(args[1], true) - DataSourceHandler.getTimestamp();
			String returnedMsg;

			checkArgument(!Ban.isBan(entity, IModule.GLOBAL_SERVER), ALREADY_BAN);

			if(args.length == 2){
				if(isIP) {
					returnedMsg = Ban.ban(entity, IModule.GLOBAL_SERVER, sender.getName(), durate, IModule.NO_REASON);
				} else {
					returnedMsg = Ban.banIP(player, IModule.GLOBAL_SERVER, sender.getName(), durate, IModule.NO_REASON);
				}
			}
			else{
				final String reason = Utils.getFinalArg(args, 2);
				if(isIP) {
					returnedMsg = Ban.ban(entity, IModule.GLOBAL_SERVER, sender.getName(), durate, reason);
				} else {
					returnedMsg = Ban.banIP(player, IModule.GLOBAL_SERVER, sender.getName(), durate, reason);
				}
			}

			BAT.broadcast(returnedMsg, BAN_PERM);
		}
	}

	@RunAsync
	public static class PardonCmd extends BATCommand{
		public PardonCmd() {super("pardon", "<nom> [serveur(serveur actuel par defaut)] [raison] - Debannit le joueur", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			checkArgument(args.length >= 1);
			final String pName = args[0];
			String returnedMsg = null;

			// Command pattern : /ban <name>
			if(args.length == 1){
				checkArgument(isPlayer(sender), SPECIFY_SERVER);
				final String server = ((ProxiedPlayer)sender).getServer().getInfo().getName();
				checkArgument(Ban.isBan(pName, server), NOT_BAN.replaceAll("%entity%", pName));

				returnedMsg = Ban.unBan(pName, server, sender.getName(), IModule.NO_REASON);
			}
			else{
				final String server = args[1];
				checkArgument(Utils.isServer(server), INVALID_SERVER);
				checkArgument(Ban.isBan(pName, server), NOT_BAN.replaceAll("%entity%", pName));

				// Command pattern : /ban <name> <server>
				if(args.length == 2) {
					returnedMsg = Ban.unBan(pName, server, sender.getName(), IModule.NO_REASON);
				} else{
					final String reason = Utils.getFinalArg(args, 2);
					returnedMsg = Ban.unBan(pName, server, sender.getName(), reason);
				}
			}


			BAT.broadcast(returnedMsg, BAN_PERM);
		}	
	}
	@RunAsync
	public static class PardonIPCmd extends BATCommand{
		public PardonIPCmd() {super("pardonip", "<nom/ip> [serveur(serveur actuel par defaut)] [raison] - Debannit l'IP", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			checkArgument(args.length >= 1);
			final String entity = args[0];
			String returnedMsg = null;

			// Command pattern : /ban <name>
			if(args.length == 1){
				checkArgument(isPlayer(sender), SPECIFY_SERVER);
				final String server = ((ProxiedPlayer)sender).getServer().getInfo().getName();
				checkArgument(Ban.isBan(entity, server), NOT_BANIP.replaceAll("%entity%", entity));

				returnedMsg = Ban.unBanIP(entity, server, sender.getName(), IModule.NO_REASON);
			}
			else{
				final String server = args[1];
				checkArgument(Utils.isServer(server), INVALID_SERVER);
				checkArgument(Ban.isBan(entity, server), NOT_BANIP.replaceAll("%entity%", entity));

				// Command pattern : /ban <name> <server>
				if(args.length == 2) {
					returnedMsg = Ban.unBanIP(entity, server, sender.getName(), IModule.NO_REASON);
				} else{
					final String reason = Utils.getFinalArg(args, 2);
					returnedMsg = Ban.unBanIP(entity, server, sender.getName(), reason);
				}
			}


			BAT.broadcast(returnedMsg, BAN_PERM);
		}	
	}
	@RunAsync
	public static class GPardonCmd extends BATCommand{
		public GPardonCmd() {super("gpardon", "<nom> [raison] - Debannit le joueur de tous les serveurs", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			checkArgument(args.length >= 1);
			final String pName = args[0];
			String returnedMsg = null;

			checkArgument(Ban.isBan(pName, IModule.ANY_SERVER), NOT_BAN.replaceAll("%entity%", pName));

			if(args.length == 1) {
				returnedMsg = Ban.unBan(pName, IModule.ANY_SERVER, sender.getName(), IModule.NO_REASON);
			} else{
				final String reason = Utils.getFinalArg(args, 1);
				returnedMsg = Ban.unBan(pName, IModule.ANY_SERVER, sender.getName(), reason);
			}

			BAT.broadcast(returnedMsg, BAN_PERM);
		}	
	}
	@RunAsync
	public static class GPardonIPCmd extends BATCommand{
		public GPardonIPCmd() {super("gpardonip", "<nom/ip> [raison] - Debannit ip l'ip ou le joueur specifié de tous les serveurs", BAN_PERM);}

		@Override
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			checkArgument(args.length >= 1);
			final String entity = args[0];
			String returnedMsg = null;

			checkArgument(Ban.isBan(entity, IModule.ANY_SERVER), NOT_BAN_ANY.replaceAll("%entity%", entity));

			if(args.length == 1){
				returnedMsg = Ban.unBanIP(entity, IModule.ANY_SERVER, sender.getName(), IModule.NO_REASON);
			}
			else{
				final String reason = Utils.getFinalArg(args, 1);
				returnedMsg = Ban.unBanIP(entity, IModule.ANY_SERVER, sender.getName(), reason);
			}

			BAT.broadcast(returnedMsg, BAN_PERM);
		}	
	}

	public static class EuclideCmd extends BATCommand{
		public EuclideCmd() {super("euclide", "<n>- Calcule le nombre d'entier a n de 0 a n", BAN_PERM);}

		@Override
		@SuppressWarnings("deprecation")
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			if(args.length < 1){
				BATCommand.invalidArgs(sender, getUsage());
				return;
			}
			final long n = Long.parseLong(args[0]);

			final long nDivisePar100 = n / 100;

			int nbPremier = 0;

			sender.sendMessage("Demarrage de la boucle");
			for(long i=1; i < n; i++){
				final long pgcd = pgcd(n, i);
				if(pgcd == 1){
					sender.sendMessage(i + " est premier");
					nbPremier++;
				}
				if(i % nDivisePar100 == 0){
					sender.sendMessage("Progression " + i / n + "%");
				}
			}
			sender.sendMessage("Il y a " + nbPremier + " nbs premiers entre 0 et n. ==> phi =" + nbPremier);
		}

		public static long pgcd(long a, long b) {
			while (a != b) {
				if (a < b) {
					b = b - a;
				} else {
					a = a - b;
				}
			}
			return a;
		}
	}

	public static class ModuloCmd extends BATCommand{
		public ModuloCmd() {super("modulo", "<a> <puissance> <n> - Calcule a^puissance (mod n)", BAN_PERM);}

		@Override
		@SuppressWarnings("deprecation")
		public void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException {
			if(args.length < 3){
				BATCommand.invalidArgs(sender, getUsage());
				return;
			}
			final Integer a = Integer.parseInt(args[0]);
			final Integer puissance = Integer.parseInt(args[1]);
			final Integer n = Integer.parseInt(args[2]);

			BigInteger result = BigInteger.valueOf(a);

			sender.sendMessage(a+ "^" + puissance + "(mod " + n  + ") est egal à ");
			result = (result.pow(puissance));
			result = result.mod(BigInteger.valueOf(n));
			sender.sendMessage(result.toString());
		}
	}


}