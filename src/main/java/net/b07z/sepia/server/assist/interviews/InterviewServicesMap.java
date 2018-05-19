package net.b07z.sepia.server.assist.interviews;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.b07z.sepia.server.assist.apis.Alarms_Basic;
import net.b07z.sepia.server.assist.apis.ApiInfo;
import net.b07z.sepia.server.assist.apis.ApiInterface;
import net.b07z.sepia.server.assist.apis.Chat_Preprocessor;
import net.b07z.sepia.server.assist.apis.Dictionary_Linguee;
import net.b07z.sepia.server.assist.apis.Directions_Default;
import net.b07z.sepia.server.assist.apis.Events_Service;
import net.b07z.sepia.server.assist.apis.Feedback_NPS;
import net.b07z.sepia.server.assist.apis.FoodDelivery_Basic;
import net.b07z.sepia.server.assist.apis.Fun_Count;
import net.b07z.sepia.server.assist.apis.Knowledgebase_Wiki;
import net.b07z.sepia.server.assist.apis.Lists_Basic;
import net.b07z.sepia.server.assist.apis.Location_Mapsearch;
import net.b07z.sepia.server.assist.apis.Music_Radio_Mixed;
import net.b07z.sepia.server.assist.apis.News_RssFeeds;
import net.b07z.sepia.server.assist.apis.SentenceConnect;
import net.b07z.sepia.server.assist.apis.ShoppingFashion_Affilinet;
import net.b07z.sepia.server.assist.apis.SmartDevice_Default;
import net.b07z.sepia.server.assist.apis.TV_Program_Default;
import net.b07z.sepia.server.assist.apis.Weather_DarkSky;
import net.b07z.sepia.server.assist.apis.Websearch_Default;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Connects system default commands to default services. 
 * @see net.b07z.sepia.server.assist.server.ConfigServices
 * 
 * @author Florian Quirin
 *
 */
public class InterviewServicesMap {
	
	private static boolean loadCustom = false; 		//use this via "loadCustom(..)" in the server "Start" class
	
	//Interview -> services
	//note: service modules are stored as class-reference to dynamically create them inside interview
	private static Map<String, List<String>> systemInterviewServicesMap = new LinkedHashMap<>();
	
