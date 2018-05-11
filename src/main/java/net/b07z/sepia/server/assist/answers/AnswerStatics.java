package net.b07z.sepia.server.assist.answers;

import java.util.HashMap;

import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Static answers for different, language specific objects/terms etc. ...
 * 
 * @author Florian Quirin
 *
 */
public class AnswerStatics {
	
	public static final String UNCONFIRMED_PLACE_WEATHER = "unconfirmed_place_weather";
	public static final String UNCONFIRMED_COUNTRY = "unconfirmed_country";
	public static final String HERE = "here";
	public static final String HOME = "home";
	public static final String WORK = "work";
	public static final String AND = "and";
	public static final String CLOSE_TO = "close_to";
	public static final String UNKNOWN = "unknown";
	
	private static HashMap<String, String> list_en = new HashMap<String, String>();
	private static HashMap<String, String> list_de = new HashMap<String, String>();
	static{
		//German
		list_de.put(UNCONFIRMED_PLACE_WEATHER, "unbestätigter Ort");
		list_de.put(UNCONFIRMED_COUNTRY, "unbestätigtes Land");
		list_de.put(HERE, "hier");
		list_de.put(HOME, "zu Hause");
		list_de.put(WORK, "Arbeit");
		list_de.put(AND, "und");
		list_de.put(CLOSE_TO, "in der Nähe von");
		list_de.put(UNKNOWN, "unbekannt");
		
		//English
		list_en.put(UNCONFIRMED_PLACE_WEATHER, "unconfirmed place");
		list_en.put(UNCONFIRMED_COUNTRY, "unconfirmed country");
		list_en.put(HERE, "here");
		list_en.put(HOME, "home");
		list_en.put(WORK, "work");
		list_en.put(AND, "and");
		list_en.put(CLOSE_TO, "close to");
		list_en.put(UNKNOWN, "unknown");
	}

	/**
	 * Get language specific object/word/term etc. for key.
	 * @param key - the thing you are looking for (see static strings in this class)
	 * @param language - language code ("en", "de", etc.)
	 * @return
	 */
	public static String get(String key, String language){
		String result = "";
		if (language.toLowerCase().matches("de")){
			result = list_de.get(key);
			
		}else if (language.toLowerCase().matches("en")){
			result = list_en.get(key);
			
		}else{
			result = list_en.get(key);
			Debugger.println("Missing local expression in '" + language + "' for '" + key + "'", 1);
			
		}
		return result;
	}
}
