package net.b07z.sepia.server.assist.parameters;

import java.util.Arrays;
import java.util.HashMap;

import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.RegexParameterSearch;
import net.b07z.sepia.server.assist.parameters.DateAndTime;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.ConfigTestServer;
import net.b07z.sepia.server.assist.tools.DateTimeConverters;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.tools.JSON;

public class Test_DateAndTime {

	public static void main(String[] args) {
		
		//fake input
		NluInput input = ConfigTestServer.getFakeInput("test", "de");
		input.userTimeLocal = "2018.01.14_10:00:00";
		input.userTime = DateTimeConverters.getUnixTimeOfDate(input.userTimeLocal, "yyyy.MM.hh_HH:mm:ss"); 
		User user = ConfigTestServer.getTestUser(ConfigTestServer.email_id1, input, false, true);
		input.userLocation = JSON.make("country", "Germany", "city", "Essen", "street", "Somestreet 1", "latitude", 51.6, "longitude", 7.1).toString();
		input.user = user;
		
		//time
		System.out.println("chosen UNIX time (s): " + Math.round(input.userTime/1000));
		System.out.println("now: " + DateTimeConverters.getSpeakableDateSpecial(input.userTimeLocal, 5l, Config.defaultSdf, input) 
									+ ", " + input.userTimeLocal + "\n");
		
		//warm up
		System.out.println("-----SENTENCE TESTING------");
		
		System.out.println("\nNOTE: Text MUST BE normalized already for this test!\n");
		
		input.language = "de";
		System.out.println("\n----- de -----");
		testTimeString("8", 									"[2018.01.14, 08:00:00]", input); 		//this is debatable
		testTimeString("erstelle einen timer fuer 8", 			"[, ]", input);
		testTimeString("erstelle einen timer fuer 8:30", 		"[2018.01.14, 08:30:00]", input);		
		testTimeString("erstelle einen timer fuer 20 minuten", 	"[2018.01.14, 10:20:00]", input);
		testTimeString("erstelle einen timer fuer fuenfzehn minuten", 	"[2018.01.14, 10:15:00]", input);
		testTimeString("weck mich in 20 minuten", 						"[2018.01.14, 10:20:00]", input);
		testTimeString("timer für 30s", 						"[2018.01.14, 10:00:30]", input);		
		
		input.language = "en";
		System.out.println("\n----- en -----");
		testTimeString("set a timer for 20 minutes", 	"[2018.01.14, 10:20:00]", input);
		testTimeString("wake me up in 20 minutes", 		"[2018.01.14, 10:20:00]", input);
		testTimeString("timer for 30 s", 				"[2018.01.14, 10:00:30]", input);
				
		System.out.println("-----------");
		
		input.language = "en";
		System.out.println("\n----- en -----");
		testTimeString("12 oclock and 30 minutes", 					"[2018.01.14, 12:30:00]", input);
		testTimeString("appointment for monday 11 p.m.", 			"[2018.01.15, 23:00:00]", input);
		testTimeString("appointment for monday 10:30 oclock", 		"[2018.01.15, 10:30:00]", input);
		testTimeString("appointment for now 20:30oclock", 			"[2018.01.14, 20:30:00]", input);
		testTimeString("set an alarm for tomorrow morning 9 a.m.", 	"[2018.01.15, 09:00:00]", input);
		testTimeString("set an alarm for tomorrow morning", 		"[2018.01.15, 08:00:00]", input);
		testTimeString("set an alarm for tuesday", 					"[2018.01.16, ]", input);
		testTimeString("set an alarm for 8:30", 					"[2018.01.14, 08:30:00]", input);
		testTimeString("set an alarm for 8:30 on wednesday",		"[2018.01.17, 08:30:00]", input);
		testTimeString("set an alarm for 8:30 in 3 days",			"[2018.01.17, 08:30:00]", input);
		testTimeString("set an alarm for 10 pm in 3 days",			"[2018.01.17, 22:00:00]", input);
		testTimeString("set an alarm in 4 days for 8:30 pm",		"[2018.01.18, 20:30:00]", input);
		testTimeString("appointment for the 12th at 7 a.m.",		"[2018.01.12, 07:00:00]", input);
		testTimeString("remind me in 70 days of the game",			"[2018.03.25, 11:00:00]", input);
		testTimeString("set an alarm for tomorrow 6pm", 	"[2018.01.15, 18:00:00]", input);
		testTimeString("set an alarm for tomorrow 6 p.m.", 	"[2018.01.15, 18:00:00]", input);
		testTimeString("set an alarm for 8 a.m.", 			"[2018.01.14, 08:00:00]", input);
				
		input.language = "de";
		System.out.println("\n----- de -----");
		testTimeString("alarm stellen fuer morgen abend 8:30uhr", 		"[2018.01.15, 20:30:00]", input);
		testTimeString("alarm stellen fuer mittwoch 8:30 uhr abends", 	"[2018.01.17, 20:30:00]", input);
		testTimeString("alarm stellen fuer 8:30 uhr abends am donnerstag", 	"[2018.01.18, 20:30:00]", input);
		testTimeString("alarm stellen fuer 8:30 uhr abends in 2 tagen", 	"[2018.01.16, 20:30:00]", input);
		testTimeString("alarm stellen fuer 8:30 in 3 tagen",				"[2018.01.17, 08:30:00]", input);
		testTimeString("alarm stellen fuer 8:30 am mittwoch",				"[2018.01.17, 08:30:00]", input);
		testTimeString("alarm stellen fuer 20:30 uhr in 2 tagen", 			"[2018.01.16, 20:30:00]", input);
		testTimeString("alarm stellen in 2 tagen fuer 20:30 uhr abends", 	"[2018.01.16, 20:30:00]", input);
		testTimeString("alarm stellen fuer abends", 						"[2018.01.14, 19:30:00]", input);
		testTimeString("reise am 06.06.2017 um 9:20 uhr", 				"[2017.06.06, 09:20:00]", input);
		testTimeString("reise am 12.12.2017", 							"[2017.12.12, ]", input);
		testTimeString("reise am 30.06.2017 morgens", 					"[2017.06.30, 08:00:00]", input);
		testTimeString("was geht naechste woche", 						"[, ]", input);
		testTimeString("wieviel uhr ist es jetzt", 						"[2018.01.14, 10:00:00]", input);
		testTimeString("termin fuer den 12. um 7 uhr", 					"[2018.01.12, 07:00:00]", input);
		testTimeString("was laeuft heute abend im tv", 					"[2018.01.14, 19:30:00]", input);
		testTimeString("weck mich um 8", 								"[2018.01.14, 08:00:00]", input);
		testTimeString("wetter morgen", 								"[2018.01.15, ]", input);
		testTimeString("erinnere mich in 70 tagen an das Spiel", 		"[2018.03.25, 11:00:00]", input);
		testTimeString("erinnere mich am montag morgen an das meeting", "[2018.01.15, 08:00:00]", input);
		testTimeString("erinnere mich am montag abend an das meeting", 	"[2018.01.15, 19:30:00]", input);
		testTimeString("erinnere mich am 3.5 an das meeting", 			"[2018.05.03, ]", input);
		testTimeString("erinnere mich in einer woche um 11 uhr an das meeting", "[2018.01.21, 11:00:00]", input);
		testTimeString("erinnere mich in einer woche um 13 uhr an reifen", 		"[2018.01.21, 13:00:00]", input);
		testTimeString("erinnere mich samstag um 10 Uhr an das fest", 			"[2018.01.20, 10:00:00]", input);
		testTimeString("erinnere mich naechste woche samstag um 13 uhr an zeug", 	"[2018.01.20, 13:00:00]", input);
	}

	public static boolean testTimeString(String text, String shouldBe, NluInput input){
		System.out.println("\n" + text);
		HashMap<String, String> dateMap = RegexParameterSearch.get_date(text, input.language);	
		System.out.println("dateEx: " + dateMap.values());
		String res = Arrays.toString(DateAndTime.convertTagToDate(dateMap, input));
		if (res.equals(shouldBe)){
			System.out.println("converted_date: " + res);
			return true;
		}else{
			try{ Thread.sleep(20); }catch(Exception e){}
			System.err.println("converted_date: " + res + " - should be: " + shouldBe);
			try{ Thread.sleep(20); }catch(Exception e){}
			return false;
		}
	}
}
