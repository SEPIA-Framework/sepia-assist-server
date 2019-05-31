package net.b07z.sepia.server.assist.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.server.ConfigTestServer;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.JSON;

public class Test_WebSearch {
	
	private static List<String> errors;
	
	public static void main(String[] args) {
		errors = new ArrayList<>();
		
		//load default settings (can add Start.loadConfigFile(serverType) before)
		ConfigTestServer.loadAnswersAndParameters();
		String userId = ConfigTestServer.getFakeUserId(null); 	//default test user 1
		
		//Prevent database access for tests
		ConfigTestServer.reduceDatabaseAccess(userId);
		//... add service specific test-settings here
		
		//Tests
		Map<String, JSONObject> tests;
		
		boolean doAnswer = false;	//just interpret (if you want to test a keywordAnalyzer defined INSIDE service) 
		String WS = CMD.WEB_SEARCH;
		String MS = CMD.MUSIC;
		String language;
		
		language = LANGUAGES.EN;
		tests = new HashMap<>();
		tests.put("Search the web for X", 			JSON.make("c", WS, "p", JSON.make(PARAMETERS.WEBSEARCH_REQUEST, "X")));
		tests.put("Search YouTube for X", 			JSON.make("c", WS, "p", JSON.make(PARAMETERS.WEBSEARCH_REQUEST, "X")));
		tests.put("Search Google for X", 			JSON.make("c", WS, "p", JSON.make(PARAMETERS.WEBSEARCH_REQUEST, "X")));
		tests.put("Search music of X on YouTube", 	JSON.make("c", WS, "p", JSON.make(PARAMETERS.WEBSEARCH_REQUEST, "X")));
		tests.put("play music of X on YouTube", 	JSON.make("c", MS, "p", JSON.make(PARAMETERS.MUSIC_ARTIST, "X")));
		tests.put("Search music of X on Spotify", 	JSON.make("c", MS, "p", JSON.make(PARAMETERS.MUSIC_ARTIST, "X")));
				
		for (Map.Entry<String, JSONObject> t : tests.entrySet()){
			String shouldBeCmd = JSON.getString(t.getValue(), "c");
			JSONObject parameterRegExpMatches = JSON.getJObject(t.getValue(), "p");
			ServiceTestTools.testSentenceViaKeywordAnalyzer(errors, doAnswer, t.getKey(), language, shouldBeCmd, parameterRegExpMatches);
		}
		
		language = LANGUAGES.DE;
		tests = new HashMap<>();
		tests.put("Suche im Web nach X", 			JSON.make("c", WS, "p", JSON.make(PARAMETERS.WEBSEARCH_REQUEST, "X")));
		tests.put("Suche auf YouTube nach X", 		JSON.make("c", WS, "p", JSON.make(PARAMETERS.WEBSEARCH_REQUEST, "X")));
		tests.put("Suche auf Google nach X", 		JSON.make("c", WS, "p", JSON.make(PARAMETERS.WEBSEARCH_REQUEST, "X")));
		tests.put("Suche Musik von X auf YouTube", 	JSON.make("c", WS, "p", JSON.make(PARAMETERS.WEBSEARCH_REQUEST, "X")));
		tests.put("Spiele Musik von X auf YouTube", 	JSON.make("c", MS, "p", JSON.make(PARAMETERS.MUSIC_ARTIST, "X")));
		tests.put("Suche Musik von X auf Spotify", 		JSON.make("c", MS, "p", JSON.make(PARAMETERS.MUSIC_ARTIST, "X")));
		
		for (Map.Entry<String, JSONObject> t : tests.entrySet()){
			String shouldBeCmd = JSON.getString(t.getValue(), "c");
			JSONObject parameterRegExpMatches = JSON.getJObject(t.getValue(), "p");
			ServiceTestTools.testSentenceViaKeywordAnalyzer(errors, doAnswer, t.getKey(), language, shouldBeCmd, parameterRegExpMatches);
		}
		
		if (!errors.isEmpty()){
			System.out.println("Errors in: ");
			for (String s : errors){
				System.out.println(s);
			}
		}
		System.out.println("--- DONE ---");
	}

}
