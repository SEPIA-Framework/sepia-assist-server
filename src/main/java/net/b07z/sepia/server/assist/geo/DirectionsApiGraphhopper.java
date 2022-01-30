package net.b07z.sepia.server.assist.geo;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.assist.tools.DateTimeConverters;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Graphhopper implementation of directions API.
 */
public class DirectionsApiGraphhopper implements DirectionsApiInterface {
	
	public static final String DEFAULT_TRAVEL_MODE = "car"; 

	@Override
	public boolean isSupported(){
		return !Is.nullOrEmpty(Config.graphhopper_key);
	}

	@Override
	public DirectionsApiResult getDurationAndDistance(String startLatitude, String startLongitude, String endLatitude,
			String endLongitude, String wpLatitude, String wpLongitude, String travelMode, String language){
		
		if (Is.nullOrEmpty(Config.graphhopper_key)){
			Debugger.println("DirectionsAPI - Graphhopper API-key is missing! Please add one via the config file to use Maps.", 1);
			return null;
		}
		if (Is.nullOrEmpty(startLongitude) || Is.nullOrEmpty(startLatitude) 
				|| Is.nullOrEmpty(endLongitude) || Is.nullOrEmpty(endLatitude)){
			Debugger.println("DirectionsAPI - Input invalid or incomplete! Please set at least start and end coordinates", 1);
			return null;
		}
		if (Is.nullOrEmpty(travelMode)){
			travelMode = DirectionsApiGoogle.DEFAULT_TRAVEL_MODE;		//we use Google format because the service answer uses it too
		}
		if (travelMode.equals("transit")){
			//unfortunately not supported right now - TODO: can we fix this?
			Debugger.println("DirectionsAPI - Graphhopper does not support 'transit' travel-mode atm.", 3);
			return new DirectionsApiResult();	//we return empty instead of null
		}
		//transform travelMode format to OSM
		String travelModeOsm = GeoTools.convertGoogleMapsTravelTypeToOsm(travelMode);
		if (Is.nullOrEmpty(travelModeOsm)){
			travelModeOsm = DEFAULT_TRAVEL_MODE;
			travelMode = DirectionsApiGoogle.DEFAULT_TRAVEL_MODE;
		}
		
		DirectionsApiResult directionsResult = new DirectionsApiResult();
		directionsResult.travelMode = travelMode;	//update in case we changed it
		
		List<String> points = new ArrayList<>();
		points.add(startLatitude + "," + startLongitude);
		if (Is.notNullOrEmpty(wpLatitude) && Is.notNullOrEmpty(wpLongitude)){
			points.add(wpLatitude + "," + wpLongitude);
		}
		points.add(endLatitude + "," + endLongitude);
		
		long tic = System.currentTimeMillis();
		try {
			String directionsApiURL = "https://graphhopper.com/api/1/route" +
				"?profile=" + URLEncoder.encode(travelModeOsm, "UTF-8") +
				"&locale=" + URLEncoder.encode(language, "UTF-8") +
				//"&details=time" + "&details=distance" +		//this is per 'leg' - there is a bunch of more stuff like 'street_name'
				"&optimize=false" + 
				"&instructions=false" + "&calc_points=true";
			
			for (String p : points){
				directionsApiURL += "&point=" +  URLEncoder.encode(p, "UTF-8");
			}
			//System.out.println("HTTP GET URL: " + directionsApiURL); 					//debug
			directionsApiURL += "&key=" + Config.graphhopper_key;
			
			//Connect
			JSONObject response = Connectors.httpGET(directionsApiURL.trim());
			Statistics.addExternalApiHit("Directions Graphhopper API");
			Statistics.addExternalApiTime("Directions Graphhopper API", tic);
			//JSON.prettyPrint(response);					//debug
			if (Connectors.httpSuccess(response)){
				try{
					JSONArray paths = JSON.getJArray(response, "paths");
					if (Is.notNullOrEmpty(paths)){
						//first index has all the info
						JSONObject pathDataZero = JSON.getJObject(paths, 0);
						
						long time = JSON.getLongOrDefault(pathDataZero, "time", -1);
						if (time > 0){
							directionsResult.durationSeconds = Math.round(time/1000);
							long secLeft = directionsResult.durationSeconds; 
							long dd = (long) Math.floor(directionsResult.durationSeconds/86400);
							secLeft = secLeft - (dd*86400);
							long hh = (long) Math.floor(secLeft/3600);
							secLeft = secLeft - (hh*3600);
							long mm = (long) Math.floor(secLeft/60);
							//secLeft = secLeft - (mm*60);
							//long ss = secLeft;
							directionsResult.durationText = DateTimeConverters.getSpeakableDuration(language, dd, hh, mm, 0);
							//System.out.println("Duration: " + directionsResult.durationText); 					//debug
						}else{
							directionsResult.durationSeconds = -1;
						}
						double distance = JSON.getDoubleOrDefault(pathDataZero, "distance", -1);
						if (distance > 0){
							directionsResult.distanceMeter = Math.round(distance);
							directionsResult.distanceText = Math.round(directionsResult.distanceMeter/1000) + " km";
						}else{
							directionsResult.distanceMeter = -1;
						}
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
