package net.b07z.sepia.server.assist.parameters;

import java.util.Arrays;
import java.util.HashMap;

import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.RegexParameterSearch;
import net.b07z.sepia.server.assist.parameters.DateAndTime;
import net.b07z.sepia.server.assist.server.ConfigTestServer;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.tools.JSON;

public class Test_DateAndTime {

	public static void main(String[] args) {
		
		//time
		System.out.println("system time: " + Math.round(System.currentTimeMillis()/1000));
		
		//fake input
		NluInput input = ConfigTestServer.getFakeInput("test", "de");
		User user = ConfigTestServer.getTestUser(ConfigTestServer.email_id1, input, false, true);
		input.userLocation = JSON.make("country", "Germany", "city", "Essen", "street", "Somestreet 1", "latitude", 51.6, "longitude", 7.1).toString();
		input.user = user;
		
		//use
		String text = "";
		HashMap<String, String> dateMap;
		
		System.out.println("now: " + input.userTimeLocal + "\n");
		
		//warm up
		System.out.println("-----SENTENCE TESTING------");
		input.language = "de";
		text = "8";	System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "erstelle einen timer fuer 8";	System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "erstelle einen timer fuer 8:30";	System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "erstelle einen timer fuer 20 minuten";	System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "weck mich in 20 minuten";			System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "timer für 30s";						System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		input.language = "en";
		text = "set a timer for 20 minutes";		System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "wake me up in 20 minutes";			System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "timer for 30 s";					System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		
		System.out.println("-----------");
		input.language = "en";
		text = "12 oclock and 30 minutes";			System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "appointment for monday 11 p.m.";	System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "appointment for monday 10:30 oclock";			System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "appointment for now 20:30oclock";	System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "set an alarm for tomorrow morning 8 a.m.";		System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "set an alarm for tomorrow morning";	System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "set an alarm for tuesday";			System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "set an alarm for 8:30";				System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "appointment for the 12th at 7 a.m.";				System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "remind me in 70 days of the game";		System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		input.language = "de";
		text = "alarm stellen für morgen abend 8:30uhr";		System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "alarm stellen für mittwoch 8:30 uhr abends";	System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "alarm stellen für abends";			System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "reise am 06.06.2017 um 9:20 uhr";	System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "reise am 12.12.2017";				System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "reise am 30.06.2017 morgens";		System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "was geht naechste woche";		System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "wieviel uhr ist es jetzt";		System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "termin für den 12. um 7 uhr";		System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "was laeuft heute abend im tv";		System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "weck mich um 8";		System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "wetter morgen";		System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "erinnere mich in 70 tagen an das Spiel";		System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "erinnere mich am montag morgen an das meeting";		System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
		text = "erinnere mich am montag abend an das meeting";		System.out.println("\n" + text);	dateMap = RegexParameterSearch.get_date(text, input.language);	System.out.println("dateEx: " + dateMap.values());
		System.out.println("converted_date: " + Arrays.toString(DateAndTime.convertTagToDate(dateMap, input)));
	}

}
