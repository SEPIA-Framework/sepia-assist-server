package net.b07z.sepia.server.assist.parameters;

import java.util.HashMap;
import java.util.Map.Entry;

import net.b07z.sepia.server.assist.data.Parameter;
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
		handlerToParameter.put(PARAMETERS.YES_NO, YesNo.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.NUMBER, net.b07z.sepia.server.assist.parameters.Number.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.ACTION, Action.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.PLACE, Place.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.TIME, DateAndTime.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.CLOCK, DateClock.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.ALARM_NAME, AlarmName.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.ALARM_TYPE, AlarmType.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.LOCATION_START, LocationStart.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.LOCATION_END, LocationEnd.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.LOCATION_WAYPOINT, LocationWaypoint.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.LIST_ITEM, ListItem.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.LIST_TYPE, ListType.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.LIST_SUBTYPE, ListSubType.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.TRAVEL_TYPE, TravelType.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.TRAVEL_REQUEST_INFO, TravelRequestInfo.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.FASHION_ITEM, FashionItem.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.FASHION_SIZE, FashionSize.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.FASHION_BRAND, FashionBrand.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.COLOR, Color.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.GENDER, Gender.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.NEWS_SECTION, NewsSection.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.NEWS_TYPE, NewsType.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.SPORTS_TEAM, SportsTeam.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.SPORTS_LEAGUE, SportsLeague.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.FOOD_ITEM, FoodItem.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.FOOD_CLASS, FoodClass.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.RADIO_STATION, RadioStation.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.MUSIC_GENRE, MusicGenre.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.MUSIC_ARTIST, MusicArtist.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.MUSIC_SERVICE, MusicService.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.MUSIC_ALBUM, MusicAlbum.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.SONG, Song.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.PLAYLIST_NAME, PlaylistName.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.SMART_DEVICE, SmartDevice.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.SMART_DEVICE_VALUE, SmartDeviceValue.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.ROOM, Room.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.WEBSEARCH_REQUEST, WebSearchRequest.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.WEBSEARCH_ENGINE, WebSearchEngine.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.SEARCH_SECTION, SearchSection.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.CLIENT_FUN, ClientFunction.class.getCanonicalName());
		//more client/platform helpers (simple generic parameters)
		handlerToParameter.put(PARAMETERS.ANDROID_FUN, GenericEmptyParameter.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.IOS_FUN, GenericEmptyParameter.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.BROWSER_FUN, GenericEmptyParameter.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.WINDOWS_FUN, GenericEmptyParameter.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.DEVICE_FUN, GenericEmptyParameter.class.getCanonicalName());
		//Generics / Exceptions / Specials
		handlerToParameter.put(PARAMETERS.CONFIRMATION, Confirm.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.SELECTION, Select.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.SENTENCES, Sentences.class.getCanonicalName());
		handlerToParameter.put(PARAMETERS.DATA, GenericEmptyParameter.class.getCanonicalName());
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
