package net.b07z.sepia.server.assist.interviews;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.b07z.sepia.server.assist.apis.ApiInfo;
import net.b07z.sepia.server.assist.apis.ApiInterface;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Connects system default commands to default services. 
 * 
 * @author Florian Quirin
 *
 */
public class InterviewServicesMap {
	
	private static final String sepiaPackage = Config.parentPackage;
	
	private static boolean loadCustom = false;
	
	//Interview -> services
	//note: service modules are stored as class-reference to dynamically create them inside interview
	private static Map<String, ArrayList<String>> systemInterviewServicesMap = new HashMap<>();
	
	/**
	 * Get interviewService map. 
	 * @return
	 */
	public static Map<String, ArrayList<String>> get(){
		return systemInterviewServicesMap;
	}
	
	/**
	 * Test if all classes can be loaded.
	 * @return true or throw error
	 */
	public static boolean test(){
		Debugger.println("Testing services for supported commands...", 3);
		int nCmd = 0;
		int nServ = 0;
		for (Entry<String, ArrayList<String>> es : systemInterviewServicesMap.entrySet()){
			String command = es.getKey();
			Debugger.println("CMD: " + command, 2);
			nCmd++;
			for (String s : es.getValue()){
				ApiInterface service = (ApiInterface) ClassBuilder.construct(s);
				//pseudo test
				ApiInfo info = service.getInfo("");
				Debugger.println(" -- " + service.getClass().getSimpleName() + " - type: " + info.contentType, 2);
				nServ++;
			}
		}
		Debugger.println(nServ + " services for " + nCmd + " commands: All valid!", 3);
		return true; 	//will fail if class not exists
	}
	
	/**
	 * Load custom map of system service-modules to the interviewService map and prevent loading of defaults.
	 */
	public static void loadCustom(Map<String, ArrayList<String>> interviewServicesMap){
		systemInterviewServicesMap = interviewServicesMap;
		loadCustom = true;
		Debugger.println("finished loading services mapping for "+ systemInterviewServicesMap.size() + " interview modules.", 3);
	}
	
	/**
	 * Clear custom map and reload default.
	 */
	public static void loadDefault(){
		loadCustom = false;
		systemInterviewServicesMap = new HashMap<>();
		load();
	}
	
