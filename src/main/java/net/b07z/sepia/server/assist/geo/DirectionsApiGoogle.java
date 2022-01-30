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

/**
 * Google-Maps implementation of Directions API.
 */
public class DirectionsApiGoogle implements DirectionsApiInterface {
	
	public static final String DEFAULT_TRAVEL_MODE = "driving";

	@Override
	public boolean isSupported(){
		return !Is.nullOrEmpty(Config.google_maps_key);
	}

	@Override
	public DirectionsApiResult getDurationAndDistance(String startLatitude, String startLongitude,
			String endLatitude, String endLongitude, String wpLatitude, String wpLongitude, 
			String travelMode, String language){
		
		if (Is.nullOrEmpty(Config.google_maps_key)){
			Debugger.println("DirectionsAPI - Google API-key is missing! Please add one via the config file to use Maps.", 1);
			return null;
		}
		if (Is.nullOrEmpty(startLongitude) || Is.nullOrEmpty(startLatitude) 
				|| Is.nullOrEmpty(endLongitude) || Is.nullOrEmpty(endLatitude)){
			Debugger.println("DirectionsAPI - Input invalid or incomplete! Please set at least start and end coordinates", 1);
			return null;
		}
		String start = startLatitude + "," + startLongitude;
		String end = endLatitude + "," + endLongitude;
		String wp = "";
		if (Is.notNullOrEmpty(wpLatitude) && Is.notNullOrEmpty(wpLongitude)){
			wp = wpLatitude + "," + wpLongitude;
		}
		
		if (travelMode == null || !travelMode.matches("(transit|bicycling|driving|walking)")){
			travelMode = DEFAULT_TRAVEL_MODE;
		}
		
		DirectionsApiResult directionsResult = new DirectionsApiResult();
		directionsResult.travelMode = travelMode;	//update in case we changed it
		
		long tic = System.currentTimeMillis();
		String googleDirectionsURL;
		try {
			long departureTime = System.currentTimeMillis()/1000;
			
			if (wp.isEmpty()){
				googleDirectionsURL = "https://maps.googleapis.com/maps/api/directions/json" +
						"?origin=" + URLEncoder.encode(start, "UTF-8") +
						"&destination=" + URLEncoder.encode(end, "UTF-8") +
						//"&waypoints=" + URLEncoder.encode(wp, "UTF-8") +
						"&departure_time=" + departureTime +
						"&mode=" + URLEncoder.encode(travelMode, "UTF-8") +
						"&region=" + language +
						"&language=" + language;
			}else{
				googleDirectionsURL = "https://maps.googleapis.com/maps/api/directions/json"+
						"?origin=" + URLEncoder.encode(start, "UTF-8") +
						"&destination=" + URLEncoder.encode(end, "UTF-8") +
						"&waypoints=via:" + URLEncoder.encode(wp, "UTF-8") +
						"&departure_time=" + departureTime +
						"&mode=" + URLEncoder.encode(travelMode, "UTF-8") +
						"&region=" + language +
						"&language=" + language;
			}
			//System.out.println("HTTP GET URL: " + googleDirectionsURL); 					//debug
			googleDirectionsURL += "&key=" + Config.google_maps_key;
			
			//Connect
			JSONObject response = Connectors.httpGET(googleDirectionsURL.trim());
			Statistics.addExternalApiHit("Directions GoogleMaps API");
			Statistics.addExternalApiTime("Directions GoogleMaps API", tic);
			//JSON.prettyPrint(response);					//debug
			
			if (Connectors.httpSuccess(response)){
				try{
					JSONArray routes = JSON.getJArray(response, "routes");
					if (Is.notNullOrEmpty(routes)){
						JSONArray legs = (JSONArray) ((JSONObject) routes.get(0)).get("legs");
						JSONObject durationJSON = (JSONObject) ((JSONObject) legs.get(0)).get("duration");
						JSONObject distanceJSON = (JSONObject) ((JSONObject) legs.get(0)).get("distance");
						
						directionsResult.durationText = JSON.getString(durationJSON, "text");
						directionsResult.durationSeconds = JSON.getLongOrDefault(durationJSON, "value", -1);
						directionsResult.distanceText = JSON.getString(distanceJSON, "text");
						directionsResult.distanceMeter = Math.round(JSON.getDoubleOrDefault(distanceJSON, "value", -1));
						
					}
					return directionsResult;
				
				}catch (Exception e){
					Debugger.println("DirectionsAPI - failed to parse result", 1);
					Debugger.printStackTrace(e, 3);
					return null;
				}
			}else{
				Debugger.println("DirectionsAPI - failed to get API result. Response: " + response.toJSONString(), 1);
				return null;
			}
			
		//API Error
		}catch (Exception e){
			Debugger.println("DirectionsAPI - failed to call API", 1);
			Debugger.printStackTrace(e, 3);
			return null;
		}
	}

}
