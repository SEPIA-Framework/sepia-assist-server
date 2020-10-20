package net.b07z.sepia.server.assist.interviews;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.services.Alarms;
import net.b07z.sepia.server.assist.services.ChatPreprocessor;
import net.b07z.sepia.server.assist.services.ClientControls;
import net.b07z.sepia.server.assist.services.ControlPreprocessor;
import net.b07z.sepia.server.assist.services.CustomFrameControl;
import net.b07z.sepia.server.assist.services.DictionaryTranslateBasic;
import net.b07z.sepia.server.assist.services.DirectionsGoogleMaps;
import net.b07z.sepia.server.assist.services.EventsWrapper;
import net.b07z.sepia.server.assist.services.FunCountToThree;
import net.b07z.sepia.server.assist.services.LanguageSwitcher;
import net.b07z.sepia.server.assist.services.Wikipedia;
import net.b07z.sepia.server.assist.services.Lists;
import net.b07z.sepia.server.assist.services.LocationSearchBasic;
import net.b07z.sepia.server.assist.services.MeshNodeConnector;
import net.b07z.sepia.server.assist.services.MusicRadioMixed;
import net.b07z.sepia.server.assist.services.MusicSearch;
import net.b07z.sepia.server.assist.services.NewsRssFeeds;
import net.b07z.sepia.server.assist.services.PlatformControls;
import net.b07z.sepia.server.assist.services.SentenceConnect;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.SmartHomeHubConnector;
import net.b07z.sepia.server.assist.services.WeatherDarkSky;
import net.b07z.sepia.server.assist.services.WeatherMeteoNorway;
import net.b07z.sepia.server.assist.services.WebsearchBasic;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;

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
	//NOTE1: service modules are stored as class-reference to dynamically create them inside interview
	//NOTE2: custom services can register themselves via ServiceInfo#getInfo (e.g. 'setCustomTriggerRegX')
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
				ServiceInterface service = (ServiceInterface) ClassBuilder.construct(s);
				//pseudo test
				ServiceInfo info = service.getInfo("");
				Debugger.println(" -- " + service.getClass().getSimpleName() + " - type: " + info.contentType, 2);
				nServ++;
			}
		}
		Debugger.println(nServ + " services for " + nCmd + " commands: All valid!", 3);
		return true; 	//will fail if class not exists
	}
	
	/**
	 * Get all commands that are supported (mapped to a service) in the previously given insertion order.
	 * The order matters because of the first-one-wins-at-equal-score rule.
	 */
	public static Set<String> getAllMappedCommands(){
		return systemInterviewServicesMap.keySet();
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
		
		String redirect = RedirectResult.class.getCanonicalName();
		
		//NEWS
		ArrayList<String> news = new ArrayList<String>();
			news.add(NewsRssFeeds.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.NEWS, news);
		//WEATHER
		ArrayList<String> weather = new ArrayList<String>();
			if (Is.notNullOrEmpty(Config.forecast_io_key)){
				weather.add(WeatherDarkSky.class.getCanonicalName());
			}else{
				weather.add(WeatherMeteoNorway.class.getCanonicalName());
			}
			systemInterviewServicesMap.put(CMD.WEATHER, weather);
		//SMART-DEVICE CONTROL
		ArrayList<String> controlSmarthome = new ArrayList<String>();
			controlSmarthome.add(SmartHomeHubConnector.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.SMARTDEVICE, controlSmarthome);
		//DIRECTIONS
		ArrayList<String> directions = new ArrayList<String>();
			directions.add(DirectionsGoogleMaps.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.DIRECTIONS, directions);
		//FASHION SHOPPING
		ArrayList<String> fashion = new ArrayList<String>();
			fashion.add(redirect);										//ShoppingFashionAffilinet.class.getCanonicalName()
			systemInterviewServicesMap.put(CMD.FASHION, fashion);
		//FOOD DELIVERY
		ArrayList<String> food = new ArrayList<String>();
			food.add(redirect);											//FoodDeliveryUrl.class.getCanonicalName()
			systemInterviewServicesMap.put(CMD.FOOD, food);
		//KONWLEDGEBASE
		ArrayList<String> kb = new ArrayList<String>();
			kb.add(Wikipedia.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.KNOWLEDGEBASE, kb);
		//WEBSEARCH
		ArrayList<String> websearch = new ArrayList<String>();
			websearch.add(WebsearchBasic.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.WEB_SEARCH, websearch);
		//TIMER
		ArrayList<String> timer = new ArrayList<String>();
			timer.add(Alarms.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.TIMER, timer);
		//MUSIC RADIO
		ArrayList<String> radio = new ArrayList<String>();
			radio.add(MusicRadioMixed.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.MUSIC_RADIO, radio);
		//MUSIC SEARCH
		ArrayList<String> music = new ArrayList<String>();
			music.add(MusicSearch.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.MUSIC, music);
		//LISTS
		ArrayList<String> list = new ArrayList<String>();
			list.add(Lists.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.LISTS, list);
		
		//BANKING
		ArrayList<String> banking = new ArrayList<String>();
			banking.add(redirect);
			systemInterviewServicesMap.put(CMD.BANKING, banking);
		//CAR WELCOME UPDATE
		ArrayList<String> carWelcome = new ArrayList<String>();
			carWelcome.add(redirect);
			systemInterviewServicesMap.put(CMD.CAR_WELCOME_UPDATE, carWelcome);
		//CHAT
		ArrayList<String> chat = new ArrayList<String>();
			chat.add(ChatPreprocessor.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.CHAT, chat);
		//CONTROL DEVICES
		ArrayList<String> controlDevice = new ArrayList<String>();
			controlDevice.add(ControlPreprocessor.class.getCanonicalName()); //.add("apis.ControlPreprocessor");
			systemInterviewServicesMap.put(CMD.CONTROL, controlDevice);
		//COUNT - EASTER-EGG
		ArrayList<String> count = new ArrayList<String>();
			count.add(FunCountToThree.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.COUNT, count);
		//DICTIONARY
		ArrayList<String> dict = new ArrayList<String>();
			dict.add(DictionaryTranslateBasic.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.DICT_TRANSLATE, dict);
		//LOCATIONS
		ArrayList<String> locations = new ArrayList<String>();
			locations.add(LocationSearchBasic.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.LOCATION, locations);
		//MOBILITY
		ArrayList<String> mobility = new ArrayList<String>();
			mobility.add(DirectionsGoogleMaps.class.getCanonicalName()); //Mobility_Qixxit
			systemInterviewServicesMap.put(CMD.MOBILITY, mobility);
		//MOVIES
		ArrayList<String> movies = new ArrayList<String>();
			movies.add(redirect); //.add("apis.Movies_iTunes");
			systemInterviewServicesMap.put(CMD.MOVIES, movies);
		//TV program
		ArrayList<String> tvProgram = new ArrayList<String>();
			tvProgram.add(redirect);									//TvProgramUrlDe.class.getCanonicalName()
			systemInterviewServicesMap.put(CMD.TV_PROGRAM, tvProgram);
		//FLIGHTS
		ArrayList<String> flights = new ArrayList<String>();
			flights.add(redirect); 										//Flights
			systemInterviewServicesMap.put(CMD.FLIGHTS, flights);
		//HOTELS
		ArrayList<String> hotels = new ArrayList<String>();
			hotels.add(redirect); 										//Hotels
			systemInterviewServicesMap.put(CMD.HOTELS, hotels);
		//TICKETS
		ArrayList<String> tickets = new ArrayList<String>();
			tickets.add(redirect); 										//Tickets
			systemInterviewServicesMap.put(CMD.TICKETS, tickets);

		//SENTENCE
		ArrayList<String> sentence_connect = new ArrayList<String>();
			sentence_connect.add(SentenceConnect.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.SENTENCE_CONNECT, sentence_connect);
		//MESH NODE PLUGIN CONNECTOR
		ArrayList<String> mesh_node_plugin = new ArrayList<String>();
			mesh_node_plugin.add(MeshNodeConnector.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.MESH_NODE_PLUGIN, mesh_node_plugin);
		//CLIENT CONTROLS
		ArrayList<String> client_controls = new ArrayList<String>();
		client_controls.add(ClientControls.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.CLIENT_CONTROLS, client_controls);
		//PLATFORM CONTROLS (of CLIENT)
		ArrayList<String> platform_controls = new ArrayList<String>();
		platform_controls.add(PlatformControls.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.PLATFORM_CONTROLS, platform_controls);
		//FRAME CONTROL
		ArrayList<String> frame_control = new ArrayList<String>();
		frame_control.add(CustomFrameControl.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.FRAME_CONTROL, frame_control);
		//LANGUAGE SWITCH 
		ArrayList<String> langSwitch = new ArrayList<String>();
		langSwitch.add(LanguageSwitcher.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.LANGUAGE_SWITCH, langSwitch);
		//PARROT (REPEAT USER)
		ArrayList<String> parrot = new ArrayList<String>();
			parrot.add(RepeatMe.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.REPEAT_ME, parrot);
		//FEEDBACK - NPS
		ArrayList<String> feedback_nps = new ArrayList<String>();
			feedback_nps.add(redirect);									//FeedbackNPS.class.getCanonicalName()
			systemInterviewServicesMap.put(CMD.FEEDBACK_NPS, feedback_nps);
		//EVENTS SERVICE
		ArrayList<String> events = new ArrayList<String>();
			events.add(EventsWrapper.class.getCanonicalName());
			systemInterviewServicesMap.put(CMD.EVENTS_PERSONAL, events);
			
		Debugger.println("finished loading services mapping for "+ systemInterviewServicesMap.size() + " interview modules.", 3);
	}

}
