package net.b07z.sepia.server.assist.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.assistant.LOCATION;
import net.b07z.sepia.server.assist.interviews.InterviewServicesMap;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.ConfigTestServer;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.JSON;

public class Test_Weather {

	private static List<String> errors;
	
	public static void main(String[] args){
		errors = new ArrayList<>();
		
		//load test config (for openHAB server URL mainly)
		Start.loadSettings(new String[]{"--test"});
		System.out.println("\nIntegration Test: Weather");
		
		//load default settings
		ConfigTestServer.loadAnswersAndParameters();
		String userId = ConfigTestServer.getFakeUserId(null); 	//default test user 1
		
		System.out.println("Class: " + InterviewServicesMap.get().get(CMD.WEATHER).get(0) + "\n");
		
		//Prevent database access for tests
		ConfigTestServer.reduceDatabaseAccess(userId);
		Config.enableSDK = false;		//we can't test the SDK services here, we need the DB for it
		//... add service specific test-settings here
				
		//API Tests
		Map<String, JSONObject> tests;
		
		boolean doAnswer = true;	//just interpret (if you want to test a keywordAnalyzer defined INSIDE service) 
		String WEA = CMD.WEATHER;
		String language;
		JSONObject location = LOCATION.makeLocation("Deutschland", "NRW", "Essen", null, null, null, "51.45", "7.01");
		
		language = LANGUAGES.DE;
		tests = new HashMap<>();
		tests.put("Wie ist das Wetter heute?",		JSON.make("c", WEA, "p", JSON.make(PARAMETERS.REPLY, "")));
		//tests.put("Wie ist das Wetter morgen?",		JSON.make("c", WEA, "p", JSON.make(PARAMETERS.REPLY, "")));
		//tests.put("Wie ist das Wetter übermorgen?",		JSON.make("c", WEA, "p", JSON.make(PARAMETERS.REPLY, "")));
		//tests.put("Wie ist das Wetter am Dienstag?",		JSON.make("c", WEA, "p", JSON.make(PARAMETERS.REPLY, "")));
		//tests.put("Wie ist das Wetter am Donnerstag?",		JSON.make("c", WEA, "p", JSON.make(PARAMETERS.REPLY, "")));
				
		for (Map.Entry<String, JSONObject> t : tests.entrySet()){
			String shouldBeCmd = JSON.getString(t.getValue(), "c");
			JSONObject parameterRegExpMatches = JSON.getJObject(t.getValue(), "p");
			ServiceTestTools.testSentenceViaKeywordAnalyzer(errors, doAnswer, t.getKey(), language, 
					shouldBeCmd, parameterRegExpMatches,
					null, location.toJSONString()
			);
		}
		
		//Icons and description
		/*
		System.out.println("\n---- IconId and description tests ----\n");
		Arrays.asList(
			"clearsky_day", "clearsky_night", "clearsky_polartwilight", "clearsky", "clearsky_else",
			"partlycloudy_day", "partlycloudy_night",
			"fair_day", "fair_night",
			"lightrainshowers",
			"lightrain",
			"heavyrainandthunder",
			"heavysnowshowersandthunder",
			"sleetandthunder"
		).forEach(iconTag -> {
			testIconAndDescription(iconTag);
		});
		*/
	}
	
	private static void testIconAndDescription(String iconTag){
		System.out.println("---- IconId: " + iconTag);
		Arrays.asList(LANGUAGES.DE, LANGUAGES.EN).forEach(lang -> {
			Arrays.asList("now", "future").forEach(predictTime -> {
				JSONObject data = WeatherMeteoNorway.getIconWithDescription(iconTag, lang, predictTime);
				System.out.println("Icon and desc.: " + data.toJSONString());
			});
		});
	}

}
