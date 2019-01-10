package net.b07z.sepia.server.assist.services;

import java.util.ArrayList;
import java.util.List;

import net.b07z.sepia.server.assist.assistant.Assistant;
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
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.assist.tools.DateTimeConverters;
import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.JSONWriter;

public class Test_Alarms {

	public static void main(String[] args) {
		
		//load statics
		Config.setupAnswers();
		Start.setupServicesAndParameters();
		Assistant.customCommandsMap = new ArrayList<>(); 	//prevents reload from DB?
		
		//fake input
		String text = "Set an Alarm for 8 a.m."; //"Set an Alarm for tomorrow 8 a.m.";
		NluInput input = ConfigTestServer.getFakeInput(text, LANGUAGES.EN);
		input.user = ConfigTestServer.getTestUser(ConfigTestServer.email_id1, input, true, false);
		input.user.userName = new Name("Mister", "Tester", "Testy");
		input.setCustomCommandToServicesMappings(new ArrayList<>());
		input.userTimeLocal = "2019.01.14_13:00:00";
		input.userTime = DateTimeConverters.getUnixTimeOfDate(input.userTimeLocal, "yyyy.MM.hh_HH:mm:ss");
		
		//time
		System.out.println("\nChosen UNIX time (s): " + Math.round(input.userTime/1000));
		System.out.println("Now (for testing): " + DateTimeConverters.getSpeakableDateSpecial(input.userTimeLocal, 5l, Config.defaultSdf, input) 
									+ ", " + input.userTimeLocal + "\n");
		
		System.out.println("Sentence: " + input.text);
		
		//create NluResult (shortcut)
		NluInterface nlp = (NluInterface) ClassBuilder.construct(Config.keywordAnalyzers.get(input.language));
		NluResult nluResult = nlp.interpret(input);
		
		System.out.println("Best NLU result:");
		System.out.println(JSONWriter.getPrettyString(nluResult.getBestResultJSON()));
		
		//get answer
		Alarms.testMode = true; 	//prevent database access
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
