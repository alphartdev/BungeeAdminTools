package fr.Alphart.BAT.I18n;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.IModule;

public class I18n {
	private static Map<String, String> argsReplacer = new HashMap<String, String>(){
		private static final long serialVersionUID = 1L;

		@Override
		public String put(final String key, final String value) {
			return super.put(key, ChatColor.translateAlternateColorCodes('&', value));
		};
	};
	private ResourceBundle enBundle;
	private ResourceBundle localeBundle;
	private ResourceBundle customBundle;

	private I18n() {
		final Locale locale = BAT.getInstance().getConfiguration().getLocale();
		enBundle = ResourceBundle.getBundle("messages", new Locale("en"), new UTF8_Control());
		try {
			localeBundle = ResourceBundle.getBundle("messages", locale, new UTF8_Control());
		} catch (final MissingResourceException e) {
			BAT.getInstance()
			.getLogger()
			.severe("The language file " + locale.toLanguageTag()
					+ " was not found or is incorrect.");
			localeBundle = enBundle;
		}
		// Try to load a custom bundle
		File pFile = null;
		try {
			for(final File file : BAT.getInstance().getDataFolder().listFiles()){
				if(file.getName().endsWith("language")){
					pFile = file;
					if(pFile.getName().toLowerCase().contains(locale.getLanguage().toLowerCase())){
						break;
					}
				}
			}
			if(pFile != null){
				customBundle = new PropertyResourceBundle(new FileReader(pFile));
			}
		} catch (final IOException e) {
			BAT.getInstance().getLogger().severe("The custom language file cannot be loaded.");
			e.printStackTrace();
		}
		if(customBundle == null){
			customBundle = localeBundle;
		}
		
		try{
			try{
				argsReplacer.put(IModule.ANY_SERVER, customBundle.getString("global"));
				argsReplacer
				.put(IModule.GLOBAL_SERVER, customBundle.getString("global"));
				argsReplacer.put(IModule.NO_REASON, customBundle.getString("noReason"));
			}catch(final MissingResourceException e){
				argsReplacer.put(IModule.ANY_SERVER, localeBundle.getString("global"));
				argsReplacer
				.put(IModule.GLOBAL_SERVER, localeBundle.getString("global"));
				argsReplacer.put(IModule.NO_REASON, localeBundle.getString("noReason"));
			}
		}catch(final MissingResourceException e){
			argsReplacer.put(IModule.ANY_SERVER, enBundle.getString("global"));
			argsReplacer
			.put(IModule.GLOBAL_SERVER, enBundle.getString("global"));
			argsReplacer.put(IModule.NO_REASON, enBundle.getString("noReason"));
		}

	}

	private static class I18nHolder {
		private static I18n instance = new I18n();
		
		private static void reload(){
			instance = new I18n();
		}
	}

	private static I18n getInstance() {
		return I18nHolder.instance;
	}

	public static String getString(final String key) throws IllegalArgumentException {
		String message;
		try{
			try{
				message = getInstance().customBundle.getString(key);
			}catch(final MissingResourceException e){
				message = getInstance().localeBundle.getString(key);
			}
		}catch(final MissingResourceException e){
			BAT.getInstance().getLogger().info("Incorrect translation key : " + key + ". Locale: " 
					+ getInstance().localeBundle.getLocale().getLanguage());
			try{
				message = getInstance().enBundle.getString(key);
			}catch(final MissingResourceException subE){
				BAT.getInstance().getLogger().warning("Incorrect translation key in default bundle."
						+ "Key : " + key);
				throw new IllegalArgumentException("Incorrect translation key, please check the log.");
			}
		}
		return message;
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

	public static void reload(){
		I18nHolder.reload();
	}
}