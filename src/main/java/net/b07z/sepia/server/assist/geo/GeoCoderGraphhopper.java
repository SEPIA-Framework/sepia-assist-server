package net.b07z.sepia.server.assist.geo;

import java.net.URLEncoder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

public class GeoCoderGraphhopper implements GeoCoderInterface {

	@Override
	public boolean isSupported(){
		//requirements
		return !Is.nullOrEmpty(Config.graphhopper_key);
	}

	@Override
	public GeoCoderResult getCoordinates(String address, String language){
		//requirements
		if (Is.nullOrEmpty(Config.graphhopper_key)){
			Debugger.println("GeoCoding - Graphhopper API-key is missing! Please add one via the config file to use the service.", 1);
			return null;
		}
		try {
			String add_params = "&debug=false&limit=1&key=" + Config.graphhopper_key;
			String url = "https://graphhopper.com/api/1/geocode?q=" 
					+ URLEncoder.encode(address, "UTF-8") + "&locale=" + language + add_params;
			//System.out.println("gh-url: " + url); 	//debug
			
			long tic = System.currentTimeMillis();
			JSONObject response = Connectors.httpGET(url.trim());
			//System.out.println("GH res: " + response); 	//debug
			Statistics.addExternalApiHit("Graphhopper Geocoder");
			Statistics.addExternalApiTime("Graphhopper Geocoder", tic);
			JSONArray hits = (JSONArray) response.get("hits");
			if (Is.notNullOrEmpty(hits)){
				JSONObject firstHit = (JSONObject) hits.get(0);
				return collectGraphhopperHit(firstHit);
			}else{
				return null;	//TODO: return null or empty class?
			}
		}catch (Exception e){
			e.printStackTrace();
			return null;
		}
	}
	/**
	 * Map Graphhopper API result hit to {@link GeoCoderResult}. NOTE: Some field might be null.
	 * @param hit - one JSON object from Graphhopper hits array
	 * @return
	 */
	public static GeoCoderResult collectGraphhopperHit(JSONObject hit){
		String country, state, city, street, streetNbr, postcode;
		//String osmKey = JSON.getString(hit, "osm_key");		//USE: https://wiki.openstreetmap.org/wiki/Map_features
		String osmValue = JSON.getString(hit, "osm_value");		//and: https://wiki.openstreetmap.org/wiki/Tags#Keys_and_values
		
		Double latitude = null;
		Double longitude = null;
		if (hit.containsKey("point")){
			JSONObject points = (JSONObject) hit.get("point");
			latitude = (Double) points.get("lat");
			longitude = (Double) points.get("lng");
		}
		
		if (osmValue.matches("country")) country = JSON.getString(hit, "name");
		else country = JSON.getString(hit, "country");
		
		if (osmValue.matches("state")) state = JSON.getString(hit, "name");
		else state = JSON.getString(hit, "state");
		
		if (osmValue.matches("(city|town|village)")) city = JSON.getString(hit, "name");
		else city = JSON.getString(hit, "city");
		
		if (osmValue.matches("(residential|footway)")) street = JSON.getString(hit, "name");
		else street = JSON.getString(hit, "street");
		
		postcode = JSON.getString(hit, "postcode");
		streetNbr = JSON.getString(hit, "housenumber");
		
		//kind of uncertain:
		String formattedAddress = JSON.getString(hit, "name");	//TODO: is this ok for all types?
		
		//fill result
		return new GeoCoderResult(formattedAddress,
			country, state, postcode, 
			city, street, streetNbr, 
			latitude, longitude
		);
	}

	@Override
	public GeoCoderResult getAddress(double latitude, double longitude, String language){
		//requirements
		if (Is.nullOrEmpty(Config.graphhopper_key)){
			Debugger.println("GeoCoding - Graphhopper API-key is missing! Please add one via the config file to use the service.", 1);
			return null;
		}
		try {
			String add_params = "&reverse=true&debug=false&limit=1&key=" + Config.graphhopper_key;
			String url = "https://graphhopper.com/api/1/geocode?point=" 
					+ String.valueOf(latitude) + "," + String.valueOf(longitude) + "&locale=" + language + add_params;
			//System.out.println("gh-url: " + url); 	//debug
			
			long tic = System.currentTimeMillis();
			JSONObject response = Connectors.httpGET(url.trim());
			//System.out.println("GH res: " + response); 	//debug
			Statistics.addExternalApiHit("Graphhopper GetAddress");
			Statistics.addExternalApiTime("Graphhopper GetAddress", tic);
			JSONArray hits = (JSONArray) response.get("hits");
			if (!hits.isEmpty()){
				//JSONObject points = (JSONObject) ((JSONObject) hits.get(0)).get("point");
				//double latitude = (double) points.get("lat");
				//double longitude = (double) points.get("lng");
				String country, state, city, street, streetNbr, postcode;
				JSONObject firstHit = (JSONObject) hits.get(0);
				//String osmValue = JSON.getString(firstHit, "osm_value");
				//String osmKey = JSON.getString(firstHit, "osm_key");
				country = JSON.getString(firstHit, "country");
				state = JSON.getString(firstHit, "state");
				city = JSON.getString(firstHit, "city");
				street = JSON.getString(firstHit, "street");
				streetNbr = JSON.getString(firstHit, "housenumber");
				postcode = JSON.getString(firstHit, "postcode");
				
				//kind of uncertain:
				String formattedAddress = JSON.getString(firstHit, "name"); 	//TODO: is this ok for all types?
				//if (osmValue.matches("(monument)")) formattedAddress = JSON.getString(firstHit, "name");
				
				//fill result
				return new GeoCoderResult(formattedAddress,
					country, state, postcode, 
					city, street, streetNbr, 
					latitude, longitude
				);
			}else{
				return new GeoCoderResult(null, null, null, null, null, null, null, 
					latitude, longitude);	//TODO: return null or empty class?
			}
		}catch (Exception e){
			e.printStackTrace();
			return null;
		}
	}

}
