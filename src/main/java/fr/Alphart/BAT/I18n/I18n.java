package fr.Alphart.BAT.I18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import fr.Alphart.BAT.BAT;

public class I18n{
	ResourceBundle bundle;
	
	private I18n(){
		bundle = ResourceBundle.getBundle("messages", new Locale("en", "EN"), new UTF8_Control());
	}
	 
	private static class I18nHolder {
		private final static I18n instance = new I18n();
	}

	private static I18n getInstance() {
		return I18nHolder.instance;
	}
	
	public static String getString(String key) throws IllegalArgumentException{
		String message = getInstance().bundle.getString(key);
		if(message != null){
			return message;
		}
		throw new IllegalArgumentException("Invalid translation key message ...");
	}
	
	/**
	 * Format a message with given object. Parse color
	 * @param message
	 * @param formatObject
	 * @return String
	 */
	public static String _(String message, Object[] formatObject){
		try{
			MessageFormat mf = new MessageFormat(getString(message));
			return ChatColor.translateAlternateColorCodes('&', mf.format(formatObject));
		}catch(IllegalArgumentException e){
			return "";
		}
	}
	
	/**
	 * Format a message with given object. Parse color
	 * @param message
	 * @param formatObject
	 * @return String
	 */
	public static String _(String message){
		try{
			return ChatColor.translateAlternateColorCodes('&', getString(message));
		}catch(IllegalArgumentException e){
			return "";
		}
	}
	
	/**
	 * Same as {@link #_(String, Object[])} except it adds a prefix
	 * @param message
	 * @param formatObject
	 * @return
	 */
	public static BaseComponent[] __(String message, Object[] formatObject){
		try{
			MessageFormat mf = new MessageFormat(getString(message));
			return BAT.__(mf.format(formatObject));
		}catch(IllegalArgumentException e){
			return TextComponent.fromLegacyText("");
		}
	}
	
	/**
	 * Same as {@link #_(String, Object[])} except it adds a prefix
	 * @param message
	 * @param formatObject
	 * @return
	 */
	public static BaseComponent[] __(String message){
		try{
			return BAT.__(getString(message));
		}catch(IllegalArgumentException e){
			return TextComponent.fromLegacyText("");
		}
	}
}