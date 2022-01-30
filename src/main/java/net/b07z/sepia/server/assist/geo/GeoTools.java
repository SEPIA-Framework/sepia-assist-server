package net.b07z.sepia.server.assist.geo;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Some tools to help calculate GEO stuff etc..
 */
public class GeoTools {
	
	/**
	 * Convert Google-Maps search types like "hospital" or "store" to OSM compatible list.
	 * @param types - array of GM types
	 * @return
	 */
	public static Collection<String> convertGoogleMapsTypesToOsm(String[] types){
		Set<String> newTypes = new HashSet<>();
		for (String t : types){
			if (googleToOsmTypeMap.containsKey(t)){
				newTypes.add(googleToOsmTypeMap.get(t));
			}
		}
		return newTypes;
	}
	private static Map<String, String> googleToOsmTypeMap = new HashMap<>();
	static {
		//TODO: add many more and sync. with 'Place.getPoiType'
		//see: https://wiki.openstreetmap.org/wiki/Map_features
		googleToOsmTypeMap.put("bar", "amenity:bar");
		googleToOsmTypeMap.put("store", "shop");
		googleToOsmTypeMap.put("liquor_store", "shop:alcohol");		//or: shop:kiosk?
		googleToOsmTypeMap.put("cafe", "amenity:cafe");
		googleToOsmTypeMap.put("food", "amenity:fast_food");
		googleToOsmTypeMap.put("restaurant", "amenity:restaurant");
		googleToOsmTypeMap.put("meal_takeaway", "amenity:fast_food");
		googleToOsmTypeMap.put("bakery", "shop:bakery");
		googleToOsmTypeMap.put("gas_station", "amenity:fuel");
		googleToOsmTypeMap.put("car_wash", "amenity:car_wash");
		googleToOsmTypeMap.put("lodging", "tourism");	//NOTE: we use just 'tourism' because its broad
		googleToOsmTypeMap.put("night_club", "amenity:nightclub");
		googleToOsmTypeMap.put("train_station", "building:transportation");		//also: train_station
		googleToOsmTypeMap.put("subway_station", "public_transport:station");	//there is more rail stuff
		googleToOsmTypeMap.put("transit_station", "public_transport:station");
		googleToOsmTypeMap.put("bus_station", "amenity:bus_station");
		googleToOsmTypeMap.put("airport", "aeroway");
		googleToOsmTypeMap.put("car_rental", "amenity:car_rental");
		googleToOsmTypeMap.put("taxi_stand", "amenity:taxi");
		googleToOsmTypeMap.put("museum", "tourism:museum");
		googleToOsmTypeMap.put("atm", "amenity:atm");
		googleToOsmTypeMap.put("bank", "amenity:bank");
		googleToOsmTypeMap.put("finance", "amenity:bank");
		googleToOsmTypeMap.put("grocery_or_supermarket", "shop:supermarket");
		googleToOsmTypeMap.put("hospital", "amenity:hospital");
		googleToOsmTypeMap.put("doctor", "amenity:doctors");
		googleToOsmTypeMap.put("pharmacy", "amenity:pharmacy");
		googleToOsmTypeMap.put("police", "amenity:police");
		googleToOsmTypeMap.put("fire_station", "amenity:fire_station");
		googleToOsmTypeMap.put("place_of_worship", "amenity:place_of_worship");
		googleToOsmTypeMap.put("church", "amenity:place_of_worship");
		googleToOsmTypeMap.put("hindu_temple", "amenity:place_of_worship");
		googleToOsmTypeMap.put("mosque", "amenity:place_of_worship");
		googleToOsmTypeMap.put("synagogue", "amenity:place_of_worship");
		googleToOsmTypeMap.put("car_repair", "shop:car_repair");
		googleToOsmTypeMap.put("stadium", "building:stadium");
		googleToOsmTypeMap.put("university", "amenity:university");
		googleToOsmTypeMap.put("movie_theater", "amenity:cinema");
		googleToOsmTypeMap.put("shopping_mall", "shop:mall");
		googleToOsmTypeMap.put("casino", "amenity:casino");
		googleToOsmTypeMap.put("establishment", "amenity:brothel");
		googleToOsmTypeMap.put("zoo", "tourism:zoo");
	}
	
	/**
	 * Convert Google-Maps travel type like '' to OSM compatible ones.
	 * @param travelType - e.g.: 'transit', 'bicycling', 'driving', 'walking'
	 * @return new type or empty
	 */
	public static String convertGoogleMapsTravelTypeToOsm(String travelType){
		return googleToOsmTravelType.getOrDefault(travelType, "");
	}
	private static Map<String, String> googleToOsmTravelType = new HashMap<>();
	static {
		//TODO: add more variants? (e.g. car -> car_delivery, hike etc.)
		//see: https://docs.graphhopper.com/#section/Map-Data-and-Routing-Profiles/OpenStreetMap
		googleToOsmTravelType.put("driving", "car");
		googleToOsmTravelType.put("bicycling", "bike");
		googleToOsmTravelType.put("transit", "");		//TODO: this is a bit special for APIs
		googleToOsmTravelType.put("walking", "foot");
	}
	
	/**
	 * Calculate (approximate) shift of longitude coordinate with regard to starting point.
	 * @param meters - meters to shift
	 * @param latStart - latitude start coordinate
	 * @return
	 */
	public static double calculateLongitudeShift(double meters, double latStart){
		// number of km per degree = ~111km (111.32 in google maps, but range varies
		// between 110.567km at the equator and 111.699km at the poles)
		// 1km in degree = 1 / 111.32km = 0.0089
		// 1m in degree = 0.0089 / 1000 = 0.0000089
		double coef = meters * 0.0000089;
		// pi / 180 = 0.018
		double shift = coef/Math.cos(latStart * 0.018);
		return shift;
	}
	/**
	 * Calculate (approximate) shift in latitude coordinate.
	 * @param meters - meters to shift
	 * @return
	 */
	public static double calculateLatitudeShift(double meters){
		double shift = meters * 0.0000089;
		return shift;
	}

}
