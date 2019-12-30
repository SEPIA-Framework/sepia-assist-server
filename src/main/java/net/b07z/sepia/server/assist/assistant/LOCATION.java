package net.b07z.sepia.server.assist.assistant;

import java.util.Map;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.answers.AnswerStatics;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.tools.GeoCoding;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Statics to get certain locations from account, user, geo tools etc.
 *  
 * @author Florian Quirin
 *
 */
public class LOCATION {

	public static final String LAT = InterviewData.LOCATION_LAT;
	public static final String LNG = InterviewData.LOCATION_LNG;
	public static final String STREET = InterviewData.LOCATION_STREET;
	public static final String STREET_NBR = InterviewData.LOCATION_STREET_NBR;
	public static final String CITY = InterviewData.LOCATION_CITY;
	public static final String POSTAL_CODE = InterviewData.LOCATION_POSTAL_CODE;
	public static final String STATE = InterviewData.LOCATION_STATE;
	public static final String COUNTRY = InterviewData.LOCATION_COUNTRY;
	public static final String NAME = InterviewData.LOCATION_NAME;
	public static final String IMAGE = InterviewData.LOCATION_IMAGE;
	public static final String ADDRESS_TEXT = InterviewData.LOCATION_ADDRESS_TEXT;
	
	/**
	 * Make a default location dataString (as used in user.user_location etc. ...). Unknown fields must be empty strings.  
	 * @param country - e.g. "Germany"
	 * @param state - e.g. "Bavaria"
	 * @param city - e.g. "Munich"
	 * @param code - e.g. "80331"
	 * @param street - e.g. "Platzl"
	 * @param street_number - e.g. "9"
	 * @param latitude - e.g. "48.137"
	 * @param longitude - e.g. "11.580"
	 * @return
	 */
	@Deprecated
	public static String makeLocationString(String country, String state, String city, String code,
									String street, String street_number, String latitude, String longitude){
		String dataString = "<" + COUNTRY + ">" +country+ "<" + STATE + ">" +state+ "<" + CITY + ">" +city+ "<" + POSTAL_CODE + ">" +code+
								"<" + STREET + ">" +street+ "<" + STREET_NBR + ">" +street_number+ "<" + LAT + ">" +latitude+ "<" + LNG + ">" +longitude;
		return dataString;
	}
	/**
	 * Make a default location dataString (as used in user.user_location etc. ...). Unknown fields must be empty strings.  
	 * @param country - e.g. "Germany"
	 * @param state - e.g. "Bavaria"
	 * @param city - e.g. "Munich"
	 * @param code - e.g. "80331"
	 * @param street - e.g. "Platzl"
	 * @param street_number - e.g. "9"
	 * @param latitude - e.g. "48.137"
	 * @param longitude - e.g. "11.580"
	 * @return
	 */
	public static JSONObject makeLocation(String country, String state, String city, String code,
									String street, String street_number, String latitude, String longitude){
		JSONObject locJson = new JSONObject();
		JSON.put(locJson, COUNTRY, country);
		JSON.put(locJson, STATE, state);
		JSON.put(locJson, CITY, city);
		JSON.put(locJson, POSTAL_CODE, code);
		JSON.put(locJson, STREET, street);
		JSON.put(locJson, STREET_NBR, street_number);
		JSON.put(locJson, LAT, latitude);
		JSON.put(locJson, LNG, longitude);
		return locJson;
	}
	
