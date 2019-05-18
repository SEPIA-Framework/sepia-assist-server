package net.b07z.sepia.server.assist.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Name;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluInterface;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.AbstractInterview;
import net.b07z.sepia.server.assist.interviews.InterviewInterface;
import net.b07z.sepia.server.assist.interviews.InterviewResult;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.ConfigServices;
import net.b07z.sepia.server.assist.server.ConfigTestServer;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.JSONWriter;

public class Test_ClientControls {
	
	private static List<String> errors = new ArrayList<>();

	public static void main(String[] args) {
		
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
			testSentenceViaKeywordAnalyzer(doAnswer, t.getKey(), language, shouldBeCmd, parameterRegExpMatches);
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
			testSentenceViaKeywordAnalyzer(doAnswer, t.getKey(), language, shouldBeCmd, parameterRegExpMatches);
		}
		
		if (!errors.isEmpty()){
			System.out.println("Errors in: ");
			for (String s : errors){
				System.out.println(s);
			}
		}
		System.out.println("--- DONE ---");
	}
	
	private static void testSentenceViaKeywordAnalyzer(boolean doAnswer, String text, String language, String shouldBeCMD, JSONObject parameterRegExpMatches){
		//load "fake" input with some defaults
		NluInput input = ConfigTestServer.getFakeInput(text, language);		//default test user 1
		
		//set some values explicitly
		input.user.userName = new Name("Mister", "Tester", "Testy");
		input.setTimeGMT("2019.01.14_13:00:00");
		
		//time
		//System.out.println("\nChosen UNIX time (s): " + Math.round(input.userTime/1000));
		//System.out.println("Now (for testing): " + DateTimeConverters.getSpeakableDateSpecial(input.userTimeLocal, 5l, Config.defaultSdf, input) + ", " + input.userTimeLocal + "\n");
		
		//start with test:
		try{ Thread.sleep(20); }catch(Exception e){}
		System.out.println("Sentence: " + input.text);
		try{ Thread.sleep(20); }catch(Exception e){}
		
		//create NluResult (shortcut)
		NluInterface nlp = (NluInterface) ClassBuilder.construct(Config.keywordAnalyzers.get(input.language)); 	//NOTE: hard-coded NLU Interface for this test
		NluResult nluResult = nlp.interpret(input);
		
		String cmd = nluResult.getCommand();
		JSONObject resJson = nluResult.getBestResultJSON();
		
		if (cmd.equals(shouldBeCMD)){
			boolean allParamsMatch = true;
			if (parameterRegExpMatches != null){
				JSONObject params = JSON.getJObject(resJson, "parameters");
				for (Object o : parameterRegExpMatches.keySet()){
					String pToCheck = (String) o;
					String actualValue = JSON.getString(params, pToCheck);
					String shouldMatch = JSON.getString(parameterRegExpMatches, pToCheck);
					if (!actualValue.matches(shouldMatch)){
						allParamsMatch = false;
						System.err.println("Mismatch! Expected " + pToCheck + "=" + shouldMatch + " - found: " + actualValue);
						break;
					}
				}
			}
			if (allParamsMatch){
				System.out.println("Best NLU result:");
				System.out.println(JSONWriter.getPrettyString(resJson));
			}else{
				System.err.println("Best NLU result:");
				System.err.println(JSONWriter.getPrettyString(resJson));
				errors.add(text);
			}
		}else{
			System.err.println("Best NLU result:");
			System.err.println(JSONWriter.getPrettyString(resJson));
			errors.add(text);
		}
		
		//get answer
		if (doAnswer){
			ServiceResult answer;
			List<ServiceInterface> services = ConfigServices.getCustomOrSystemServices(input, input.user, cmd);
			InterviewInterface interview = new AbstractInterview();
			interview.setCommand(cmd);
			interview.setServices(services);
			InterviewResult iResult = interview.getMissingParameters(nluResult);
			if (iResult.isComplete()){
				answer = interview.getServiceResults(iResult);
			}else{
				answer = iResult.getApiComment();
			}		
			try{ Thread.sleep(20); }catch(Exception e){}
			System.out.println(JSONWriter.getPrettyString(answer.getResultJSONObject()));
			System.out.println("");
		}else{
			System.out.println("");
		}
		try{ Thread.sleep(20); }catch(Exception e){}
	}

}
