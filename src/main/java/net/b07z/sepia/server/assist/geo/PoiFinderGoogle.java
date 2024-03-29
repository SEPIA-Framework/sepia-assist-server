package net.b07z.sepia.server.assist.geo;

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

public class PoiFinderGoogle implements PoiFinderInterface {
	
	@Override
	public boolean isSupported(){
		//requirements
		return !Is.nullOrEmpty(Config.google_maps_key);
	}
	
	@Override
	public String buildCloseToSearch(String place, JSONObject locationJSON, String language){
		//build close to there search
		String searchPlace = "";
		String city = (String) locationJSON.get(LOCATION.CITY);
		String street = (String) locationJSON.get(LOCATION.STREET);
		String poiLoc = (String) locationJSON.get("poiLocation");	//unspecific location, probably some name
		String tag = " close to ";
		String distanceTag = (String) locationJSON.get("distanceTag");
		if (Is.notNullOrEmpty(distanceTag) && distanceTag.equals("<in>")){
			tag = " in ";
		}
		if (Is.notNullOrEmpty(city) && Is.notNullOrEmpty(street)){
			searchPlace = place + tag + street + ", " + city;
		}else if (Is.notNullOrEmpty(city)){
			searchPlace = place + tag + city;
		}else if (Is.notNullOrEmpty(street)){
			searchPlace = place + tag + street;
		}else if (Is.notNullOrEmpty(poiLoc)){
			searchPlace = place + tag + poiLoc;
		}else{
			searchPlace = place;
			Debugger.println("PoiFinderGoogle - buildCloseToSearch - location info is incomplete (req. city or street)!", 1);
		}
		return searchPlace;
	}

	@Override
	public JSONArray getPOI(String search, String[] types, Double latitude, Double longitude, String language){
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
			if (latitude != null && longitude != null){
				addGPS = "&location=" + latitude + "," + longitude;
				addGPS += "&radius=" + String.valueOf(15000);
			}
			//use types?
			String addTypes = "";
			if (types != null){
				addTypes = String.join("|", types);
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
				for (Object obj : results){
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
			Debugger.println("PoiFinderGoogle:getPOI - failed due to: " + e.getMessage(), 1);
			Debugger.printStackTrace(e, 5);
			return new JSONArray();
		}
		return places;
	}
}
