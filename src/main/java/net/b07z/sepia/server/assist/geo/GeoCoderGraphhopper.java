package net.b07z.sepia.server.assist.geo;

import java.net.URLEncoder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;

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
				String osmValue = (String) ((JSONObject) hits.get(0)).get("osm_value");
				String country, state, city, street, postcode;
				if (osmValue.matches("country"))	country = (String) ((JSONObject) hits.get(0)).get("name");
				else								country = (String) ((JSONObject) hits.get(0)).get("country");
				if (osmValue.matches("state"))		state = (String) ((JSONObject) hits.get(0)).get("name");
				else 								state = (String) ((JSONObject) hits.get(0)).get("state");
				if (osmValue.matches("(city|town|village)"))	city = (String) ((JSONObject) hits.get(0)).get("name");
				else											city = (String) ((JSONObject) hits.get(0)).get("city");
				if (osmValue.matches("(residential|footway)"))	street = (String) ((JSONObject) hits.get(0)).get("name");
				else											street = (String) ((JSONObject) hits.get(0)).get("street");
				postcode = (String) ((JSONObject) hits.get(0)).get("postcode");
				
				//TODO: missing
				String streetNbr = null;
				String formattedAddress = null;
				
				//fill result
				return new GeoCoderResult(formattedAddress,
					country, state, postcode, 
					city, street, streetNbr, 
					latitude, longitude
				);
			}
		}catch (Exception e){
			e.printStackTrace();
			return null;
		}
		return null;
	}

	@Override
	public GeoCoderResult getAddress(double latitude, double longitude, String language){
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
		Debugger.println("GeoCoding - Graphhopper 'getAddress' not implemented yet!", 1);
		return null;
	}

}
