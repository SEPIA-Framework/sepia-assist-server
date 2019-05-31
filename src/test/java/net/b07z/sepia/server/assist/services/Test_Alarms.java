package net.b07z.sepia.server.assist.services;

import java.util.List;

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
import net.b07z.sepia.server.assist.tools.DateTimeConverters;
import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.JSONWriter;

public class Test_Alarms {

	public static void main(String[] args) {
		
		//load default settings (can add Start.loadConfigFile(serverType) before)
		ConfigTestServer.loadAnswersAndParameters();
		
		//load "fake" input with some defaults
		String language = LANGUAGES.EN;
		String text = "Set an Alarm for 8 a.m."; //"Set an Alarm for tomorrow 8 a.m.";
		NluInput input = ConfigTestServer.getFakeInput(text, language);
		
		//set some values explicitly
		input.user.userName = new Name("Mister", "Tester", "Testy");
		input.setTimeGMT("2019.01.14_13:00:00");
		
		//Prevent database access for tests
		Alarms.testMode = true;
		ConfigTestServer.reduceDatabaseAccess(input.user.getUserID());
		
		//time
		System.out.println("\nChosen UNIX time (s): " + Math.round(input.userTime/1000));
		System.out.println("Now (for testing): " + DateTimeConverters.getSpeakableDateSpecial(input.userTimeLocal, 5l, Config.defaultSdf, input) 
									+ ", " + input.userTimeLocal + "\n");
		
		//start with test:
		System.out.println("Sentence: " + input.text);
		
		//create NluResult (shortcut)
		NluInterface nlp = (NluInterface) ClassBuilder.construct(Config.keywordAnalyzers.get(input.language));
		NluResult nluResult = nlp.interpret(input);
		System.out.println("Best NLU result:");
		System.out.println(JSONWriter.getPrettyString(nluResult.getBestResultJSON()));
		
		//get answer
		ServiceResult answer;
		String cmd = nluResult.getCommand();
		
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
		System.out.println(JSONWriter.getPrettyString(answer.getResultJSONObject()));
	}

}
