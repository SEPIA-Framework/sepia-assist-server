package net.b07z.sepia.server.assist.geo;

import java.net.URLEncoder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;

public class GeoCoderGoogle implements GeoCoderInterface {

	@Override
	public boolean isSupported(){
		return !Is.nullOrEmpty(Config.google_maps_key);
	}

	@Override
	public GeoCoderResult getCoordinates(String address, String language){
		//requirements
		if (Is.nullOrEmpty(Config.google_maps_key)){
			Debugger.println("GeoCoding - Google API-key is missing! Please add one via the config file to use Maps.", 1);
			return null;
		}
		try {
			String addRegion = "&region=" + language + "&language=" + language;		//TODO: split region and language?
			String addKey = "";
			if (!Config.google_maps_key.equals("test")){
				addKey = "&key=" + Config.google_maps_key;
			}
			String url = "https://maps.googleapis.com/maps/api/geocode/json?address=" 
					+ URLEncoder.encode(address, "UTF-8") 
					+ addRegion
					//+ "&bounds=51.361,6.978|51.561,7.178"
					+ addKey; 	//TODO: add the "near to" options
			
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

				Double latitude = (Double) location.get("lat");
				Double longitude = (Double) location.get("lng");
																
				// get the city and country from formatted_address
				//String formatted_address = (String) ((JSONObject) results.get(0)).get("formatted_address"); // e.g. Faro District, Portugal
				//String[] parts = formatted_address.split(","); 
				//city = parts[parts.length - 2]; 
				//country = parts[parts.length - 1]; // in the last part of formatted_address is the country
				
				JSONArray addressComponents = (JSONArray) ((JSONObject) results.get(0)).get("address_components"); 
				String city = null, country = null, postcode = null, street = null, state = null, streetNbr = null;
				int i = 0;
				for (Object obj : addressComponents)
				{
					String currentComponent = ((JSONObject) obj).get("types").toString();
					
					// "types" : [ "street_number" ]
					if (currentComponent.contains("\"street_number\""))
					{
						streetNbr = (String) ((JSONObject) addressComponents.get(i)).get("long_name"); 		
					}
					// "types" : [ "route" ]
					else if (currentComponent.contains("\"route\""))
					{
						street = (String) ((JSONObject) addressComponents.get(i)).get("long_name"); 		
					}
					// search for GoogleOutput:		"types" : [ "locality", "political" ]
					else if (currentComponent.contains("\"locality\""))
					{
						city = (String) ((JSONObject) addressComponents.get(i)).get("long_name"); 		
					}
					// "types" : [ "sublocality", "political" ] -- best replacement for city ???
					else if (currentComponent.contains("\"sublocality\""))
					{
						if (city == null)
							city = (String) ((JSONObject) addressComponents.get(i)).get("long_name"); 		
					}
					// "types" : [ "administrative_area_level_1", "political" ] -- it should be the state 
					else if (currentComponent.contains("\"administrative_area_level_1\""))
					{
						state = (String) ((JSONObject) addressComponents.get(i)).get("long_name"); 		
					}
					// search for GoogleOutput:		"types" : [ "country", "political" ]
					else if (currentComponent.contains("\"country\""))
					{
						country = (String) ((JSONObject) addressComponents.get(i)).get("long_name"); 
					}
					// search for GoogleOutput:		"types" : [ "postal_code" ]
					else if (currentComponent.contains("\"postal_code\""))
					{
						postcode = (String) ((JSONObject) addressComponents.get(i)).get("long_name"); 
					}
					
					i++;
				}
				String formattedAddress = (String) ((JSONObject) results.get(0)).get("formatted_address");
				//fill result
				return new GeoCoderResult(formattedAddress.replaceFirst(",.*?$", "").trim(),
					country, state, postcode, 
					city, street, streetNbr, 
					latitude, longitude
				);
			}else{
				return null;	//TODO: return null or empty class?
			}
		}catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public GeoCoderResult getAddress(double latitude, double longitude, String language){
		//requirements
		if (Is.nullOrEmpty(Config.google_maps_key)){
			Debugger.println("GeoCoding - Google API-key is missing! Please add one via the config file to use Maps.", 1);
			return null;
		}
		try {
			String addKey = "";
			if (!Config.google_maps_key.equals("test")){
				addKey = "&key=" + Config.google_maps_key;
			}
			String add_region = "&region=" + language + "&language=" + language;		//TODO: split region and language?
			String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" 
					+ String.valueOf(latitude) + "," + String.valueOf(longitude) 
					+ add_region + addKey;
			
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
				
				JSONArray addressComponents = (JSONArray) ((JSONObject) results.get(0)).get("address_components"); 
				String city = null, country = null, postcode = null, street = null, state = null, streetNbr = null;
				int i = 0;
				for (Object obj : addressComponents)
				{
					String currentComponent = ((JSONObject) obj).get("types").toString();

					// "types" : [ "street_number" ]
					if (currentComponent.contains("\"street_number\""))
					{
						streetNbr = (String) ((JSONObject) addressComponents.get(i)).get("long_name"); 		
					}
					// "types" : [ "route" ]
					else if (currentComponent.contains("\"route\""))
					{
						street = (String) ((JSONObject) addressComponents.get(i)).get("long_name"); 		
					}
					// search for GoogleOutput:		"types" : [ "locality", "political" ]
					else if (currentComponent.contains("\"locality\""))
					{
						city = (String) ((JSONObject) addressComponents.get(i)).get("long_name"); 		
					}
					// "types" : [ "sublocality", "political" ] -- best replacement for city ???
					else if (currentComponent.contains("\"sublocality\""))
					{
						if (city == null)
							city = (String) ((JSONObject) addressComponents.get(i)).get("long_name"); 		
					}
					// "types" : [ "administrative_area_level_1", "political" ] -- it should be the state 
					else if (currentComponent.contains("\"administrative_area_level_1\""))
					{
						state = (String) ((JSONObject) addressComponents.get(i)).get("long_name"); 		
					}
					// search for GoogleOutput:		"types" : [ "country", "political" ]
					else if (currentComponent.contains("\"country\""))
					{
						country = (String) ((JSONObject) addressComponents.get(i)).get("long_name"); 
					}
					// search for GoogleOutput:		"types" : [ "postal_code" ]
					else if (currentComponent.contains("\"postal_code\""))
					{
						postcode = (String) ((JSONObject) addressComponents.get(i)).get("long_name"); 
					}
					
					i++;
				} 
				String formattedAddress = (String) ((JSONObject) results.get(0)).get("formatted_address");
				//fill result
				return new GeoCoderResult(formattedAddress.replaceFirst(",.*?$", "").trim(),
					country, state, postcode, 
					city, street, streetNbr, 
					latitude, longitude
				);
			}else{
				return new GeoCoderResult(null, null, null, null, null, null, null, 
					latitude, longitude);	//TODO: return null or empty class?
			}
			
		}catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
