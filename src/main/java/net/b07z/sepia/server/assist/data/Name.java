package net.b07z.sepia.server.assist.data;

import java.util.Map;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.core.tools.JSON;

/**
 * Class representing a user's name.
 * 
 * @author Florian Quirin
 *
 */
public class Name {
	public static final String FIRST = "first";
	public static final String LAST = "last";
	public static final String NICK = "nick";
	
	public String first = "";
	public String last = "";
	public String nick = "Boss";
	JSONObject jsonName;
	
	/**
	 * Create new Name object. Use this constructor whenever you can to make sure you have the correct variables. 
	 * @param first - first name
	 * @param last - last name
	 * @param nick - nick name
	 */
	public Name(String first, String last, String nick){
		this.first = first;
		this.last = last;
		this.nick = nick;
	}
	/**
	 * Create default Name object.
	 */
	public Name(){}
	/**
	 * Use this if you have a name-map and you KNOW! that it has the correct format.
	 */
	public Name(Map<String, Object> map){
		this.first = (String) map.get(FIRST);
		this.last = (String) map.get(LAST);
		this.nick = (String) map.get(NICK);
	}
	
	/**
	 * Make sure all entries are free of malicious code.
	 * @param json - JSONObject representing name
	 * @return cleaned up JSON
	 */
	public static JSONObject cleanUpNameJson(JSONObject json){
		for (Object kObj : json.keySet()){
			String entry = JSON.getStringOrDefault(json, (String) kObj, "");
			//JSON.put(json, (String) kObj, Converters.escapeHTML(entry));
			JSON.put(json, (String) kObj, entry.replaceAll("[^\\w\\s#\\-\\+&]","").replaceAll("\\s+", " ").trim());
		}
		return json;
	}
	
	@Override
	public String toString(){
		return buildJSON().toJSONString();
	}
	
	/**
	 * Build a JSONObject from data in class. Field can be null. 
	 */
	public JSONObject buildJSON(){
		jsonName = new JSONObject();
		JSON.put(jsonName, FIRST, this.first);
		JSON.put(jsonName, LAST, this.last);
		JSON.put(jsonName, NICK, this.nick);
		return jsonName;
	}
}
