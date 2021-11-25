package net.b07z.sepia.server.assist.geo;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LOCATION;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Result returned from geo-coder request.
 */
public class GeoCoderResult {

	public String name;
	public String country;
	public String state;
	public String postalCode;
	public String city;
	public String street;
	public String streetNumber;
	
	public Double latitude;
	public Double longitude;
	
	public GeoCoderResult(){}
	
	public GeoCoderResult(String name, String country, String state,
			String postalCode, String city, String street, String streetNumber,
			Double latitude, Double longitude){
		
		this.name = name;
		this.country = country;
		this.state = state;
		this.postalCode = postalCode;
		this.city = city;
		this.street = street;
		this.streetNumber = streetNumber;
		
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	public JSONObject exportJson(){
		JSONObject json = new JSONObject();
		if ( name != null) JSON.put(json, LOCATION.NAME, name);
		if ( country != null) JSON.put(json, LOCATION.COUNTRY, country);
		if ( state != null) JSON.put(json, LOCATION.STATE, state);
		if ( postalCode != null) JSON.put(json, LOCATION.POSTAL_CODE, postalCode);
		if ( city != null) JSON.put(json, LOCATION.CITY, city);
		if ( street != null) JSON.put(json, LOCATION.STREET, street);
		if ( streetNumber != null) JSON.put(json, LOCATION.STREET_NBR, streetNumber);
		if ( latitude != null) JSON.put(json, LOCATION.LAT, latitude);
		if ( longitude != null) JSON.put(json, LOCATION.LNG, longitude);
		return json;
	}
}
