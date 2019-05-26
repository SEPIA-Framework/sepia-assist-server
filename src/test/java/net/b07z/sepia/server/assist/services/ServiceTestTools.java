package net.b07z.sepia.server.assist.services;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;

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
import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.JSONWriter;

public class ServiceTestTools {
	
	
	public static void testSentenceViaKeywordAnalyzer(List<String> errors, boolean doAnswer, String text, String language, String shouldBeCMD, JSONObject parameterRegExpMatches){
		if (errors == null) errors = new ArrayList<>();
		
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
