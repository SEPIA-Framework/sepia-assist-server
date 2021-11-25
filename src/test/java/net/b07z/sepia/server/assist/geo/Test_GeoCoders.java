package net.b07z.sepia.server.assist.geo;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.server.Start;

public class Test_GeoCoders {

	public static void main(String[] args){
		//load custom config
		Start.loadSettings(new String[]{"--test"});
		
		String searchAddress = "Pariser Platz 1, Berlin, Deutschland";
		Double latitude = 52.5159037d;
		Double longitude = 13.3779998;
		String language = LANGUAGES.DE;
		System.out.println("Search address: " + searchAddress + " - Lang.: " + language);
		System.out.println("Search GPS lat.: " + latitude + ", long.: " + longitude);
		
		//Google
		System.out.println("------GOOGLE-------");
		GeoCoderInterface geoCoderGoogle = new GeoCoderGoogle();
		if (geoCoderGoogle.isSupported()){
			testGetCoordinates(geoCoderGoogle, searchAddress, language);
			testGetAddress(geoCoderGoogle, latitude, longitude, language);
		}else{
			System.out.println("Googlemaps is missing API key!");
		}
		
		//Graphhopper
		System.out.println("------GRAPHHOPPER-------");
		GeoCoderInterface geoCoderGraphhopper = new GeoCoderGraphhopper();
		if (geoCoderGraphhopper.isSupported()){
			testGetCoordinates(geoCoderGraphhopper, searchAddress, language);
			testGetAddress(geoCoderGraphhopper, latitude, longitude, language);
		}else{
			System.out.println("Graphhopper is missing API key!");
		}
	}
	
	/**
	 * Test get_coordinates output of all available methods.
	 * @param address - user input when asking for an address
	 * @param language - language code
	 */
	private static void testGetCoordinates(GeoCoderInterface geoCoder, String address, String language){
		System.out.println("--From address:");
		GeoCoderResult result = geoCoder.getCoordinates(address, language);
		printGeoCoderResult(result);
	}
	/**
	 * Test get_address output of all available methods.
	 * @param latitude - latitude coordinate to search for
	 * @param longitude - longitude coordinate to search for
	 * @param language - language code
	 */
	private static void testGetAddress(GeoCoderInterface geoCoder, Double latitude, Double longitude, String language){
		System.out.println("--From GPS:");
		GeoCoderResult result = geoCoder.getAddress(latitude, longitude, language);
		printGeoCoderResult(result);
	}
	private static void printGeoCoderResult(GeoCoderResult result){
		System.out.println("Coordinates: " + result.latitude + ", "+ result.longitude);
		System.out.println("Street Nbr.: " + result.streetNumber);
		System.out.println("Street: " + result.street);
		System.out.println("Postal Code: " + result.postalCode);
		System.out.println("City: " + result.city);
		System.out.println("State: " + result.state);
		System.out.println("Country: " + result.country);
	}
}
