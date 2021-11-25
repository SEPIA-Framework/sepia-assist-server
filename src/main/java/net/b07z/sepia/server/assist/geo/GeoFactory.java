package net.b07z.sepia.server.assist.geo;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Build GEO classes.
 */
public class GeoFactory {

	//engines
	public static final String OSM = "osm";						//TODO: implement
	public static final String GOOGLE = "google";
	public static final String GRAPHHOPPER = "graphhopper"; 	//TODO: add missing features
		
	/**
	 * Create geo-coder according to configuration.
	 */
	public static GeoCoderInterface createGeoCoder(){
		if (Config.default_geo_api.equals(OSM)){
			//TODO: implement
			Debugger.println("GeoFactory - unknown GeoCoderInterface: " + Config.default_geo_api, 1);
			return null;
		}else if (Config.default_geo_api.equals(GOOGLE)){
			return new GeoCoderGoogle();
		}else if (Config.default_geo_api.equals(GRAPHHOPPER)){
			return new GeoCoderGraphhopper();
		}else{
			Debugger.println("GeoFactory - unknown GeoCoderInterface: " + Config.default_geo_api, 1);
			return null;
		}
	}
	
	/**
	 * Create POI-finder according to configuration.
	 */
	public static PoiFinderInterface createPoiFinder(){
		if (Config.default_poi_api.equals(GOOGLE)){
			return new PoiFinderGoogle();
		}else{
			Debugger.println("GeoFactory - unknown PoiFinderInterface: " + Config.default_poi_api, 1);
			return null;
		}
	}
	
	/**
	 * Create directions-API according to configuration.
	 */
	public static DirectionsApiInterface createDirectionsApi(){
		//TODO: implement
		Debugger.println("GeoFactory - unknown DirectionsApiInterface: " + Config.default_directions_api, 1);
		return null;
	}
}
