package net.b07z.sepia.server.assist.interpreters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.b07z.sepia.server.assist.answers.AnswerTools;
import net.b07z.sepia.server.assist.assistant.LOCATION;
import net.b07z.sepia.server.assist.data.Address;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interpreters.RegexParameterSearch;
import net.b07z.sepia.server.assist.server.ConfigTestServer;
import net.b07z.sepia.server.assist.tools.StringCompare;
import net.b07z.sepia.server.assist.users.User;

public class Test_Methods {

	public static void main(String[] args) {
		
		//time
		System.out.println("system time: " + Math.round(System.currentTimeMillis()/1000));
		
		//fake input
		NluInput input = ConfigTestServer.getFakeInput("test", "de");
		User user = ConfigTestServer.getTestUser(ConfigTestServer.email_id1, input, false, true);
		//input.user_location = "<country>Germany<city>Essen<street>Somestreet 1<code>45138<latitude>51.6<longitude>7.1";
		input.user = user;
		
		//use
		String text = "";
		//String date = "";
		
		//test sentence
		System.out.println("-----SENTENCE TESTING------");
		text = "bring me to the street called A to B";	System.out.println(text);	printHM(RegexParameterSearch.get_locations(text, "en"));
		text = "bring me to the closest supermarket";			System.out.println(text);	printHM(RegexParameterSearch.get_locations(text, "en"));
		text = "show me the address of the statute of liberty";		System.out.println(text);	printHM(RegexParameterSearch.get_locations(text, "en"));
		System.out.println("-----------");
		text = "bring mich zu der strasse namens A zu B";	System.out.println(text);	printHM(RegexParameterSearch.get_locations(text, "de"));
		text = "bring mich vom schoenen zu hause zur schrecklichen arbeit";			System.out.println(text);	printHM(RegexParameterSearch.get_locations(text, "de"));
		text = "weg nach berlin am montag";					System.out.println(text);	printHM(RegexParameterSearch.get_locations(text, "de"));
		System.out.println("-----------");
		text = "search online for the best price for coffee";		System.out.println(text);	printHM(RegexParameterSearch.get_search(text, "en"));
		text = "search the best price for coffee online";			System.out.println(text);	printHM(RegexParameterSearch.get_search(text, "en"));
		System.out.println("-----------");
		text = "suche online nach dem besten preis für kaffee";		System.out.println(text);	printHM(RegexParameterSearch.get_search(text, "de"));
		text = "suche den besten preis fuer kaffe online";			System.out.println(text);	printHM(RegexParameterSearch.get_search(text, "de"));
		System.out.println("-----------");
		text = "how is the weather at the 10/31/2015?";		System.out.println(text);	printHM(RegexParameterSearch.get_date(text, "en"));
		text = "how is the weather tomorrow?";				System.out.println(text);	printHM(RegexParameterSearch.get_date(text, "en"));
		text = "how is the weather on monday night?";		System.out.println(text);	printHM(RegexParameterSearch.get_date(text, "en"));
		System.out.println("-----------");
		text = "ich suche ein italienisches restaurant in der naehe meiner arbeit";		System.out.println(text);	printHM(RegexParameterSearch.get_locations(text, "de"));
		text = "ich suche ein guenstiges hotel in berlin fuer naechste woche";			System.out.println(text);	printHM(RegexParameterSearch.get_locations(text, "de"));
		text = "I'm looking for an italian restaurant in berlin close to my work";		System.out.println(text);	printHM(RegexParameterSearch.get_locations(text, "en"));
		System.out.println("-----------");
		text = "erinnere mich daran in einer stunde die kids abzuholen";		System.out.println(text);	printHM(RegexParameterSearch.get_date(text, "de"));
		text = "remind me to fetch the kids in 1 hour";		System.out.println(text);	printHM(RegexParameterSearch.get_date(text, "en"));
		System.out.println("-----------");
		text = "zeig mir suzuki motorräder von 2000 bis 4000 €";		System.out.println(text);	printHM(RegexParameterSearch.get_amount_of(text, "money", "de"));
		text = "zeig mir suzuki motorräder mit 50ps bis 80ps";		System.out.println(text);	printHM(RegexParameterSearch.get_amount_of(text, "power", "de"));
		text = "zeig mir alles von 1.3kg bis 2.7kg";		System.out.println(text);	printHM(RegexParameterSearch.get_amount_of(text, "weight", "de"));
		text = "zeig mir alles was mehr als 299.99 euro kostet";		System.out.println(text);	printHM(RegexParameterSearch.get_amount_of(text, "money", "de"));
		text = "zeig mir alles was weniger wiegt als 50kg";		System.out.println(text);	printHM(RegexParameterSearch.get_amount_of(text, "weight", "de"));
		text = "zeig mir alles dessen verbrauch niedriger ist als 5l";		System.out.println(text);	printHM(RegexParameterSearch.get_amount_of(text, "energy", "de"));
		text = "zeig mir alles was neuer ist als 5 jahre";		System.out.println(text);	printHM(RegexParameterSearch.get_age(text, "de"));
		text = "zeig mir alles zwischen 1999 und 2009";		System.out.println(text);	printHM(RegexParameterSearch.get_age(text, "de"));
		text = "mit einer laufleistung von maximal 200000 kilometern";		System.out.println(text);	printHM(RegexParameterSearch.get_amount_of(text, "distance", "de"));
		text = "mit einer laufleistung von 100000 bis 200000 kilometern";		System.out.println(text);	printHM(RegexParameterSearch.get_amount_of(text, "distance", "de"));
		text = "mit einem preis von 2000 euro von 1999";		System.out.println(text);	printHM(RegexParameterSearch.get_age(text, "de"));
		text = "suche ein motorrad in blau das weniger als 2000 euro kostet und mehr als 50ps hat";		System.out.println(text);	printHM(RegexParameterSearch.get_amount_of(text, "power", "de"));
		text = "show me suzuki motorcycles from 2000 to 4000 dollar";		System.out.println(text);	printHM(RegexParameterSearch.get_amount_of(text, "money", "en"));
		System.out.println(RegexParameterSearch.convert_amount_to_default("money", "dollar", "2000<to>4000", "de"));
		text = "show me suzuki motorcycles that weight less than 300 pounds";		System.out.println(text);	printHM(RegexParameterSearch.get_amount_of(text, "weight", "en"));
		text = "show me suzuki motorcycles with >50 horsepowers";		System.out.println(text);	printHM(RegexParameterSearch.get_amount_of(text, "power", "en"));
		text = "show me motorcycles that are not older than 5 years";		System.out.println(text);	printHM(RegexParameterSearch.get_age(text, "en"));
		System.out.println("-----------");

		
		//test occurrence method
		text = "this *** is my *** ***.";/*
		int count = NLU_Tools.countOccurrenceOf(text, "***");
		System.out.println("count: " + count);
		System.out.println("-----------");*/
		
		//test answer sets
		String answer_set = "it works||<test_0a>||<direct> yes it works <user_name>";
		System.out.println("answer key: " + AnswerTools.handleUserAnswerSets(answer_set));
		System.out.println("-----------");
		
		//test geocoding
		String address = "Hamburg";
		//String address = "walk of fame, hollywood";
		System.out.println("----TEST GEOCODING FOR: " + address + " ----");
		//Tools_GeoCoding.test_get_coordinates(address, "de");
		
		String lat = "40.714224";	String lng = "-73.961452";		//New York random address
		//String lat = "41.016103"; 	String lng = "28.959086";	//Istanbul random address
		System.out.println("----TEST REVERSE GEOCODING FOR: " + lat + ", " + lng + " ----");
		//Tools_GeoCoding.test_get_address(lat, lng, "de");
		
		System.out.println("-----------");
		/*
		HashMap<String, Object> coords = Tools_GeoCoding.get_coordinates(address, "de"); 
		System.out.println(coords);
		System.out.println("latitude? " + (coords.get("latitude")!=null) + "; city? " + (coords.get("city")!=null));
		System.out.println("-----------");
		*/
		
		//test user locations
		user.userLocation.country = "South Africa";
		user.userLocation.city = "Kapstadt";
		user.userLocation.street = "rugby rd";
		user.userLocation.latitude = "-33.95";
		user.userLocation.longitude = "18.46";
		System.out.println("city: " + user.userLocation.getFromJsonOrDefault(LOCATION.CITY, ""));
		
		System.out.println("------Contains user locations?-----");
		text = "this test must be false";
		String user_param = User.containsUserSpecificLocation(text, user);
		System.out.println("User location param.: " + user_param + " - is: " + !user_param.isEmpty());
		text = "<user_work>";
		user_param = User.containsUserSpecificLocation(text, user);
		System.out.println("User location: " + user.userLocation.toString());
		System.out.println("User home: " + user.getTaggedAddress(Address.USER_HOME_TAG, true)); 		//TODO: is this broken?
		System.out.println("User work: " + user.getTaggedAddress(Address.USER_WORK_TAG, true));
		System.out.println("User location param.: " + user_param + " - is: " + !user_param.isEmpty());
		System.out.println("Full adr.: " + LOCATION.getFullAddress(user, user_param, "; "));
		System.out.println("Part adr.: " + LOCATION.getPartAddress(user, user_param, ". ", LOCATION.STREET_NBR, LOCATION.STREET, LOCATION.CITY));
		text = "This is the way from <user_work> to <user_home>.";
		user_param = User.containsUserSpecificLocation(text, user);
		System.out.println("User location param.: " + user_param + " - is: " + !user_param.isEmpty());
		System.out.println("User location repl.: " + user.replaceUserSpecificLocation(text, user_param, LOCATION.CITY));
		System.out.println("User location repl.: " + user.replaceUserSpecificLocation(text, user_param, LOCATION.LAT));
		System.out.println("User location repl.: " + user.replaceUserSpecificLocation(text, user_param, LOCATION.LNG));
		text = "";
		System.out.println("User location repl.: " + user.replaceUserSpecificLocation(text, user_param, LOCATION.LAT));
		
		//test text compare and word inclusions
		text = "this' is a test; text. including special: characters!?!";
		String words = "test done?";
		System.out.println("wordInlusion res.: " + StringCompare.wordInclusion(words, text));
		
		text = "A-GPS";
		words = "Assisted-Global Positioning System";
		System.out.println("is abbreviation: " + NluTools.isAbbreviation(text, words));

	}
	
	// ----------
	
	private static void printHM(HashMap<String, String> hm){
		for (Map.Entry<String, String> entry : hm.entrySet()) {
			System.out.println(entry.getKey() + " = " + entry.getValue());
		}
	}
	private static void printHM2(HashMap<String, Object> hm){
		for (Map.Entry<String, Object> entry : hm.entrySet()) {
			System.out.println(entry.getKey() + " = " + entry.getValue());
		}
	}
	private static void printList(ArrayList<String> list){
		for (String e : list){
			System.out.println("List element: " + e);
		}
	}

}
