package net.b07z.sepia.server.assist.tools;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LOCATION;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.URLBuilder;

/**
 * Different tools to do geo-coding.
 * 
 * @author Florian Quirin
 *
 */
public class GeoCoding {
	
	//engines - TODO: use 'Config.default_geo_api'
	public static final String OSM = "osm";						//TODO: implement
	public static final String GOOGLE = "google";
	public static final String GRAPHHOPPER = "graphhopper"; 	//TODO: add missing features
	
	/**
	 * Is geo-coding supported?
	 */
	public static boolean isSupported(){
		//return graphhopper_is_supported();
		return googlemaps_is_supported();
	}
	
	/**
	 * Default method to get latitude, longitude from address name. 
	 * @param address - string with an address, optimally Street, City, Country
	 * @return Map with different parameters ("latitude", "longitude", ...), see LOCATION class statics.
	 */
	public static Map<String, Object> getCoordinates(String address, String language){
		Map<String, Object> result = new HashMap<>();
		//different implementations:
		//result = graphhopper_get_coordinates(address, language);
		result = google_get_coordinates(address, language);
		//result = our_own_get_coordinates(address, language);
		//...
		//TODO: write result to database?
		return result;
	}
	
	/**
	 * Default method to get an address from latitude longitude coordinates. 
	 * @param latitude - e.g. 51.45
	 * @param longitude - e.g. 7.02
	 * @return Map with different parameters ("street", "city", "country", ...), see LOCATION class statics.
	 */
	public static Map<String, Object> getAddress(String latitude, String longitude, String language){
		Map<String, Object> result = new HashMap<>();
		//different implementations:
		//result = graphhopper_get_address(latitude, longitude, language);
		result = google_get_address(latitude, longitude, language);
		//result = our_own_get_address(latitude, longitude, language);
		//...
		//TODO: write result to database?
		return result;
	}
	
	/**
	 * Get a POI (point-of-interest) instead of doing a "normal" address search. Can be specified with certain "types" like "food" (empty means no restriction).
	 * @param search - search parameter in the style of "restaurants in Berlin"
	 * @param types - POI types like "food", "hospital", "lodging" etc. ... can be a list food|restaurants|...
	 * @param latitude - latitude GPS coordinate or empty.
	 * @param longitude - longitude GPS coordinate or empty. If both lat. and lng. are not empty a 15km radius is applied for the search 
	 * @param language - language code for specific region output
	 */
	public static JSONArray getPOI(String search, String types, String latitude, String longitude, String language){
		return google_get_POI(search, latitude, longitude, language, types);
	}
	
