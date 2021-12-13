package net.b07z.sepia.server.assist.geo;

import org.json.simple.JSONArray;

/**
 * Result of directions API request.
 */
public class DirectionsApiResult {
	
	public long durationSeconds;
	public String durationText;
	
	public long distanceMeter;
	public String distanceText;
	
	public String travelMode;
	
	public JSONArray route;
}
