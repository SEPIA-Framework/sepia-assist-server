package net.b07z.sepia.server.assist.interviews;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.answers.DefaultReplies;
import net.b07z.sepia.server.assist.apis.ApiResult;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluInterface;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.ResponseHandler;
import net.b07z.sepia.server.assist.interviews.AbstractInterview;
import net.b07z.sepia.server.assist.interviews.InterviewResult;
import net.b07z.sepia.server.assist.interviews.InterviewInterface;
import net.b07z.sepia.server.assist.parameters.ParameterConfig;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.ConfigTestServer;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

public class Test_Interviews {
	
	//--------------MAIN---------------
	public static void main(String[] args) {
		//check time
		long tic = Debugger.tic();
		
		//setup answers
		Config.toggleAnswerModule(); 	//default is ES so this sets txt-file
		DefaultReplies.setupDefaults(); 			//setup default question mapping for parameters and stuff
		
		//setup commands and parameters
		InterviewServicesMap.load();	//services connected to interviews
		ParameterConfig.setup(); 		//connect parameter names to handlers and other stuff
		
		//test interviews
		testFlightSearch();
		//testWeatherSearch();
		//testShopping();
		
		//check time
		System.out.println("Time needed (ms): " + Debugger.toc(tic));
	}
	
	//----FLIGHTS----
	public static void testFlightSearch(){
		//fake input
		//String text = "Ich suche einen Flug für 2 Personen von Düsseldorf bitte nach New York";
		String text = "I'm looking for a flight for 2 persons from Düsseldorf to New York";
		NluInput input = ConfigTestServer.getFakeInput(text, "en");
		User user1 = ConfigTestServer.getTestUser(ConfigTestServer.email_id1, input, false, true);
		System.out.println("User1: " + user1.userName.toString());
		System.out.println("");
		
		//Interview class
		InterviewInterface assistant = new AbstractInterview(); //new FlightSearch();
		assistant.setCommand(CMD.FLIGHTS);
		
		//Interview info
		JSONObject info = assistant.getInfo("en").getJSON();
		System.out.println("Interview info: ");
		JSON.printJSONpretty(info);
		System.out.println("");
		
		//NLP 1 - initial input
		String NLP_class = Config.keywordAnalyzers.get(input.language);
		NluInterface NLP = (NluInterface) ClassBuilder.construct(NLP_class);
		NluResult nlu_res = NLP.interpret(input);
		//check
		JSON.printJSONpretty(nlu_res.getBestResultJSON());
		System.out.println("");
		
		//Interview 1
		InterviewResult iResult = assistant.getMissingParameters(nlu_res);
		ApiResult result;
		if (iResult.isComplete()){
			result = assistant.getServiceResults(iResult);
		}else{
			result = iResult.apiResult;
		}
		//check
		JSON.printJSONpretty(result.resultJson);
		System.out.println("");
		
		//NLP 2 - answer
		//input.text = "am 12. Dezember";
		input.text = "at december the 12th";
		input.input_miss = result.getMissingInput();
		input.last_cmd = nlu_res.cmdSummary;
		NLP = new ResponseHandler();
		nlu_res = NLP.interpret(input);
		//check
		JSON.printJSONpretty(nlu_res.getBestResultJSON());
		System.out.println("");
		
		//Interview 2
		iResult = assistant.getMissingParameters(nlu_res);
		if (iResult.isComplete()){
			result = assistant.getServiceResults(iResult);
			//check
			System.out.println("TEST FINAL API_RESULT: " + result.getResultJSON());
			JSON.printJSONpretty(result.resultJson);
			System.out.println("");
		}else{
			System.out.println("NO RESULT");
		}
	}
	
	//----WEATHER----
	public static void testWeatherSearch(){
		//fake input
		String text = "How is the weather in New York today";
		NluInput input = ConfigTestServer.getFakeInput(text, "en");
		User user1 = ConfigTestServer.getTestUser(ConfigTestServer.email_id1, input, false, true);
		System.out.println("User1: " + user1.userName.toString());
		System.out.println("");
		
		//Interview class
		InterviewInterface assistant = new AbstractInterview(); //new FlightSearch();
		assistant.setCommand(CMD.WEATHER);
		
		//Interview info
		JSONObject info = assistant.getInfo("en").getJSON();
		System.out.println("Interview info: ");
		JSON.printJSONpretty(info);
		System.out.println("");
		
		//NLP 1 - initial input
		String NLP_class = Config.keywordAnalyzers.get(input.language);
		NluInterface NLP = (NluInterface) ClassBuilder.construct(NLP_class);
		NluResult nlu_res = NLP.interpret(input);
		//check
		JSON.printJSONpretty(nlu_res.getBestResultJSON());
		System.out.println("");
		
		//Interview 1
		InterviewResult iResult = assistant.getMissingParameters(nlu_res);
		ApiResult result;
		if (iResult.isComplete()){
			result = assistant.getServiceResults(iResult);
			//check
			System.out.println("TEST FINAL API_RESULT: " + result.getResultJSON());
			JSON.printJSONpretty(result.resultJson);
			System.out.println("");
		}else{
			System.out.println("NO RESULT");
		}
	}
	
	//----SHOPPING----
	public static void testShopping(){
		//fake input
		String text = "Weiße Adidas Samba Sneakers";
		NluInput input = ConfigTestServer.getFakeInput(text, "de");
		User user1 = ConfigTestServer.getTestUser(ConfigTestServer.email_id1, input, false, true);
		System.out.println("User1: " + user1.userName.toString());
		System.out.println("");
		
		//Interview class
		InterviewInterface assistant = new AbstractInterview(); //new FlightSearch();
		assistant.setCommand(CMD.FASHION);
		
		//Interview info
		JSONObject info = assistant.getInfo("en").getJSON();
		System.out.println("Interview info: ");
		JSON.printJSONpretty(info);
		System.out.println("");
		
		//NLP 1 - initial input
		String NLP_class = Config.keywordAnalyzers.get(input.language);
		NluInterface NLP = (NluInterface) ClassBuilder.construct(NLP_class);
		NluResult nlu_res = NLP.interpret(input);
		nlu_res.setCommand(CMD.FASHION);
		nlu_res.setParameter(PARAMETERS.FASHION_ITEM, "Adidas Samba Sneakers");
		nlu_res.setParameter(PARAMETERS.COLOR, "white");
		//check
		JSON.printJSONpretty(nlu_res.getBestResultJSON());
		System.out.println("");
		
		//Interview 1
		InterviewResult iResult = assistant.getMissingParameters(nlu_res);
		ApiResult result;
		if (iResult.isComplete()){
			result = assistant.getServiceResults(iResult);
			//check
			System.out.println("TEST FINAL API_RESULT: " + result.getResultJSON());
			JSON.printJSONpretty(result.resultJson);
			System.out.println("");
		}else{
			System.out.println("NO RESULT");
		}
	}
}
