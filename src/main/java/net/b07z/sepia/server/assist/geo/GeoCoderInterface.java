package net.b07z.sepia.server.assist.geo;

/**
 * Interface for geo-coder services.
 */
public interface GeoCoderInterface {
	
	/**
	 * Is geo-coding supported? E.g. there might be an API key required.
	 */
	public boolean isSupported();
	
	/**
	 * Get latitude, longitude from address name. 
	 * @param address - string with an address, optimally Street, City, Country
	 * @param language - language code (e.g. 'en')
	 * @return GeoCoderResult
	 */
	public GeoCoderResult getCoordinates(String address, String language);
	
	/**
	 * Get an address from latitude longitude coordinates. 
	 * @param latitude - e.g. 51.45
	 * @param longitude - e.g. 7.02
	 * @param language - language code (e.g. 'en')
	 * @return GeoCoderResult
	 */
	public GeoCoderResult getAddress(double latitude, double longitude, String language);

}
