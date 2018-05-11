package net.b07z.sepia.server.assist.interpreters;

import java.util.HashMap;
import java.util.Map;

import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interpreters.RegexParameterSearch;
import net.b07z.sepia.server.assist.parameters.DateAndTime;
import net.b07z.sepia.server.assist.server.ConfigTestServer;
import net.b07z.sepia.server.assist.users.User;

public class Test_Parameter_Search {

	public static void main(String[] args) {
		
		//fake input
		NluInput input = ConfigTestServer.getFakeInput("test", "de");
		User user = ConfigTestServer.getTestUser(ConfigTestServer.email_id1, input, false, true);
		input.user_location = "<country>Germany<city>Essen<street>Somestreet 1<code>45138<latitude>51.6<longitude>7.1";
		input.user = user;
		input.user_time_local = "2016.01.01_00:00:01";
		
		//test sentence
		System.out.println("-----SENTENCE TESTING------");
		String text = "ich suche ein italienisches restaurant in der naehe meiner arbeit";		System.out.println(text);	printHM(RegexParameterSearch.get_locations(text, "de"));
		text = "ich suche ein guenstiges hotel in berlin fuer naechste woche";			System.out.println(text);	printHM(RegexParameterSearch.get_locations(text, "de"));
		text = "I'm looking for an italian restaurant in berlin close to my work";		System.out.println(text);	printHM(RegexParameterSearch.get_locations(text, "en"));
		System.out.println("-----------");
		text = "erinnere mich daran in einer stunde die kids abzuholen";		System.out.println(text);	printHM(RegexParameterSearch.get_date(text, "de"));
		text = "remind me to fetch the kids in 1 hour";		System.out.println(text);	printHM(RegexParameterSearch.get_date(text, "en"));
		System.out.println("-----------");
		text = "ich muss auf dem weg von zu hause zu susi noch tanken";		System.out.println(text);	printHM(RegexParameterSearch.get_locations(text, "de"));
		text = "tanken auf dem weg von zu hause zu susi";					System.out.println(text);	printHM(RegexParameterSearch.get_locations(text, "de"));
		text = "i have to get some gas on my way from here to suzi";		System.out.println(text);	printHM(RegexParameterSearch.get_locations(text, "en"));
		text = "on my way to suzi i have to get some gas ";		System.out.println(text);	printHM(RegexParameterSearch.get_locations(text, "en"));
		System.out.println("-----------");
		System.out.println("suzi -> gas? " + NluTools.checkOrder(text, "suzi", "gas"));
		System.out.println("-----------");
		text = "ich suche ein Hotel in Berlin vom 1.4.2016 bis 13.4";		System.out.println(text);	printHM(RegexParameterSearch.get_locations(text, "de"));
		System.out.println("Date: " + DateAndTime.convertTagToDate(RegexParameterSearch.get_locations(text, "de").get("travel_time"), "", input));
		System.out.println("Date end: " + DateAndTime.convertTagToDate(RegexParameterSearch.get_locations(text, "de").get("travel_time_end"), "", input));
		System.out.println("-----------");
		text = "bring mich von A nach B mit zwischenstopp C";		System.out.println(text);	printHM(RegexParameterSearch.get_locations(text, "de"));
		text = "bring mich von A nach B ueber C";		System.out.println(text);	printHM(RegexParameterSearch.get_locations(text, "de"));
		text = "bring me from A to B with stopover C";		System.out.println(text);	printHM(RegexParameterSearch.get_locations(text, "en"));
		System.out.println("-----------");
		text = "ich suche ein hotel in new york vom 7. april bis 12. april";		System.out.println(text);	printHM(RegexParameterSearch.get_locations(text, "de"));
		text = "im looking for hotles in new york from the 7th of april till the 12th of april";		System.out.println(text);	printHM(RegexParameterSearch.get_locations(text, "en"));
		System.out.println("-----------");
		text = "zeig mir die günstigste verbindung nach münchen am 7. mai";		System.out.println(text);	printHM(RegexParameterSearch.get_locations(text, "de"));
		System.out.println("-----------");
		text = "timer für eine minuten und 70 sekunden";		System.out.println(text);	printHM(RegexParameterSearch.get_date(text, "de"));
		text = "timer for  20 minutes and 30 seconds";		System.out.println(text);	printHM(RegexParameterSearch.get_date(text, "en"));
		System.out.println("-----------");
	}
	
	// ----------
	
	private static void printHM(HashMap<String, String> hm){
		for (Map.Entry<String, String> entry : hm.entrySet()) {
			System.out.println(entry.getKey() + " = " + entry.getValue());
		}
	}

}
