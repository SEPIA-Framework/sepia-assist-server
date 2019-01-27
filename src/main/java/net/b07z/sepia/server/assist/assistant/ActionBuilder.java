package net.b07z.sepia.server.assist.assistant;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.core.tools.JSON;

/**
 * Class to build actions for services (e.g. for buttons etc.).
 * 
 * @author Florian Quirin
 *
 */
public class ActionBuilder {

	/**
	 * Make a simple action with just one key-value pair. 
	 * @param type - ACTIONS.xyt action type
	 * @param key - key of the only entry
	 * @param value - value of the only entry
	 * @return JSONArray with one action
	 */
	public static JSONArray makeSimpleAction(String type, String key, String value){
		JSONArray ja = new JSONArray();
		JSONObject jo = new JSONObject();
		JSON.put(jo, "type", type);
		JSON.put(jo, key, value);
		JSON.add(ja, jo);
		return ja;
	}
	/**
	 * Make a simple action with added JSON data. 
	 * @param type - ACTIONS.xyt action type
	 * @param json - JSON object to add. NOTE: the key "type" may not be used and will be overwritten by the ACTION type!
	 * @return JSONArray with action
	 */
	public static JSONArray makeSimpleAction(String type, JSONObject json){
		JSONArray ja = new JSONArray();
		JSONObject jo = json;
		JSON.put(jo, "type", type);
		JSON.add(ja, jo);
		return ja;
	}

	/**
	 * Get the default "Click here to open" text in local version.
	 * @param language - language code
	 */
	public static String getDefaultButtonText(String language){
		if (language.equals(LANGUAGES.DE)){
			return "Zum Öffnen bitte hier klicken";
		}else{
			return "Click here to open";
		}
	}

}
