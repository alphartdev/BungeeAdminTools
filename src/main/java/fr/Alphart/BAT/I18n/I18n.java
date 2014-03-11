package fr.Alphart.BAT.I18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

import com.google.common.collect.Maps;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.IModule;

public class I18n {
	private static Map<String, String> argsReplacer = Maps.newHashMap();
	private ResourceBundle bundle;

	private I18n() {
		final Locale locale = BAT.getInstance().getConfiguration().getLocale();
		try {
			bundle = ResourceBundle.getBundle("messages", locale, new UTF8_Control());
			bundle.getString("GLOBAL");
		} catch (final MissingResourceException e) {
			BAT.getInstance()
			.getLogger()
			.severe("The language file " + locale.toLanguageTag()
					+ " was not found or is incorrect. The language was set to english.");
			bundle = ResourceBundle.getBundle("messages", new Locale("en"), new UTF8_Control());
		}

		argsReplacer.put(IModule.ANY_SERVER, ChatColor.translateAlternateColorCodes('&', bundle.getString("GLOBAL")));
		argsReplacer
		.put(IModule.GLOBAL_SERVER, ChatColor.translateAlternateColorCodes('&', bundle.getString("GLOBAL")));
		argsReplacer.put(IModule.NO_REASON, ChatColor.translateAlternateColorCodes('&', bundle.getString("NO_REASON")));
	}

	private static class I18nHolder {
		private final static I18n instance = new I18n();
	}

	private static I18n getInstance() {
		return I18nHolder.instance;
	}

	public static String getString(final String key) throws IllegalArgumentException {
		final String message = getInstance().bundle.getString(key);
		if (message != null) {
			return message;
		}
		throw new IllegalArgumentException("Invalid translation key message ...");
	}

	/**
	 * Format a message with given object. Parse color
	 * 
	 * @param message
	 * @param formatObject
	 * @return String
	 */
	public static String _(final String message, final String[] formatObject) {
		try {
			final MessageFormat mf = new MessageFormat(getString(message));
			return ChatColor.translateAlternateColorCodes('&', mf.format(preprocessArgs(formatObject)));
		} catch (final IllegalArgumentException e) {
			return "";
		}
	}

	/**
	 * Format a message with given object. Parse color
	 * 
	 * @param message
	 * @param formatObject
	 * @return String
	 */
	public static String _(final String message) {
		try {
			// Replace the quote as the message formatter does
			return ChatColor.translateAlternateColorCodes('&', getString(message).replace("''", "'"));
		} catch (final IllegalArgumentException e) {
			return "";
		}
	}

	/**
	 * Same as {@link #_(String, String[])} except it adds a prefix
	 * 
	 * @param message
	 * @param formatObject
	 * @return
	 */
	public static BaseComponent[] __(final String message, final String[] formatObject) {
		try {
			final MessageFormat mf = new MessageFormat(getString(message));
			return BAT.__(mf.format(preprocessArgs(formatObject)));
		} catch (final IllegalArgumentException e) {
			return TextComponent.fromLegacyText("");
		}
	}

	/**
	 * Same as {@link #_(String, String[])} except it adds a prefix
	 * 
	 * @param message
	 * @param formatObject
	 * @return
	 */
	public static BaseComponent[] __(final String message) {
		try {
			// Replace the quote as the message formatter does
			return BAT.__(getString(message).replace("''", "'"));
		} catch (final IllegalArgumentException e) {
			return TextComponent.fromLegacyText("");
		}
	}

	/**
	 * Preprocess formatArgs to replace value contained in the map argsReplacer,
	 * in order to have global instead of global for example
	 * 
	 * @param args
	 * @return
	 */
	public static String[] preprocessArgs(final String[] formatArgs) {
		for (int i = 0; i < formatArgs.length; i++) {
			if (argsReplacer.containsKey(formatArgs[i])) {
				formatArgs[i] = argsReplacer.get(formatArgs[i]);
			}
		}
		return formatArgs;
	}
}