package net.b07z.sepia.server.assist.geo;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.assistant.LOCATION;
import net.b07z.sepia.server.assist.parameters.Place;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.core.tools.JSON;

public class Test_GeoCoders {

	public static void main(String[] args){
		//load custom config
		Start.loadSettings(new String[]{"--test"});
		
		System.out.println("\n------GEO-CODER-------\n");
		
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
			System.out.println("Googlemaps GEO-CODER is not supported (missing API key?)");
		}
		//Graphhopper
		System.out.println("------GRAPHHOPPER-------");
		GeoCoderInterface geoCoderGraphhopper = new GeoCoderGraphhopper();
		if (geoCoderGraphhopper.isSupported()){
			testGetCoordinates(geoCoderGraphhopper, searchAddress, language);
			testGetAddress(geoCoderGraphhopper, latitude, longitude, language);
		}else{
			System.out.println("Graphhopper GEO-CODER is not supported (missing API key?)");
		}
		
		//nowhere
		latitude = 54.5d;
		longitude = 2.8;
		System.out.println("\nSearch address: middle of nowhere ;-)");
		System.out.println("Search GPS lat.: " + latitude + ", long.: " + longitude);
		
		//Google
		System.out.println("------GOOGLE-------");
		if (geoCoderGoogle.isSupported()){
			testGetAddress(geoCoderGoogle, latitude, longitude, language);
		}
		//Graphhopper
		System.out.println("------GRAPHHOPPER-------");
		if (geoCoderGraphhopper.isSupported()){
			testGetAddress(geoCoderGraphhopper, latitude, longitude, language);
		}
		
		System.out.println("\n------POI-FINDER-------\n");
		
		String search = "Krankenhaus";
		JSONObject closeToLocation = JSON.make(
			LOCATION.CITY, "Berlin",
			LOCATION.STREET, "Wiesenstraße"
		);
		JSONObject closeToGps = JSON.make(
			LOCATION.LAT, 52.5439, 
			LOCATION.LNG, 13.3798
		);
		String languagePoi = LANGUAGES.DE;
		String[] commonTypes = Place.getPoiType(search.toLowerCase(), languagePoi);
		System.out.println("Search: " + search + " - close to: " + closeToLocation);
		System.out.println("Common type(s): " + String.join(", ", commonTypes));
		
		//Google
		System.out.println("------GOOGLE-------");
		PoiFinderInterface poiFinderGoogle = new PoiFinderGoogle();
		if (poiFinderGoogle.isSupported()){
			testGetPoi(poiFinderGoogle, search, closeToLocation, commonTypes, languagePoi);
			testGetPoi(poiFinderGoogle, search, closeToGps, commonTypes, languagePoi);
			testGetPoi(poiFinderGoogle, "Flughafen", closeToLocation, Place.getPoiType("flughafen", languagePoi), languagePoi);
			testGetPoi(poiFinderGoogle, "baeckerei", closeToLocation, Place.getPoiType("baeckerei", languagePoi), languagePoi);
		}else{
			System.out.println("Googlemaps POI-Finder is not supported (missing API key?)");
		}
		//Graphhopper
		System.out.println("------GRAPHHOPPER-------");
		PoiFinderInterface poiFinderGraphhopper = new PoiFinderGraphhopper();
		if (poiFinderGraphhopper.isSupported()){
			testGetPoi(poiFinderGraphhopper, search, closeToLocation, commonTypes, languagePoi);
			testGetPoi(poiFinderGraphhopper, search, closeToGps, commonTypes, languagePoi);
			testGetPoi(poiFinderGraphhopper, "Flughafen", closeToLocation, Place.getPoiType("flughafen", languagePoi), languagePoi);
			testGetPoi(poiFinderGraphhopper, "baeckerei", closeToLocation, Place.getPoiType("baeckerei", languagePoi), languagePoi);
		}else{
			System.out.println("Graphhopper POI-Finder is not supported (missing API key?)");
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
		System.out.println("Name: " + result.name);
		System.out.println("Coordinates: " + result.latitude + ", "+ result.longitude);
		System.out.println("Street: " + result.street);
		System.out.println("Street Nbr.: " + result.streetNumber);
		System.out.println("City: " + result.city);
		System.out.println("Postal Code: " + result.postalCode);
		System.out.println("State: " + result.state);
		System.out.println("Country: " + result.country);
	}
	
	private static void testGetPoi(PoiFinderInterface poiFinder, String search, JSONObject closeToLocation, 
			String[] types, String languagePoi){
		String searchTerm = poiFinder.buildCloseToSearch(search, closeToLocation, languagePoi);
		System.out.println("Search term: " + searchTerm);
		JSONArray poiRes = poiFinder.getPOI(searchTerm, types, 
				(Double) closeToLocation.get(LOCATION.LAT), (Double) closeToLocation.get(LOCATION.LNG), languagePoi);
		for (int i=0; i<poiRes.size(); i++){
			JSONObject res = JSON.getJObject(poiRes, i);
			System.out.println("POI result " + i + ": " + res.get("name") + " - " + res.get("addressText"));
		}
	}
}
