package net.b07z.sepia.server.assist.geo;

import org.json.simple.JSONArray;

/**
 * Interface for POI (poit-of-interest) finders.
 */
public interface PoiFinderInterface {

	/**
	 * Get a POI (point-of-interest) instead of doing a "normal" address search.
	 * Can be specified with certain "types" like "food" (empty means no restriction).
	 * @param search - search parameter in the style of "restaurants in Berlin"
	 * @param types - POI types like "food", "hospital", "lodging" etc. ... can be a list food|restaurants|...
	 * @param latitude - latitude GPS coordinate or null
	 * @param longitude - longitude GPS coordinate or null. If both lat. and lng. are set a 15km radius is applied for the search 
	 * @param language - language code for specific region output
	 */
	public JSONArray getPOI(String search, String types, Double latitude, Double longitude, String language);
}
