package net.b07z.sepia.server.assist.data;

import java.util.Map;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LOCATION;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Class for addresses.
 * 
 * @author Florian Quirin
 *
 */
public class Address {
	
	//the path in the DB (e.g. type in Elasticsearch)
	public static final String ADDRESSES_TYPE = "addresses";
	
	//use this in contacts or favorites to save addresses
	public static final String FIELD = "address";
	
	//special tags
	public static final String USER_HOME_TAG = "user_home";
	public static final String USER_WORK_TAG = "user_work";
	
	//set these manually or by using the JSON constructor
	public String description;
	public String name;
	public String latitude;
	public String longitude;
	public String street;
	public String streetNumber;
	public String city;
	public String postalCode;
	public String state;
	public String country;
	private JSONObject jsonAddress;
	
	//there are not in JSON but useful for services. Set manually during DB extraction!
	public String user;					//user who stored this
	public String userSpecialTag;		//special tag given by the user like "user_home"
	public String userSpecialName;		//special name given by the user like "my monday soccer"
	public String dbId;					//database ID for this address (e.g. Elasticsearch _id)
	
	private boolean isCreatedEmpty = false;
	
	/**
	 * Create new Address object. Use this constructor whenever you can to make sure you have the correct variables. 
	 * @param country
	 * @param state
	 * @param city
	 * @param postalCode
	 * @param street
	 * @param streetNumber
	 * @param latitude
	 * @param longitude
	 * @param name
	 * @param description
	 */
	public Address(String country, String state, String city, String postalCode, 
			String street, String streetNumber, 
			String latitude, String longitude,
			String name, String description){
		this.country = country;
		this.state = state;
		this.city = city;
		this.postalCode = postalCode;
		this.street = street;
		this.streetNumber = streetNumber;
		this.latitude = latitude;
		this.longitude = longitude;
		this.name = name;
		this.description = description;
	}
	/**
	 * Use this if you want to manually set the data.
	 */
	public Address (){}
	/**
	 * Use this if you want to create an object that is explicitly set as empty address (to make the isEmpty() methods 100% safe).
	 */
	public Address (boolean isCreatedEmpty){
		this.isCreatedEmpty = isCreatedEmpty;
	}
	/**
	 * Use this if you have an address-JSONObject and you KNOW! that it has the correct format.
	 */
	public Address (JSONObject jo){
		jsonAddress = jo;
		this.description = (String) jo.get(PersonalInfo.DESCRIPTION);
		this.name = (String) jo.get(LOCATION.NAME);
		this.latitude = JSON.getString(jo, LOCATION.LAT);
		this.longitude = JSON.getString(jo, LOCATION.LNG);
		this.street = (String) jo.get(LOCATION.STREET);
		this.streetNumber = JSON.getString(jo, LOCATION.STREET_NBR);
		this.city = (String) jo.get(LOCATION.CITY);
		this.postalCode = JSON.getString(jo, LOCATION.POSTAL_CODE);
		this.state = (String) jo.get(LOCATION.STATE);
		this.country = (String) jo.get(LOCATION.COUNTRY);
	}
	/**
	 * Use this if you have an address-map and you KNOW! that it has the correct format.
	 */
	public Address (Map<String, Object> map){
		this.description = (String) map.get(PersonalInfo.DESCRIPTION);
		this.name = (String) map.get(LOCATION.NAME);
		this.latitude = (String) map.get(LOCATION.LAT);
		this.longitude = (String) map.get(LOCATION.LNG);
		this.street = (String) map.get(LOCATION.STREET);
		this.streetNumber = (String) map.get(LOCATION.STREET_NBR);
		this.city = (String) map.get(LOCATION.CITY);
		this.postalCode = (String) map.get(LOCATION.POSTAL_CODE);
		this.state = (String) map.get(LOCATION.STATE);
		this.country = (String) map.get(LOCATION.COUNTRY);
	}
	
	/**
	 * Check if this object has some data by checking country, city, street and latitude.<br>
	 * If that is not enough you need to check the remaining fields yourself.
	 */
	public boolean isEmpty(){
		return (country == null && city == null && street == null && latitude == null);
	}
	/**
	 * Check if this object was intentionally created empty.
	 */
	public boolean wasCreatedEmpty(){
		return isCreatedEmpty;
	}
	
	/**
	 * Get value from the submitted JSON string (used in the constructor) or give default value. If you've added fields manually
	 * you can use "buildJSON" before to update the JSON sting. 
	 * @param addressPart - parameter you are looking for like LOCATION.CITY
	 * @param defaultValue - value returned if the parameter is null or empty
	 */
	public String getFromJsonOrDefault(String addressPart, String defaultValue){
		if (jsonAddress == null){
			buildJSON();
		}
		if (jsonAddress.containsKey(addressPart)){
			Object val = jsonAddress.get(addressPart);
			if (val == null){
				return defaultValue;
			}else{
				String value = val.toString();
				if (value.isEmpty()){
					return defaultValue;
				}else{
					return value;
				}
			}
		}
		return defaultValue;
	}
	
	@Override
	public String toString(){
		if (jsonAddress == null){
			buildJSON();
		}
		return jsonAddress.toJSONString();
	}
	
	/**
	 * Build a JSONObject from data in class. Field can be null. 
	 */
	public JSONObject buildJSON(){
		jsonAddress = new JSONObject();
		if (country != null) 	JSON.add(jsonAddress, LOCATION.COUNTRY, country);
		if (state != null) 		JSON.add(jsonAddress, LOCATION.STATE, state);
		if (city != null) 		JSON.add(jsonAddress, LOCATION.CITY, city);
		if (postalCode != null) JSON.add(jsonAddress, LOCATION.POSTAL_CODE, postalCode);
		if (street != null) 	JSON.add(jsonAddress, LOCATION.STREET, street);
		if (streetNumber != null) 	JSON.add(jsonAddress, LOCATION.STREET_NBR, streetNumber);
		if (latitude != null) 	JSON.add(jsonAddress, LOCATION.LAT, latitude);
		if (longitude != null) 	JSON.add(jsonAddress, LOCATION.LNG, longitude);
		if (Is.notNullOrEmpty(latitude) && Is.notNullOrEmpty(longitude)){
			JSON.add(jsonAddress, "location", latitude + "," + longitude); 		//for Elasticsearch geo_point
		}
		if (name != null) 		JSON.add(jsonAddress, LOCATION.NAME, name);
		if (description != null) JSON.add(jsonAddress, PersonalInfo.DESCRIPTION, description);
		return jsonAddress;
	}

}