	/**
	 * Get full address of specific user location with a chosen delimiter ("," or ";" or " " ...) as you need.  
	 * @param user - user you are looking for 
	 * @param user_param - parameter found during "inputContainsUserLocation(..)" like &lt;user_home&gt;.
	 * @param delimiter - column, point, empty string ... whatever you need 
	 * @return address or empty string
	 */
	public static String getFullAddress(User user, String user_param, String delimiter){
		return getPartAddress(user, user_param, delimiter, STREET, STREET_NBR,
														POSTAL_CODE, CITY, STATE, COUNTRY);
	}
	/**
	 * Return pre-loaded full address of specific user location as JSON string.
	 * @param user - user with loaded data
	 * @param user_param - parameter like &lt;user_home&gt;
	 * @return JSONObject, fields can be empty
	 */
	public static JSONObject getFullAddressJSON(User user, String user_param){
		JSONObject jo = new JSONObject();
		String country = user.getUserSpecificLocation(user_param, COUNTRY);
		String state = user.getUserSpecificLocation(user_param, STATE);
		String city =  user.getUserSpecificLocation(user_param, CITY);
		String postal_code = user.getUserSpecificLocation(user_param, POSTAL_CODE);
		String street = user.getUserSpecificLocation(user_param, STREET);
		String street_nbr = user.getUserSpecificLocation(user_param, STREET_NBR);
		String lat = user.getUserSpecificLocation(user_param, LAT);
		String lng = user.getUserSpecificLocation(user_param, LNG);
		if (!country.isEmpty()) JSON.add(jo, COUNTRY, country);
		if (!state.isEmpty())	JSON.add(jo, STATE, state);
		if (!city.isEmpty())	JSON.add(jo, CITY, city);
		if (!postal_code.isEmpty()) 	JSON.add(jo, POSTAL_CODE, postal_code);
		if (!street.isEmpty())	JSON.add(jo, STREET, street);
		if (!street_nbr.isEmpty())		JSON.add(jo, STREET_NBR, street_nbr);
		if (!lat.isEmpty()) JSON.add(jo, LAT, lat);
		if (!lng.isEmpty()) JSON.add(jo, LNG, lng);
		return jo;
	}
	/**
	 * Get partial address of specific user location with a chosen delimiter ("," or ";" or " " ...) and chosen LOCATIONs as you need.  
	 * @param user - user you are looking for 
	 * @param user_param - parameter found during "inputContainsUserLocation(..)" like &#60;user_home&#62;.
	 * @param delimiter - column, point, empty string ... whatever you need
	 * @param keys... - LOCATIONs to put into string in the order they appear
	 * @return address or empty string
	 */
	public static String getPartAddress(User user, String user_param, String delimiter, String... keys){
		if (!delimiter.equals(" "))
			delimiter = delimiter.trim();
		String address = "";
		String previous_key = "";
		String previous_param = "";
		for (String s : keys){
			String param = user.getUserSpecificLocation(user_param, s);
			if (s.equals(STREET_NBR) && !s.isEmpty() && previous_key.equals(STREET) && !previous_param.isEmpty()){
				//there should be no delimiter between street and number
				address = address.trim().replaceFirst(Pattern.quote(delimiter) + "$", " ");
				address += param + delimiter + " ";
			}else{
				address += (!param.isEmpty())? (param + delimiter + " ") : "";
			}
			previous_key = s;
			previous_param = param;
		}
		address = address.trim().replaceFirst(Pattern.quote(delimiter) + "$", "").replaceAll("\\s+", " ").trim();
		return address;
	}
	