	/**
	 * Test get_coordinates output of all available methods.
	 * @param address - user input when asking for an address
	 * @param language - language code
	 */
	public static void test_get_coordinates(String address, String language){
		Map<String, Object> result = new HashMap<String, Object>();
		//different implementations:
		if (graphhopper_is_supported()){
			result = graphhopper_get_coordinates(address, language);
			System.out.println("------GRAPHHOPPER-------");
			System.out.println("Coordinates: " + result.get(LOCATION.LAT) + ", "+ result.get(LOCATION.LNG));
			System.out.println("Street Nbr.: " + result.get(LOCATION.STREET_NBR));
			System.out.println("Street: " + result.get(LOCATION.STREET));
			System.out.println("Postal Code: " + result.get(LOCATION.POSTAL_CODE));
			System.out.println("City: " + result.get(LOCATION.CITY));
			System.out.println("State: " + result.get(LOCATION.STATE));
			System.out.println("Country: " + result.get(LOCATION.COUNTRY));
		}else{
			System.out.println("Graphhopper is missing API key!");
		}
		if (googlemaps_is_supported()){
			result = google_get_coordinates(address, language);
			System.out.println("------GOOGLE-------");
			System.out.println("Coordinates: " + result.get(LOCATION.LAT) + ", "+ result.get(LOCATION.LNG));
			System.out.println("Street Nbr.: " + result.get(LOCATION.STREET_NBR));
			System.out.println("Street: " + result.get(LOCATION.STREET));
			System.out.println("Postal Code: " + result.get(LOCATION.POSTAL_CODE));
			System.out.println("City: " + result.get(LOCATION.CITY));
			System.out.println("State: " + result.get(LOCATION.STATE));
			System.out.println("Country: " + result.get(LOCATION.COUNTRY));
		}else{
			System.out.println("Googlemaps is missing API key!");
		}
	}
	/**
	 * Test get_address output of all available methods.
	 * @param latitude - latitude coordinate to search for
	 * @param longitude - longitude coordinate to search for
	 * @param language - language code
	 */
	public static void test_get_address(String latitude, String longitude, String language){
		HashMap<String, Object> result = new HashMap<String, Object>();
		//different implementations:
		result = graphhopper_get_address(latitude, longitude, language);
		System.out.println("------GRAPHHOPPER-------");
		System.out.println("Coordinates: " + result.get(LOCATION.LAT) + ", "+ result.get(LOCATION.LNG));
		System.out.println("Street Nbr.: " + result.get(LOCATION.STREET_NBR));
		System.out.println("Street: " + result.get(LOCATION.STREET));
		System.out.println("Postal Code: " + result.get(LOCATION.POSTAL_CODE));
		System.out.println("City: " + result.get(LOCATION.CITY));
		System.out.println("State: " + result.get(LOCATION.STATE));
		System.out.println("Country: " + result.get(LOCATION.COUNTRY));
		result = google_get_address(latitude, longitude, language);
		System.out.println("------GOOGLE-------");
		System.out.println("Coordinates: " + result.get(LOCATION.LAT) + ", "+ result.get(LOCATION.LNG));
		System.out.println("Street Nbr.: " + result.get(LOCATION.STREET_NBR));
		System.out.println("Street: " + result.get(LOCATION.STREET));
		System.out.println("Postal Code: " + result.get(LOCATION.POSTAL_CODE));
		System.out.println("City: " + result.get(LOCATION.CITY));
		System.out.println("State: " + result.get(LOCATION.STATE));
		System.out.println("Country: " + result.get(LOCATION.COUNTRY));
	}
	
	//----private methods----
	
	private static boolean graphhopper_is_supported(){
		//requirements
		return !Is.nullOrEmpty(Config.graphhopper_key);
	}
	
