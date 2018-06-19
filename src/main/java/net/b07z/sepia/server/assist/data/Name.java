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
	 * @param first - first name
	 * @param last - last name
	 * @param nick - nick name
	 */
	public Name(){}
	/**
	 * Use this if you have a name-JSONObject and you KNOW! that it has the correct format.
	 */
	public Name(JSONObject jo){
		jsonName = jo;
		this.first = (String) jo.get(FIRST);
		this.last = (String) jo.get(LAST);
		this.nick = (String) jo.get(NICK);
	}
	/**
	 * Use this if you have a name-map and you KNOW! that it has the correct format.
	 */
	public Name(Map<String, Object> map){
		this.first = (String) map.get(FIRST);
		this.last = (String) map.get(LAST);
		this.nick = (String) map.get(NICK);
	}
	
	@Override
	public String toString(){
		return ("First: " + first + ", Last: " + last + ", Nick: " + nick);
	}
	
	/**
	 * Build a JSONObject from data in class. Field can be null. 
	 */
	public JSONObject buildJSON(){
		jsonName = new JSONObject();
		JSON.add(jsonName, FIRST, this.first);
		JSON.add(jsonName, LAST, this.last);
		JSON.add(jsonName, NICK, this.nick);
		return jsonName;
	}
}