	/**
	 * Get interviewService map. 
	 * @return
	 */
	public static Map<String, List<String>> get(){
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
		for (Entry<String, List<String>> es : systemInterviewServicesMap.entrySet()){
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
	 * Load custom map of system service-modules to the interviewService map and prevent loading of defaults.<br>
	 * ATTENTION: Please note that the order matters so a LinkedHashMap is recommended!
	 */
	public static void loadCustom(Map<String, List<String>> interviewServicesMap){
		systemInterviewServicesMap = interviewServicesMap;
		loadCustom = true;
		Debugger.println("finished loading services mapping for "+ systemInterviewServicesMap.size() + " interview modules.", 3);
	}
	
	/**
	 * Clear custom map and reload default.
	 */
	public static void loadDefault(){
		loadCustom = false;
		systemInterviewServicesMap = new LinkedHashMap<>();
		load();
	}
	
	/**
	 * Load all system service-modules to the interviewService map in a SPECIFIC order.
	 */
	public static void load(){
		//If we have a custom map we don't load this
		if (loadCustom){
			return;
		}
		
		//TODO: rearrange the order! If this map is used to iterate and the scores for each service are equal the first one wins.
		
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
			chat.add(Chat_Preprocessor.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.CHAT, chat);
		//CONTROL DEVICES
		ArrayList<String> controlDevice = new ArrayList<String>();
			controlDevice.add(RedirectResult.class.getCanonicalName()); //.add("apis.Control_Preprocessor");
			systemInterviewServicesMap.put(CMD.CONTROL, controlDevice);
		//SMARTHOME CONTROL
		ArrayList<String> controlSmarthome = new ArrayList<String>();
			controlSmarthome.add(SmartDevice_Default.class.getCanonicalName()); //.add("apis.Control_Preprocessor");
			systemInterviewServicesMap.put(CMD.SMARTDEVICE, controlSmarthome);
		//COUNT - EASTER-EGG
		ArrayList<String> count = new ArrayList<String>();
			count.add(Fun_Count.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.COUNT, count);
		//DICTIONARY
		ArrayList<String> dict = new ArrayList<String>();
			dict.add(Dictionary_Linguee.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.DICT_TRANSLATE, dict);
		//DIRECTIONS
		ArrayList<String> directions = new ArrayList<String>();
			directions.add(Directions_Default.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.DIRECTIONS, directions);
		//LOCATIONS
		ArrayList<String> locations = new ArrayList<String>();
			locations.add(Location_Mapsearch.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.LOCATION, locations);
		//MOBILITY
		ArrayList<String> mobility = new ArrayList<String>();
			mobility.add(Directions_Default.class.getCanonicalName()); //Mobility_Qixxit
			systemInterviewServicesMap.put(CMD.MOBILITY, mobility);
		//KONWLEDGEBASE
		ArrayList<String> kb = new ArrayList<String>();
			kb.add(Knowledgebase_Wiki.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.KNOWLEDGEBASE, kb);
		//MOVIES
		ArrayList<String> movies = new ArrayList<String>();
			movies.add(RedirectResult.class.getCanonicalName()); //.add("apis.Movies_iTunes");
			systemInterviewServicesMap.put(CMD.MOVIES, movies);
		//TV program
		ArrayList<String> tvProgram = new ArrayList<String>();
			tvProgram.add(TV_Program_Default.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.TV_PROGRAM, tvProgram);
		//LISTS
		ArrayList<String> list = new ArrayList<String>();
			list.add(Lists_Basic.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.LISTS, list);
		//FLIGHTS
		ArrayList<String> flights = new ArrayList<String>();
			flights.add(RedirectResult.class.getCanonicalName()); 	//flights.add("apis.Flights_Expedia_v2");
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
			radio.add(Music_Radio_Mixed.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.MUSIC_RADIO, radio);
		//NEWS
		ArrayList<String> news = new ArrayList<String>();
			news.add(News_RssFeeds.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.NEWS, news);
		//FASHION SHOPPING
		ArrayList<String> fashion = new ArrayList<String>();
			fashion.add(ShoppingFashion_Affilinet.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.FASHION, fashion);
		//FOOD DELIVERY
		ArrayList<String> food = new ArrayList<String>();
			food.add(FoodDelivery_Basic.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.FOOD, food);
		//WEATHER
		ArrayList<String> weather = new ArrayList<String>();
			weather.add(Weather_DarkSky.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.WEATHER, weather);
		//WEBSEARCH
		ArrayList<String> websearch = new ArrayList<String>();
			websearch.add(Websearch_Default.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.WEB_SEARCH, websearch);
		//TIMER
		ArrayList<String> timer = new ArrayList<String>();
			timer.add(Alarms_Basic.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.TIMER, timer);
		//SENTENCE
		ArrayList<String> sentence_connect = new ArrayList<String>();
			sentence_connect.add(SentenceConnect.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.SENTENCE_CONNECT, sentence_connect);
		//PARROT (REPEAT USER)
		ArrayList<String> parrot = new ArrayList<String>();
			parrot.add(Repeat_Me.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.REPEAT_ME, parrot);
		//FEEDBACK - NPS
		ArrayList<String> feedback_nps = new ArrayList<String>();
			feedback_nps.add(Feedback_NPS.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.FEEDBACK_NPS, feedback_nps);
		//EVENTS SERVICE
		ArrayList<String> events = new ArrayList<String>();
			events.add(Events_Service.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.EVENTS_PERSONAL, events);
			
		Debugger.println("finished loading services mapping for "+ systemInterviewServicesMap.size() + " interview modules.", 3);
	}

}
