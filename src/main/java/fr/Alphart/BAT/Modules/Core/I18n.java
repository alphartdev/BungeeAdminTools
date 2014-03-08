package fr.Alphart.BAT.Modules.Core;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Utils.FormatUtils;

public class I18n{
	ResourceBundle bundle;
	
	private I18n(){
		bundle = ResourceBundle.getBundle("TODO", new Control(){
		@Override
	    public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException, IOException
	    {
	        String bundleName = toBundleName(baseName, locale);
	        String resourceName = toResourceName(bundleName, "properties");
	        ResourceBundle bundle = null;
	        InputStream stream = null;
	        if (reload) {
	            URL url = loader.getResource(resourceName);
	            if (url != null) {
	                URLConnection connection = url.openConnection();
	                if (connection != null) {
	                    connection.setUseCaches(false);
	                    stream = connection.getInputStream();
	                }
	            }
	        } else {
	            stream = loader.getResourceAsStream(resourceName);
	        }
	        if (stream != null) {
	            try {
	                bundle = new PropertyResourceBundle(new InputStreamReader(stream, "UTF-8"));
	            } finally {
	                stream.close();
	            }
	        }
	        return bundle;
	    }
		});		
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
	 * @return BaseComponent array
	 */
	public static BaseComponent[] _(String message, Object[] formatObject){
		try{
			MessageFormat mf = new MessageFormat(getString(message));
			return FormatUtils._(mf.format(formatObject));
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
	public static BaseComponent[] __(String message, Object[] formatObject){
		try{
			MessageFormat mf = new MessageFormat(getString(message));
			return BAT.__(mf.format(formatObject));
		}catch(IllegalArgumentException e){
			return TextComponent.fromLegacyText("");
		}
	}
}