	//Graphhopper implementation for get_coordinates
	private static HashMap<String, Object> graphhopper_get_coordinates(String address, String language){
		//requirements
		if (Is.nullOrEmpty(Config.graphhopper_key)){
			Debugger.println("GeoCoding - Graphhopper API-key is missing! Please add one via the config file to use the service.", 1);
			return null;
		}
		HashMap<String, Object> result = new HashMap<String, Object>();
		try {
			String add_params = "&debug=false&limit=1&key=" + Config.graphhopper_key;
			String url = "https://graphhopper.com/api/1/geocode?q=" + URLEncoder.encode(address, "UTF-8") + "&locale=" + language + add_params;
			//System.out.println("gh-url: " + url); 	//debug
			
			long tic = System.currentTimeMillis();
			JSONObject response = Connectors.httpGET(url.trim());
			Statistics.addExternalApiHit("Graphhopper Geocoder");
			Statistics.addExternalApiTime("Graphhopper Geocoder", tic);
			JSONArray hits = (JSONArray) response.get("hits");
			if (!hits.isEmpty()){
				JSONObject points = (JSONObject) ((JSONObject) hits.get(0)).get("point");
				double latitude = (double) points.get("lat");
				double longitude = (double) points.get("lng");
				String osm_value = (String) ((JSONObject) hits.get(0)).get("osm_value");
				String country, state, city, street, postcode;
				if (osm_value.matches("country"))	country = (String) ((JSONObject) hits.get(0)).get("name");
				else								country = (String) ((JSONObject) hits.get(0)).get("country");
				if (osm_value.matches("state"))		state = (String) ((JSONObject) hits.get(0)).get("name");
				else 								state = (String) ((JSONObject) hits.get(0)).get("state");
				if (osm_value.matches("(city|town|village)"))	city = (String) ((JSONObject) hits.get(0)).get("name");
				else											city = (String) ((JSONObject) hits.get(0)).get("city");
				if (osm_value.matches("(residential|footway)"))	street = (String) ((JSONObject) hits.get(0)).get("name");
				else											street = (String) ((JSONObject) hits.get(0)).get("street");
				postcode = (String) ((JSONObject) hits.get(0)).get("postcode");
				
				//fill result
				result.put(LOCATION.LAT, latitude);		result.put(LOCATION.LNG, longitude);
				result.put(LOCATION.COUNTRY, country);	result.put(LOCATION.STATE, state);		result.put(LOCATION.POSTAL_CODE, postcode);			
				result.put(LOCATION.CITY, city);		result.put(LOCATION.STREET, street);		result.put(LOCATION.STREET_NBR, null);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
			/*
			result.put(LOCATION.LAT, null);			result.put(LOCATION.LNG, null);
			result.put(LOCATION.COUNTRY, null);		result.put(LOCATION.STATE, null);		result.put(LOCATION.POSTAL_CODE, null);			
			result.put(LOCATION.CITY, null);		result.put(LOCATION.STREET, null);		result.put(LOCATION.STREET_NBR, null);
			*/
		}
		return result;
	}
	
	//Graphhopper implementation for get_address
	private static HashMap<String, Object> graphhopper_get_address(String latitude, String longitude, String language) {
		//requirements
		if (Is.nullOrEmpty(Config.graphhopper_key)){
			Debugger.println("GeoCoding - Graphhopper API-key is missing! Please add one via the config file to use the service.", 1);
			return null;
		}
		// TODO implement
		/*
		Statistics.addExternalApiHit("Graphhopper GetAddress");
		Statistics.addExternalApiTime("Graphhopper GetAddress", tic);
		 */
		HashMap<String, Object> result = new HashMap<String, Object>();
		//copy from input
		result.put(LOCATION.LAT, latitude);		
		result.put(LOCATION.LNG, longitude);
		
		result.put(LOCATION.COUNTRY, null);		result.put(LOCATION.STATE, null);		result.put(LOCATION.POSTAL_CODE, null);			
		result.put(LOCATION.CITY, null);		result.put(LOCATION.STREET, null);		result.put(LOCATION.STREET_NBR, null);
		return result;
	}
	
	private static boolean googlemaps_is_supported(){
		//requirements
		return !Is.nullOrEmpty(Config.google_maps_key);
	}

	//Google implementation for get_coordinates
	private static Map<String, Object> google_get_coordinates(String address, String language){
		//requirements
		if (Is.nullOrEmpty(Config.google_maps_key)){
			Debugger.println("GeoCoding - Google API-key is missing! Please add one via the config file to use Maps.", 1);
			return null;
		} 
		Map<String, Object> result = new HashMap<String, Object>();
		try {
			String add_region = "&region=" + language + "&language=" + language;		//TODO: split region and language?
			String add_key = "";
			if (!Config.google_maps_key.equals("test")){
				add_key = "&key=" + Config.google_maps_key;
			}
			String url = "https://maps.googleapis.com/maps/api/geocode/json?address=" 
					+ URLEncoder.encode(address, "UTF-8") 
					+ add_region
					//+ "&bounds=51.361,6.978|51.561,7.178"
					+ add_key; 	//TODO: add the "near to" options
			
			//System.out.println("NAVI: " + url);
			
			//make the HTTP GET call to Google Geocode API
			long tic = System.currentTimeMillis();
			JSONObject response = Connectors.httpGET(url.trim());
			JSONArray results = (JSONArray) response.get("results");
			Statistics.addExternalApiHit("Google Geocoder");
			Statistics.addExternalApiTime("Google Geocoder", tic);
			
			// possible status-modes from Google-Geocoding:
			// "OK"; // "ZERO_RESULTS"; // "OVER_QUERY_LIMIT"; // "REQUEST_DENIED"; // (result_type or location_type); // "INVALID_REQUEST"; // "UNKNOWN_ERROR";
			//TODO: handle over_query_limit with a) 1s break b) 2s break c) possible quota end
			
			//if (status_results.equals("OK")) {	
			if (!results.isEmpty()){
				JSONObject geometry = (JSONObject) ((JSONObject) results.get(0)).get("geometry"); 
				JSONObject location = (JSONObject) geometry.get("location");

				double latitude = (double) location.get("lat");
				double longitude = (double) location.get("lng");
																
				// get the city and country from formatted_address
				//String formatted_address = (String) ((JSONObject) results.get(0)).get("formatted_address"); // e.g. Faro District, Portugal
				//String[] parts = formatted_address.split(","); 
				//city = parts[parts.length - 2]; 
				//country = parts[parts.length - 1]; // in the last part of formatted_address is the country
				
				JSONArray address_components = (JSONArray) ((JSONObject) results.get(0)).get("address_components"); 
				String city = null, country = null, postcode = null, street = null, state = null, street_nbr = null;
				int i = 0;
				for (Object obj : address_components)
				{
					String currentComponent = ((JSONObject) obj).get("types").toString();
					
					// "types" : [ "street_number" ]
					if (currentComponent.contains("\"street_number\""))
					{
						street_nbr = (String) ((JSONObject) address_components.get(i)).get("long_name"); 		
					}
					// "types" : [ "route" ]
					else if (currentComponent.contains("\"route\""))
					{
						street = (String) ((JSONObject) address_components.get(i)).get("long_name"); 		
					}
					// search for GoogleOutput:		"types" : [ "locality", "political" ]
					else if (currentComponent.contains("\"locality\""))
					{
						city = (String) ((JSONObject) address_components.get(i)).get("long_name"); 		
					}
					// "types" : [ "sublocality", "political" ] -- best replacement for city ???
					else if (currentComponent.contains("\"sublocality\""))
					{
						if (city == null)
							city = (String) ((JSONObject) address_components.get(i)).get("long_name"); 		
					}
					// "types" : [ "administrative_area_level_1", "political" ] -- it should be the state 
					else if (currentComponent.contains("\"administrative_area_level_1\""))
					{
						state = (String) ((JSONObject) address_components.get(i)).get("long_name"); 		
					}
					// search for GoogleOutput:		"types" : [ "country", "political" ]
					else if (currentComponent.contains("\"country\""))
					{
						country = (String) ((JSONObject) address_components.get(i)).get("long_name"); 
					}
					// search for GoogleOutput:		"types" : [ "postal_code" ]
					else if (currentComponent.contains("\"postal_code\""))
					{
						postcode = (String) ((JSONObject) address_components.get(i)).get("long_name"); 
					}
					
					i++;
				}
				String formatted_address = (String) ((JSONObject) results.get(0)).get("formatted_address");
				//fill result
				result.put(LOCATION.NAME, formatted_address.replaceFirst(",.*?$", "").trim());
				result.put(LOCATION.LAT, latitude);		result.put(LOCATION.LNG, longitude);
				result.put(LOCATION.COUNTRY, country);	result.put(LOCATION.STATE, state);		result.put(LOCATION.POSTAL_CODE, postcode);			
				result.put(LOCATION.CITY, city);		result.put(LOCATION.STREET, street);		result.put(LOCATION.STREET_NBR, street_nbr);
			}
			
		}catch (Exception e) {
			e.printStackTrace();
			return null;
			/*
			result.put(LOCATION.LAT, null);			result.put(LOCATION.LNG, null);
			result.put(LOCATION.COUNTRY, null);		result.put(LOCATION.STATE, null);		result.put(LOCATION.POSTAL_CODE, null);			
			result.put(LOCATION.CITY, null);		result.put(LOCATION.STREET, null);		result.put(LOCATION.STREET_NBR, null);
			*/
		}
		return result;
	}
	
	//Google implementation for get_address
	private static HashMap<String, Object> google_get_address(String latitude, String longitude, String language) {
		//requirements
		if (Is.nullOrEmpty(Config.google_maps_key)){
			Debugger.println("GeoCoding - Google API-key is missing! Please add one via the config file to use Maps.", 1);
			return null;
		}
		HashMap<String, Object> result = new HashMap<String, Object>();
		try {
			String add_key = "";
			if (!Config.google_maps_key.equals("test")){
				add_key = "&key=" + Config.google_maps_key;
			}
			String add_region = "&region=" + language + "&language=" + language;		//TODO: split region and language?
			String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" 
					+ latitude + "," + longitude 
					+ add_region + add_key;
			
			//make the HTTP GET call to Google Geocode API
			long tic = System.currentTimeMillis();
			JSONObject response = Connectors.httpGET(url.trim());
			JSONArray results = (JSONArray) response.get("results");
			Statistics.addExternalApiHit("Google Geocoder (reverse)");
			Statistics.addExternalApiTime("Google Geocoder (reverse)", tic);
			
			//TODO: handle over_query_limit with a) 1s break b) 2s break c) possible quota end
			
			if (!results.isEmpty()){
				// get the city and country from formatted_address
				//String formatted_address = (String) ((JSONObject) results.get(0)).get("formatted_address"); // e.g. Faro District, Portugal
				//String[] parts = formatted_address.split(","); 
				//city = parts[parts.length - 2]; 
				//country = parts[parts.length - 1]; // in the last part of formatted_address is the country
				
				JSONArray address_components = (JSONArray) ((JSONObject) results.get(0)).get("address_components"); 
				String city = null, country = null, postcode = null, street = null, state = null, street_nbr = null;
				int i = 0;
				for (Object obj : address_components)
				{
					String currentComponent = ((JSONObject) obj).get("types").toString();

					// "types" : [ "street_number" ]
					if (currentComponent.contains("\"street_number\""))
					{
						street_nbr = (String) ((JSONObject) address_components.get(i)).get("long_name"); 		
					}
					// "types" : [ "route" ]
					else if (currentComponent.contains("\"route\""))
					{
						street = (String) ((JSONObject) address_components.get(i)).get("long_name"); 		
					}
					// search for GoogleOutput:		"types" : [ "locality", "political" ]
					else if (currentComponent.contains("\"locality\""))
					{
						city = (String) ((JSONObject) address_components.get(i)).get("long_name"); 		
					}
					// "types" : [ "sublocality", "political" ] -- best replacement for city ???
					else if (currentComponent.contains("\"sublocality\""))
					{
						if (city == null)
							city = (String) ((JSONObject) address_components.get(i)).get("long_name"); 		
					}
					// "types" : [ "administrative_area_level_1", "political" ] -- it should be the state 
					else if (currentComponent.contains("\"administrative_area_level_1\""))
					{
						state = (String) ((JSONObject) address_components.get(i)).get("long_name"); 		
					}
					// search for GoogleOutput:		"types" : [ "country", "political" ]
					else if (currentComponent.contains("\"country\""))
					{
						country = (String) ((JSONObject) address_components.get(i)).get("long_name"); 
					}
					// search for GoogleOutput:		"types" : [ "postal_code" ]
					else if (currentComponent.contains("\"postal_code\""))
					{
						postcode = (String) ((JSONObject) address_components.get(i)).get("long_name"); 
					}
					
					i++;
				} 
				String formatted_address = (String) ((JSONObject) results.get(0)).get("formatted_address");
				//fill result
				result.put(LOCATION.NAME, formatted_address.replaceFirst(",.*?$", "").trim());
				result.put(LOCATION.LAT, latitude);		result.put(LOCATION.LNG, longitude);
				result.put(LOCATION.COUNTRY, country);	result.put(LOCATION.STATE, state);		result.put(LOCATION.POSTAL_CODE, postcode);			
				result.put(LOCATION.CITY, city);		result.put(LOCATION.STREET, street);		result.put(LOCATION.STREET_NBR, street_nbr);
			}
			
		}catch (Exception e) {
			e.printStackTrace();
			return null;
			/*
			result.put(LOCATION.LAT, null);			result.put(LOCATION.LNG, null);
			result.put(LOCATION.COUNTRY, null);		result.put(LOCATION.STATE, null);		result.put(LOCATION.POSTAL_CODE, null);			
			result.put(LOCATION.CITY, null);		result.put(LOCATION.STREET, null);		result.put(LOCATION.STREET_NBR, null);
			*/
		}
		return result;		
	}	
	
	//Google implementation for get_close_POI
	private static JSONArray google_get_POI(String search, String latitude, String longitude, String language, String types) {
		//requirements
		if (Is.nullOrEmpty(Config.google_maps_key)){
			Debugger.println("GeoCoding - Google API-key is missing! Please add one via the config file to use Maps.", 1);
			return new JSONArray();
		}
		JSONArray places = new JSONArray();
		int N = 8; //max results 		
		try {
			//use GPS and radius?
			String addGPS = "";
			if (!latitude.isEmpty() && !longitude.isEmpty()){
				addGPS = "&location=" + latitude + "," + longitude;
				addGPS += "&radius=" + String.valueOf(15000);
			}
			//use types?
			String addTypes = "";
			if (types != null){
				addTypes = types;
			}
			String addKey = "";
			if (!Config.google_maps_key.equals("test")){
				addKey = "&key=" + Config.google_maps_key;
			}
			String url = URLBuilder.getString("https://maps.googleapis.com/maps/api/place/textsearch/json",
					"?query=", search,
					"&language=", language,
					"&types=", addTypes
					//"&opennow=", true,
			);
			url += addKey;
			url += addGPS;
			//System.out.println("google_get_POI - search: " + search);							//debug
			//System.out.println("google_get_POI - URL: " + url); 								//debug
			
			//make the HTTP GET call to Google Geocode API
			long tic = System.currentTimeMillis();
			JSONObject response = Connectors.httpGET(url.trim());
			Statistics.addExternalApiHit("Google Places");
			Statistics.addExternalApiTime("Google Places", tic);
			//System.out.println("google_get_POI - Result: " + response.toJSONString()); 		//debug
			
			//TODO: handle over_query_limit with a) 1s break b) 2s break c) possible quota end
			
			JSONArray results = (JSONArray) response.get("results");
			if (results != null && !results.isEmpty()){
				int i = 0;
				for (Object obj : results)
				{
					JSONObject placeJSON = new JSONObject();
					JSONObject resJSON = (JSONObject) obj;
					/*
					"formatted_address" : "Wattenscheider Str. 39, 45307 Essen, Deutschland"
					"geometry" : { "location" : { "lat" : 51.4621956, "lng" : 7.0873221 } }
					"icon" : "https://maps.gstatic.com/mapfiles/place_api/icons/restaurant-71.png"
					"name" : "Krayer Hof"
					"types" : [ "restaurant", "food", "point_of_interest", "establishment" ]
					*/
					JSON.add(placeJSON, LOCATION.ADDRESS_TEXT, resJSON.get("formatted_address"));
					JSON.add(placeJSON, LOCATION.IMAGE, resJSON.get("icon"));
					JSON.add(placeJSON, "types", resJSON.get("types"));
					JSON.add(placeJSON, LOCATION.NAME, resJSON.get("name"));
					JSON.add(placeJSON, LOCATION.LAT, ((JSONObject)((JSONObject) resJSON.get("geometry")).get("location")).get("lat"));
					JSON.add(placeJSON, LOCATION.LNG, ((JSONObject)((JSONObject) resJSON.get("geometry")).get("location")).get("lng"));
					
					JSON.add(places, placeJSON);
					i++;
					if (i > N){
						break;
					}
				} 
			}
			
		}catch (Exception e) {
			Debugger.println("google_get_POI - failed due to: " + e.getMessage(), 1);
			Debugger.printStackTrace(e, 5);
			return new JSONArray();
		}
		return places;
	}

}
