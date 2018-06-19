package net.b07z.sepia.server.assist.data;

import org.json.simple.JSONObject;

/**
 * Class for user specific personal stuff like favorites, relations (mother, father), etc. ...
 * 
 * @author Florian Quirin
 *
 */
public class PersonalInfo {
	
	//use this in contacts or favorites to save a description
	public static final String DESCRIPTION = "desc";
	
	//TODO: implement and use
	//TODO: use Word, Address, Contact ... 
	public String item = "";			//things like motorcycle, mother, restaurant, island
	public String description = "";		//description of the item like a short name to identify it. Will be shown first and maybe used for TTS.
	public String[] type;				//types like vehicle, person, location, location ... multiple types possible
	public JSONObject name;				//name of persons
	public JSONObject address;			//address of persons or locations
	public JSONObject more;				//details depending on type like {"model":"Suzuki GSX 1300 R"} or address JSON string 

}
