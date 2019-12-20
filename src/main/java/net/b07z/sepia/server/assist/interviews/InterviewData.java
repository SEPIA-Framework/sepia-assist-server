package net.b07z.sepia.server.assist.interviews;

/**
 * An interview creates default results for parameters as JSON strings. This class shows the possible fields inside 
 * such an result JSON strings.
 * 
 * @author Florian Quirin
 *
 */
public class InterviewData {
	
	//tags to build default results
	
	//-shared
	public static final String INPUT = "input";					//in parameters where the input cannot be validated for sure you might find this
	public static final String INPUT_RAW = "input_raw";			//in parameters where even the normalization might be too much you can find this value
	public static final String VALUE = "value"; 				//value of the parameter, often used for numbers, results with only one field, etc. ...
	public static final String VALUE_REDUCED = "value_reduced";	//value of the parameter that has been reduced more aggressively. Sometimes that might be too much.
	public static final String VALUE_LOCAL = "value_local";		//value in the input language e.g. shoes (en) -> Schuhe (de)
	public static final String GENERALIZED = "generalized";		//general or abstract value like a language code for languages or simply a basic form of a word (went -> go)
	public static final String FOUND = "found";					//value found during extraction (before generalized)
	public static final String ACCOUNT = "account";				//value given in user account
	public static final String UNSPECIFIC = "unspecific";		//NOTE: general version of TIME_UNSPECIFIC. A value that is related to the parameter but cannot be well determined like "the next days"
	public static final String CACHE_ENTRY = "cache_entry";		//if the parameter can cache results this is the cache entry
	public static final String CACHE_DATA = "cache_data";		//if the parameter can cache results this is the cache data
	public static final String OPTIONS = "options";				//if there is more than one result ...
	public static final String ARRAY = "array";					//a number of values
	public static final String EXTRAS = "extras";				//custom JSON object that can be filled with anything parameter specific as required
	
	//-location
	public static final String LOCATION_LAT = "latitude";
	public static final String LOCATION_LNG = "longitude";
	public static final String LOCATION_STREET = "street";
	public static final String LOCATION_STREET_NBR = "s_nbr";
	public static final String LOCATION_CITY = "city";
	public static final String LOCATION_POSTAL_CODE = "code";
	public static final String LOCATION_STATE = "area_state";
	public static final String LOCATION_COUNTRY = "country";
	public static final String LOCATION_NAME = "name";
	public static final String LOCATION_ADDRESS_TEXT = "addressText"; 		//TODO: oh oh there is a camel-case name, search and replace (server and client?)
	public static final String LOCATION_IMAGE = "image";
	
	//-time
	public static final String DATE_DAY = "date_day";			//date day of event in default format
	public static final String DATE_TIME = "date_time";			//date time of event in default format
	public static final String DATE_INPUT = "date_input";		//unknown date input when day + time extraction fails
	public static final String TIME_DIFF = "diff"; 				//JSON string with time differences to now (hh, mm, ss, ...)
	public static final String TIME_TYPE = "type"; 				//type of the time input like "in 10 min." (duration) or "next week" (unspecific)
	public static final String TIME_UNSPECIFIC = "unspecific";	//a value that is related to the parameter but cannot be well determined like "the next days"
	
	//-numbers and amounts
	public static final String NUMBER_TYPE = "type"; 			//something like plain, currency, temperature, weight, (unit types...), ...
	
	//-radio
	public static final String RADIO_NAME = "name";				//name of the station
	public static final String RADIO_STREAM = "stream_url";		//stream of the station
	
	//-lists
	public static final String LIST_TYPE = "type";					//type of list
	public static final String LIST_TYPE_LOCALE = "type_locale";	//type of list in locale readable version
	public static final String LIST_SUBTYPE = "subtype";			//sub-type of list
	
	//-smart home and devices
	public static final String SMART_DEVICE_VALUE_TYPE = "type";	//something like number_plain, number_temperature, ...
	public static final String ITEM_INDEX = "index";				//index number of an item like device or room, e.g. Light 1, Room 1: index=1
}
