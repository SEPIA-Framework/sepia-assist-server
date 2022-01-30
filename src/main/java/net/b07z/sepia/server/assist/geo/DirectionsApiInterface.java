package net.b07z.sepia.server.assist.geo;

/**
 * Interface for map directions APIs. 
 */
public interface DirectionsApiInterface {
	
	/**
	 * Is directions API supported? E.g. there might be an API key required.
	 */
	public boolean isSupported();
	
	/**
	 * Get duration and distance from start and end location with optional way-point for specific travel type. 
	 * @param startLatitude - start latitude
	 * @param startLongitude - start longitude
	 * @param endLatitude - end latitude
	 * @param endLongitude - end longitude
	 * @param wpLatitude - way-point latitude
	 * @param wpLongitude - way-point longitude
	 * @param travelMode - one of 'transit', 'bicycling', 'driving', 'walking'
	 * @param language - language/region code for result optimization
	 * @return result or null on any error
	 */
	public DirectionsApiResult getDurationAndDistance(String startLatitude, String startLongitude,
			String endLatitude, String endLongitude, String wpLatitude, String wpLongitude,
			String travelMode, String language);

}
