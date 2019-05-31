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

public class Test_ClientControls {
	
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
		String CC = CMD.CLIENT_CONTROLS;
		String M = CMD.MUSIC;
		String language;
		
		language = LANGUAGES.EN;
		tests = new HashMap<>();
		tests.put("Set the volume to 11", 		JSON.make("c", CC, "p", JSON.make(PARAMETERS.MEDIA_CONTROLS, "<volume_set>.*")));
		tests.put("Set the volume to eleven", 	JSON.make("c", CC));
		tests.put("Play the next song", 		JSON.make("c", CC, "p", JSON.make(PARAMETERS.MEDIA_CONTROLS, "<next>.*")));
		tests.put("Next", 						JSON.make("c", CC, "p", JSON.make(PARAMETERS.CLIENT_FUN, "<media>.*")));
		tests.put("Back", 						JSON.make("c", CC, "p", JSON.make(PARAMETERS.CLIENT_FUN, "<media>.*")));
		tests.put("Stop music", 				JSON.make("c", CC, "p", JSON.make(PARAMETERS.MEDIA_CONTROLS, "<stop>.*")));
		tests.put("Play music", 						JSON.make("c", M));
		tests.put("Play stop and go by Fake Artist", 	JSON.make("c", M));
		tests.put("What is Lahmacun", 				JSON.make("c", CMD.KNOWLEDGEBASE));
		tests.put("What is the best way to work", 	JSON.make("c", CMD.DIRECTIONS));
		
		for (Map.Entry<String, JSONObject> t : tests.entrySet()){
			String shouldBeCmd = JSON.getString(t.getValue(), "c");
			JSONObject parameterRegExpMatches = JSON.getJObject(t.getValue(), "p");
			ServiceTestTools.testSentenceViaKeywordAnalyzer(errors, doAnswer, t.getKey(), language, shouldBeCmd, parameterRegExpMatches);
		}
		
		language = LANGUAGES.DE;
		tests = new HashMap<>();
		tests.put("Setze die Lautstärke auf 11", 	JSON.make("c", CC, "p", JSON.make(PARAMETERS.MEDIA_CONTROLS, "<volume_set>.*")));
		tests.put("Lautstärke auf elf", 			JSON.make("c", CC));
		tests.put("Starte den nächsten Song", 		JSON.make("c", CC, "p", JSON.make(PARAMETERS.MEDIA_CONTROLS, "<next>.*")));
		tests.put("Weiter", 						JSON.make("c", CC, "p", JSON.make(PARAMETERS.CLIENT_FUN, "<media>.*")));
		tests.put("Zurück", 						JSON.make("c", CC, "p", JSON.make(PARAMETERS.CLIENT_FUN, "<media>.*")));
		tests.put("Musik anhalten", 				JSON.make("c", CC, "p", JSON.make(PARAMETERS.MEDIA_CONTROLS, "<pause>.*")));
		tests.put("Musik spielen", 								JSON.make("c", M));
		tests.put("Spiele Stop und Weiter von Fake Künstler", 	JSON.make("c", M));
		tests.put("Was ist Lahmacun", 							JSON.make("c", CMD.KNOWLEDGEBASE));
		tests.put("Was ist der beste Weg zur Arbeit", 	JSON.make("c", CMD.DIRECTIONS));
		
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
