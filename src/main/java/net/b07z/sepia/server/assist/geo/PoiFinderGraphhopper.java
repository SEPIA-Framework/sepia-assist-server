package net.b07z.sepia.server.assist.geo;

import java.net.URLEncoder;
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

public class PoiFinderGraphhopper implements PoiFinderInterface {
	
	@Override
	public boolean isSupported(){
		//requirements
		return !Is.nullOrEmpty(Config.graphhopper_key);
	}

	@Override
	public String buildCloseToSearch(String place, JSONObject locationJSON, String language){
		String searchPlace = "";
		String city = (String) locationJSON.get(LOCATION.CITY);
		String street = (String) locationJSON.get(LOCATION.STREET);
		String poiLoc = (String) locationJSON.get("poiLocation");	//unspecific location, probably some name
		//String distanceTag = (String) locationJSON.get("distanceTag");	//could be <in> but ignored here
		if ((city != null && !city.isEmpty()) && (street != null && !street.isEmpty())){
			searchPlace = place + ", " + street + ", " + city;
		}else if (city != null && !city.isEmpty()){
			searchPlace = place + ", " + city;
		}else if (street != null && !street.isEmpty()){
			searchPlace = place + ", " + street;
		}else if (poiLoc != null && !poiLoc.isEmpty()){
			searchPlace = place + ", " + poiLoc;
		}else{
			searchPlace = place;
			Debugger.println("PoiFinderGraphhopper - buildCloseToSearch - location info is incomplete (req. city or street)!", 1);
		}
		return searchPlace;
	}
	
	@Override
	public JSONArray getPOI(String search, String[] types, Double latitude, Double longitude, String language){
		//requirements
		if (Is.nullOrEmpty(Config.graphhopper_key)){
			Debugger.println("PoiFinder - Graphhopper API-key is missing! Please add one via the config file to use the service.", 1);
			return null;
		}
		JSONArray places = new JSONArray();
		int N = 8; //max results
		try {
			//use GPS and radius?
			String addGPS = "";
			if (latitude != null && longitude != null){
				//approx. 30km box
				double shiftLat = GeoTools.calculateLatitudeShift(15000);
				double shiftLong = GeoTools.calculateLongitudeShift(15000, latitude);
				double minLong = longitude - shiftLong;
				double maxLong = longitude + shiftLong;
				double minLat = latitude - shiftLat;
				double maxLat = latitude + shiftLat;
				addGPS = "&bbox=" + minLong + "," + minLat + "," + maxLong + "," + maxLat;
			}
			//use types?
			String addTypes = "";
			if (types != null){
				//E.g. osm_tag=tourism:museum or just the key osm_tag=tourism. To in/exclude multiple tags you add multiple osm_tag parameters
				for (String t : GeoTools.convertGoogleMapsTypesToOsm(types)){
					addTypes += "&osm_tag=" + URLEncoder.encode(t, "UTF-8");
				}
			}
			String add_params = "&debug=false&limit=" + N + "&key=" + Config.graphhopper_key + addGPS + addTypes;
			//add_params += addGPS;
			String url = URLBuilder.getString("https://graphhopper.com/api/1/geocode",
					"?q=", search,
					"&locale=", language
			);
			url += add_params;
			//System.out.println("graphhopper_get_POI - search: " + search);	//debug
			//System.out.println("graphhopper_get_POI - URL: " + url);			//debug
			
			long tic = System.currentTimeMillis();
			JSONObject response = Connectors.httpGET(url.trim());
			//System.out.println("GH res: " + response); 						//debug
			Statistics.addExternalApiHit("Graphhopper Geocoder POI");
			Statistics.addExternalApiTime("Graphhopper Geocoder POI", tic);
			JSONArray hits = (JSONArray) response.get("hits");
			if (hits != null && !hits.isEmpty()){
				int i = 0;
				for (Object obj : hits){
					JSONObject hit = (JSONObject) obj;
					//System.out.println("GH res: " + hit);						//debug
					GeoCoderResult geoRes = GeoCoderGraphhopper.collectGraphhopperHit(hit);
					
					JSONObject placeJSON = new JSONObject();
					JSON.put(placeJSON, LOCATION.ADDRESS_TEXT, geoRes.buildAddressText(language));
					JSON.put(placeJSON, LOCATION.IMAGE, null);
					JSON.put(placeJSON, "types", null);
					JSON.put(placeJSON, LOCATION.NAME, geoRes.name);
					JSON.put(placeJSON, LOCATION.LAT, geoRes.latitude);
					JSON.put(placeJSON, LOCATION.LNG, geoRes.longitude);
					
					JSON.add(places, placeJSON);
					i++;
					if (i > N){
						break;
					}
				} 
			}
		}catch (Exception e){
			Debugger.println("PoiFinderGraphhopper:getPOI - failed due to: " + e.getMessage(), 1);
			Debugger.printStackTrace(e, 5);
			return new JSONArray();
		}
		return places;
	}
	
}