	/**
	 * Load all system service-modules to the interviewService map.
	 */
	public static void load(){
		//If we have a custom map we don't load this
		if (loadCustom){
			return;
		}
		
		//BANKING
		ArrayList<String> banking = new ArrayList<String>();
			banking.add(RedirectResult.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.BANKING, banking);
		//CAR WELCOME UPDATE
		ArrayList<String> carWelcome = new ArrayList<String>();
			carWelcome.add(RedirectResult.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.CAR_WELCOME_UPDATE, carWelcome);
		//CHAT
		ArrayList<String> chat = new ArrayList<String>();
			chat.add(sepiaPackage + ".apis.Chat_Preprocessor");
			systemInterviewServicesMap.put(CMD.CHAT, chat);
		//CONTROL DEVICES
		ArrayList<String> controlDevice = new ArrayList<String>();
			controlDevice.add(RedirectResult.class.getCanonicalName()); //.add("apis.Control_Preprocessor");
			systemInterviewServicesMap.put(CMD.CONTROL, controlDevice);
		//SMARTHOME CONTROL
		ArrayList<String> controlSmarthome = new ArrayList<String>();
			controlSmarthome.add(sepiaPackage + ".apis.SmartDevice_Default"); //.add("apis.Control_Preprocessor");
			systemInterviewServicesMap.put(CMD.SMARTDEVICE, controlSmarthome);
		//COUNT - EASTER-EGG
		ArrayList<String> count = new ArrayList<String>();
			count.add(sepiaPackage + ".apis.Fun_Count");
			systemInterviewServicesMap.put(CMD.COUNT, count);
		//DICTIONARY
		ArrayList<String> dict = new ArrayList<String>();
			dict.add(sepiaPackage + ".apis.Dictionary_Linguee");
			systemInterviewServicesMap.put(CMD.DICT_TRANSLATE, dict);
		//DIRECTIONS
		ArrayList<String> directions = new ArrayList<String>();
			directions.add(sepiaPackage + ".apis.Directions_Default");
			systemInterviewServicesMap.put(CMD.DIRECTIONS, directions);
		//LOCATIONS
		ArrayList<String> locations = new ArrayList<String>();
			locations.add(sepiaPackage + ".apis.Location_Mapsearch");
			systemInterviewServicesMap.put(CMD.LOCATION, locations);
		//MOBILITY
		ArrayList<String> mobility = new ArrayList<String>();
			mobility.add(sepiaPackage + ".apis.Directions_Default"); //Mobility_Qixxit
			systemInterviewServicesMap.put(CMD.MOBILITY, mobility);
		//KONWLEDGEBASE
		ArrayList<String> kb = new ArrayList<String>();
			kb.add(sepiaPackage + ".apis.Knowledgebase_Wiki");
			systemInterviewServicesMap.put(CMD.KNOWLEDGEBASE, kb);
		//MOVIES
		ArrayList<String> movies = new ArrayList<String>();
			movies.add(RedirectResult.class.getCanonicalName()); //.add("apis.Movies_iTunes");
			systemInterviewServicesMap.put(CMD.MOVIES, movies);
		//TV program
		ArrayList<String> tvProgram = new ArrayList<String>();
			tvProgram.add(sepiaPackage + ".apis.TV_Program_Default");
			systemInterviewServicesMap.put(CMD.TV_PROGRAM, tvProgram);
		//LISTS
		ArrayList<String> list = new ArrayList<String>();
			list.add(sepiaPackage + ".apis.Lists_Basic"); //.add("apis.Lists_Default");
			systemInterviewServicesMap.put(CMD.LISTS, list);
		//FLIGHTS
		ArrayList<String> flights = new ArrayList<String>();
			flights.add(RedirectResult.class.getCanonicalName()); 
			//flights.add("apis.Flights_Expedia_v2");
			//flights.add("apis.Flights_Test");
			systemInterviewServicesMap.put(CMD.FLIGHTS, flights);
		//HOTELS
		ArrayList<String> hotels = new ArrayList<String>();
			hotels.add(RedirectResult.class.getCanonicalName()); //.add("apis.Hotels_Expedia");
			systemInterviewServicesMap.put(CMD.HOTELS, hotels);
		//TICKETS
		ArrayList<String> tickets = new ArrayList<String>();
			tickets.add(RedirectResult.class.getCanonicalName()); //.add("apis.Tickets_Default");
			systemInterviewServicesMap.put(CMD.TICKETS, tickets);
		//MUSIC RADIO
		ArrayList<String> radio = new ArrayList<String>();
			radio.add(sepiaPackage + ".apis.Music_Radio_Mixed");
			systemInterviewServicesMap.put(CMD.MUSIC_RADIO, radio);
		//NEWS
		ArrayList<String> news = new ArrayList<String>();
			news.add(sepiaPackage + ".apis.News_RssFeeds");
			systemInterviewServicesMap.put(CMD.NEWS, news);
		//FASHION SHOPPING
		ArrayList<String> fashion = new ArrayList<String>();
			fashion.add(sepiaPackage + ".apis.ShoppingFashion_Affilinet");
			systemInterviewServicesMap.put(CMD.FASHION, fashion);
		//FOOD DELIVERY
		ArrayList<String> food = new ArrayList<String>();
			food.add(sepiaPackage + ".apis.FoodDelivery_Basic");
			systemInterviewServicesMap.put(CMD.FOOD, food);
		//WEATHER
		ArrayList<String> weather = new ArrayList<String>();
			weather.add(sepiaPackage + ".apis.Weather_DarkSky");
			systemInterviewServicesMap.put(CMD.WEATHER, weather);
		//WEBSEARCH
		ArrayList<String> websearch = new ArrayList<String>();
			websearch.add(sepiaPackage + ".apis.Websearch_Default");
			systemInterviewServicesMap.put(CMD.WEB_SEARCH, websearch);
		//TIMER
		ArrayList<String> timer = new ArrayList<String>();
			timer.add(sepiaPackage + ".apis.Alarms_Basic");
			systemInterviewServicesMap.put(CMD.TIMER, timer);
		//SENTENCE
		ArrayList<String> sentence_connect = new ArrayList<String>();
			sentence_connect.add(sepiaPackage + ".apis.SentenceConnect");
			systemInterviewServicesMap.put(CMD.SENTENCE_CONNECT, sentence_connect);
		//PARROT (REPEAT USER)
		ArrayList<String> parrot = new ArrayList<String>();
			parrot.add(sepiaPackage + ".interviews.Repeat_Me");
			systemInterviewServicesMap.put(CMD.REPEAT_ME, parrot);
		//FEEDBACK - NPS
		ArrayList<String> feedback_nps = new ArrayList<String>();
			feedback_nps.add(sepiaPackage + ".apis.Feedback_NPS");
			systemInterviewServicesMap.put(CMD.FEEDBACK_NPS, feedback_nps);
		//EVENTS SERVICE
		ArrayList<String> events = new ArrayList<String>();
			events.add(sepiaPackage + ".apis.Events_Service");
			systemInterviewServicesMap.put(CMD.EVENTS_PERSONAL, events);
			
		Debugger.println("finished loading services mapping for "+ systemInterviewServicesMap.size() + " interview modules.", 3);
	}

}
