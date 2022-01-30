package net.b07z.sepia.server.assist.geo;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Interface for POI (point-of-interest) finders.
 */
public interface PoiFinderInterface {
	
	/**
	 * Is POI search supported? E.g. there might be an API key required.
	 */
	public boolean isSupported();

	/**
	 * Try to build a "close to location" search term that can be used inside the specific places API.
	 * @param place - place to start with, e.g. a POI
	 * @param locationJSON - location JSONObject containing the data of the "close to" place
	 * @param language - language code
	 */
	public String buildCloseToSearch(String place, JSONObject locationJSON, String language);
	
	/**
	 * Get a POI (point-of-interest) instead of doing a "normal" address search.
	 * Can be specified with certain "types" like "food" (empty means no restriction).
	 * @param search - search parameter in the style of "restaurants in Berlin"
	 * @param types - POI types like "food", "hospital", "lodging" etc. in Google-Maps compatible naming ... can be empty or null ..
	 * @param latitude - latitude GPS coordinate or null
	 * @param longitude - longitude GPS coordinate or null. If both lat. and lng. are set a 15km radius is applied for the search 
	 * @param language - language code for specific region output
	 */
	public JSONArray getPOI(String search, String[] types, Double latitude, Double longitude, String language);
	
}