	/**
	 * Check if this location parameter can be added to the user account. Typically things like &#60;user_home&#62; etc. can.
	 * @param user_param - user specific location parameter (like &#60;user_home&#62; ...)
	 * @return true/false
	 */
	public static boolean canBeAddedToAccount(String user_param){
		if (NluTools.stringContains(user_param, "<user_home>|<user_work>")){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * Get more info about an address by searching the database (aka a geocoder API).
	 * @param searchTerm - hint of the address in any form
	 * @param nluInput - {@link NluInput} object
	 */
	public static JSONObject getInfoBySearch(String searchTerm, NluInput nluInput){
		if (GeoCoding.isSupported()){
			JSONObject jo = new JSONObject();
			Map<String, Object> result = GeoCoding.getCoordinates(searchTerm, nluInput.language);
			if (result.get(LAT) != null)	JSON.add(jo, LAT, result.get(LAT));
			if (result.get(LNG) != null)	JSON.add(jo, LNG, result.get(LNG));
			if (result.get(STREET_NBR) != null)	JSON.add(jo, STREET_NBR, result.get(STREET_NBR));
			if (result.get(STREET) != null)		JSON.add(jo, STREET, result.get(STREET));
			if (result.get(POSTAL_CODE) != null)JSON.add(jo, POSTAL_CODE, result.get(POSTAL_CODE));
			if (result.get(CITY) != null)		JSON.add(jo, CITY, result.get(CITY));
			if (result.get(STATE) != null)		JSON.add(jo, STATE, result.get(STATE));
			if (result.get(COUNTRY) != null)	JSON.add(jo, COUNTRY, result.get(COUNTRY));
			return jo;
		}else{
			return null;
		}
	}
	
	/**
	 * Check for a user specific location like &#60;user_home&#62;, if there is one get the location's longitude, latitude.
	 * If they are not available get the full address (COUNTRY, CITY, STREET, ...) and load partial address (CITY, STREET, STREET_NBR)
	 * as "location to say" (we don't want to speak coordinates or too long addresses). Parameter, location and location_to_say are packed in a string array (0,1,2).
	 * @param user - user to check
	 * @param input - input to search for user location parameter
	 * @return String array, 0-parameter (&#60;user_home&#62;, ...), 1-location (to search for, delimiter: comma), 2-location_to_say (speak that one) 
	 */
	public static String[] getUserSpecificLocation_4_Maps(User user, String input){
		String user_param = User.containsUserSpecificLocation(input, user);
		String location = "";
		String location_to_say = "";
		if (!user_param.isEmpty()){
			//check latitude longitude
			location = getPartAddress(user, user_param, "," , LAT, LNG);
			location_to_say = getPartAddress(user, user_param, ",", STREET, STREET_NBR, CITY);
			//only latitude, longitude but no name for current location?
			if (user_param.equals("<user_location>") && location_to_say.isEmpty() && !location.isEmpty()){
				location_to_say = AnswerStatics.get(AnswerStatics.HERE, user.language);
			}
			//if this is empty check full address
			if (location.isEmpty()){
				location = getFullAddress(user, user_param, ",");
			}
		}
		return new String[]{user_param, location, location_to_say};
	}
	/**
	 * Check for a user specific location like &#60;user_home&#62;, if there is one get the location's street, city and country.
	 * Get the partial address (CITY, STREET) as "location to say". Please add the POI to strings manually.
	 * Parameter, location and location_to_say are packed in a string array (0,1,2).
	 * @param user - user to check
	 * @param input - input to search for user location parameter
	 * @return String array, 0-parameter (&#60;user_home&#62;, ...), 1-location (to search for, delimiter: comma), 2-location_to_say (speak that one) 
	 */
	public static String[] getUserSpecificLocation_4_Maps_with_POI(User user, String input){
		String user_param = User.containsUserSpecificLocation(input, user);
		String location = "";
		String location_to_say = "";
		if (!user_param.isEmpty()){
			//check latitude longitude
			location = getPartAddress(user, user_param, ",", STREET, CITY, COUNTRY);
			location_to_say = getPartAddress(user, user_param, ",", STREET, CITY);
			//no name for current location?
			if (user_param.equals("<user_location>") && location_to_say.isEmpty()){
				location_to_say = AnswerStatics.get(AnswerStatics.HERE, user.language);
			}
		}
		return new String[]{user_param, location, location_to_say};
	}
	/**
	 * Check for a user specific location like &#60;user_home&#62;, if there is one get a partial address (CITY, STREET, STREET_NBR) and load partial address (CITY, STREET, STREET_NBR)
	 * as "location to say" (we don't want to speak too long addresses). Parameter, location and location_to_say are packed in a string array (0,1,2).
	 * @param user - user to check
	 * @param input - input to search for user location parameter
	 * @return String array, 0-parameter (&#60;user_home&#62;, ...), 1-location (to search for, delimiter: comma), 2-location_to_say (speak that one) 
	 */
	public static String[] getUserSpecificLocation_4_Mobility(User user, String input){
		String user_param = User.containsUserSpecificLocation(input, user);
		String location = "";
		String location_to_say = "";
		if (!user_param.isEmpty()){
			//check latitude longitude
			location = getPartAddress(user, user_param, ",", CITY, STREET, STREET_NBR);
			//location = getFullAddress(user, user_param, ",");
			location_to_say = getPartAddress(user, user_param, ",", STREET, STREET_NBR, CITY);
			//only latitude, longitude but no name for current location?
			if (user_param.equals("<user_location>") && location_to_say.isEmpty() && !location.isEmpty()){
				location_to_say = AnswerStatics.get(AnswerStatics.HERE, user.language);
			}
		}
		return new String[]{user_param, location, location_to_say};
	}
	/**
	 * Check for a user specific location like &#60;user_home&#62;, if there is one get the location's longitude, latitude.
	 * Load partial address (CITY) as "location to say" (we don't want to speak coordinates or too long addresses).
	 * Parameter, location and location_to_say are packed in a string array (0,1,2).
	 * @param user - user to check
	 * @param input - input to search for user location parameter
	 * @return String array, 0-parameter (&#60;user_home&#62;, ...), 1-location (to search for, delimiter: comma), 2-location_to_say (speak that one) 
	 */
	public static String[] getUserSpecificLocation_4_Weather(User user, String input){
		String user_param = User.containsUserSpecificLocation(input, user);
		String location = "";
		String location_to_say = "";
		if (!user_param.isEmpty()){
			//check latitude longitude
			location = getPartAddress(user, user_param, "," , LAT, LNG);
			location_to_say = getPartAddress(user, user_param, ",", CITY);
			//only latitude, longitude but no name for current location?
			if (user_param.equals("<user_location>") && location_to_say.isEmpty() && !location.isEmpty()){
				location_to_say = AnswerStatics.get(AnswerStatics.HERE, user.language);
			}
		}
		return new String[]{user_param, location, location_to_say};
	}
	/**
	 * Check for a user specific location like &#60;user_home&#62;, if there is one get the location's city.
	 * Load partial address (CITY) as "location to say" as well.
	 * Parameter, location and location_to_say are packed in a string array (0,1,2).
	 * @param user - user to check
	 * @param input - input to search for user location parameter
	 * @return String array, 0-parameter (&#60;user_home&#62;, ...), 1-location (to search for, delimiter: comma), 2-location_to_say (speak that one) 
	 */
	public static String[] getUserSpecificLocation_4_Flights(User user, String input){
		String user_param = User.containsUserSpecificLocation(input, user);
		String location = "";
		String location_to_say = "";
		if (!user_param.isEmpty()){
			//check latitude longitude
			location = getPartAddress(user, user_param, "," , CITY);
			location_to_say = getPartAddress(user, user_param, ",", CITY);
			//only latitude, longitude but no name for current location?
			if (user_param.equals("<user_location>") && location_to_say.isEmpty() && !location.isEmpty()){
				location_to_say = AnswerStatics.get(AnswerStatics.HERE, user.language);
			}
		}
		return new String[]{user_param, location, location_to_say};
	}
}
