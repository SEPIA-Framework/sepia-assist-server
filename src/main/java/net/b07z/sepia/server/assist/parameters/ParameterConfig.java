package net.b07z.sepia.server.assist.parameters;

import java.util.HashMap;
import java.util.Map.Entry;

import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Save some data here like the mapping from parameter.name to parameterHandler.
 * 
 * @author Florian Quirin
 *
 */
public class ParameterConfig {
	
	static HashMap<String, String> handlerToParameter = new HashMap<>();
	
	/**
	 * Test if all parameters have a valid handler.
	 * @return true or throw error
	 */
	public static boolean test(){
		Debugger.println("Testing parameters handlers...", 3);
		int nP = 0;
		for (Entry<String, String> e : handlerToParameter.entrySet()){
			String name = e.getKey();
			ParameterHandler ph = new Parameter(name).getHandler();
			//pseudo test
			ph.buildSuccess();
			Debugger.println("Parameter '" + name + "' is valid.", 2);
			nP++;
		}
		Debugger.println(nP + " parameters: All valid!", 3);
		return true;
	}
	
	/**
	 * Setup parameters by mapping the handler-class.
	 */
	public static void setup(){
		//NOTE: If you ever rename any of the handlers this breaks! (use the test method!)
		handlerToParameter.put(PARAMETERS.YES_NO, Config.parentPackage + ".parameters.YesNo");
		handlerToParameter.put(PARAMETERS.CONFIRMATION, Config.parentPackage + ".parameters.Confirm");
		handlerToParameter.put(PARAMETERS.NUMBER, net.b07z.sepia.server.assist.parameters.Number.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.ACTION, Action.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.ALARM_NAME, Config.parentPackage + ".parameters.AlarmName");
		handlerToParameter.put(PARAMETERS.LOCATION_START, Config.parentPackage + ".parameters.LocationStart");
		handlerToParameter.put(PARAMETERS.LOCATION_END, Config.parentPackage + ".parameters.LocationEnd");
		handlerToParameter.put(PARAMETERS.LOCATION_WAYPOINT, Config.parentPackage + ".parameters.LocationWaypoint");
		handlerToParameter.put(PARAMETERS.LIST_ITEM, Config.parentPackage + ".parameters.ListItem");
		handlerToParameter.put(PARAMETERS.LIST_TYPE, Config.parentPackage + ".parameters.ListType");
		handlerToParameter.put(PARAMETERS.LIST_SUBTYPE, Config.parentPackage + ".parameters.ListSubType");
		handlerToParameter.put(PARAMETERS.TRAVEL_TYPE, Config.parentPackage + ".parameters.TravelType");
		handlerToParameter.put(PARAMETERS.TRAVEL_REQUEST_INFO, Config.parentPackage + ".parameters.TravelRequestInfo");
		handlerToParameter.put(PARAMETERS.PLACE, Config.parentPackage + ".parameters.Place");
		handlerToParameter.put(PARAMETERS.TIME, Config.parentPackage + ".parameters.DateAndTime");
		handlerToParameter.put(PARAMETERS.CLOCK, Config.parentPackage + ".parameters.DateClock");
		handlerToParameter.put(PARAMETERS.ALARM_TYPE, Config.parentPackage + ".parameters.AlarmType");
		handlerToParameter.put(PARAMETERS.FASHION_ITEM, Config.parentPackage + ".parameters.FashionItem");
		handlerToParameter.put(PARAMETERS.FASHION_SIZE, Config.parentPackage + ".parameters.FashionSize");
		handlerToParameter.put(PARAMETERS.FASHION_BRAND, Config.parentPackage + ".parameters.FashionBrand");
		handlerToParameter.put(PARAMETERS.COLOR, Config.parentPackage + ".parameters.Color");
		handlerToParameter.put(PARAMETERS.GENDER, Config.parentPackage + ".parameters.Gender");
		handlerToParameter.put(PARAMETERS.NEWS_SECTION, Config.parentPackage + ".parameters.NewsSection");
		handlerToParameter.put(PARAMETERS.NEWS_TYPE, Config.parentPackage + ".parameters.NewsType");
		handlerToParameter.put(PARAMETERS.SPORTS_TEAM, Config.parentPackage + ".parameters.SportsTeam");
		handlerToParameter.put(PARAMETERS.SPORTS_LEAGUE, Config.parentPackage + ".parameters.SportsLeague");
		handlerToParameter.put(PARAMETERS.FOOD_ITEM, Config.parentPackage + ".parameters.FoodItem");
		handlerToParameter.put(PARAMETERS.FOOD_CLASS, Config.parentPackage + ".parameters.FoodClass");
		handlerToParameter.put(PARAMETERS.RADIO_STATION, Config.parentPackage + ".parameters.RadioStation");
		handlerToParameter.put(PARAMETERS.MUSIC_GENRE, Config.parentPackage + ".parameters.MusicGenre");
		handlerToParameter.put(PARAMETERS.SMART_DEVICE, SmartDevice.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.SMART_DEVICE_VALUE, SmartDeviceValue.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.ROOM, Room.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.WEBSEARCH_REQUEST, Config.parentPackage + ".parameters.WebSearchRequest");
		handlerToParameter.put(PARAMETERS.WEBSEARCH_ENGINE, Config.parentPackage + ".parameters.WebSearchEngine");
		handlerToParameter.put(PARAMETERS.SEARCH_SECTION, Config.parentPackage + ".parameters.SearchSection");
		handlerToParameter.put(PARAMETERS.CLIENT_FUN, ClientFunction.class.getCanonicalName());
		//Generics / Exceptions / Specials
		handlerToParameter.put(PARAMETERS.SENTENCES, Config.parentPackage + ".parameters.Sentences");
		handlerToParameter.put(PARAMETERS.REPLY, GenericParameter.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.REPLY_SUCCESS, GenericParameter.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.REPLY_FAIL, GenericParameter.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.MESH_NODE_URL, GenericParameter.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.MESH_NODE_PLUGIN_NAME, GenericParameter.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.MESH_NODE_PLUGIN_DATA, GenericParameter.class.getCanonicalName());
	}
	/**
	 * Add another parameter handler.
	 * @param parameterName - name taken from 'PARAMETERS'
	 * @param parameterHandler - a class given as full string name (including package name etc.)
	 */
	public static void setHandler(String parameterName, String parameterHandler){
		handlerToParameter.put(parameterName, parameterHandler);
	}
	
	/**
	 * Get system handler for parameter or return generic handler and log a warning.
	 * @param p - name taken from 'PARAMETERS'
	 */
	public static String getHandler(String p){
		String h = handlerToParameter.get(p);
		if (h == null || h.isEmpty()){
			//throw new RuntimeException(DateTime.getLogDate() + " ERROR - ParameterConfig.java / getHandler() - NO HANDLER for: " + p);
			//return generic handler
			Debugger.println("ParameterConfig.getHandler() - NO HANDLER for: " + p, 3);
			return GenericParameter.class.getCanonicalName();
		}
		return h;
	}
	
	/**
	 * Check if the parameter has a system handler.
	 * @param p - name taken from 'PARAMETERS'
	 * @return true/false
	 */
	public static boolean hasHandler(String p){
		return handlerToParameter.containsKey(p);
	}

}
