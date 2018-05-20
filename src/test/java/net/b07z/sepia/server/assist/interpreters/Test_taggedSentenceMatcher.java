package net.b07z.sepia.server.assist.interpreters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.interviews.AbstractInterview;
import net.b07z.sepia.server.assist.interviews.InterviewResult;
import net.b07z.sepia.server.assist.interviews.InterviewServicesMap;
import net.b07z.sepia.server.assist.interviews.InterviewInterface;
import net.b07z.sepia.server.assist.server.ConfigServices;
import net.b07z.sepia.server.assist.server.ConfigTestServer;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.tools.DateTime;
import net.b07z.sepia.server.core.tools.JSON;

public class Test_taggedSentenceMatcher {

	public static void main(String[] args) {
		
		//database
		HashMap<String, TreeMap<String, String>> sentences = new HashMap<>();
		TreeMap<String, String> poolDE = new TreeMap<>(Collections.reverseOrder());
			poolDE.put("wie ist das wetter in <place>", "weather;;place=<place>;;");
			poolDE.put("wetter in <place>", "weather;;place=<place>;;");
			poolDE.put("was ist ein synonym fuer <word>", "tatoeba");
		sentences.put("de", poolDE);
		TreeMap<String, String> poolEN = new TreeMap<>(Collections.reverseOrder());
			poolEN.put("how is the weather in <place>", "weather;;place=<place>;;");
			poolEN.put("weather in <place>", "weather;;place=<place>;;");
		sentences.put("en", poolEN);
	
		NluInterface NLP = new NluTaggedSentenceMatcher(sentences);			//interpreters implement the NLU_interface
		
		String text = "wie ist das wetter in München";
		text = "was ist ein synonym fuer Kabeljau";
		System.out.println("INPUT: " + text);
		
		//parameters
		String language = "de";
		String context = "default";
		int mood = -1;
		String environment = "web_app";
		
		//get the result of the natural-language-processor (i will usually call it either NLP or NLU and mix at will ;-) )
		NluInput input = new NluInput(text, language, context, mood, environment);
		input.userLocation = JSON.make("city", "Berlin City", "latitude", 52.519, "longitude", 13.405).toString();
		input.userTime = System.currentTimeMillis();
		input.userTimeLocal = DateTime.getFormattedDate("yyyy.MM.dd_HH:mm:ss");
		//System.out.println("Today: " + Tools_DateTime.getToday("HH:mm:ss yyyy.MM.dd", input));
		User user = ConfigTestServer.getTestUser(ConfigTestServer.email_id1, input, false, true);
		input.user = user;
		NluResult result = NLP.interpret(input);
		
		//show all possible results
		System.out.println("CMDs identified by NL-Proc: " + result.showAllPossibleCMDs());
		System.out.println("CMD selected: " + result.getCommand());
		
		//internally results are usually passed around as NLU_results, but clients can ask for a JSON string via HTTP GET
		//JSON example as seen by clients:
		JSONObject bestResult = result.getBestResultJSON();				//convert best interpreter result to JSON
		System.out.println(bestResult.toJSONString());						//full result as JSON string
		if (((String)bestResult.get("result")).matches("success")){
			System.out.println("BEST CMD: " + bestResult.get("command"));		//JSON version of result.get_command()
			JSONObject params = (JSONObject) bestResult.get("parameters");		//all parameters
			System.out.println("PARAMETERS: " + params.toJSONString());			//JSON version of result.get_parameter("xy")
		}
		
		System.out.println("---command: " + result.getCommand());
		System.out.println("---parameters:");
		for (Map.Entry<String, String> entry : result.parameters.entrySet()) {
			System.out.println("----" + entry.getKey() + " = " + entry.getValue());
		}
		
		//interview module with services
		Start.setupModules();
		Start.setupServicesAndParameters();
		//add a dev. service
		Class<?> devServiceClazz = null;
		String devServiceCMD = "tatoeba";
		ArrayList<String> devService = new ArrayList<String>();
			devService.add(devServiceClazz.getCanonicalName());
			InterviewServicesMap.get().put(devServiceCMD, devService);
		//ConfigServices.loadInterviewServicesMap();
		//ParameterConfig.setup();
		List<ServiceInterface> services = ConfigServices.getCustomOrSystemServices(input, user, result.getCommand());
		if (!services.isEmpty()){
			InterviewInterface interview = new AbstractInterview();
			interview.setCommand(result.getCommand());
			interview.setServices(services);
			InterviewResult iResult = interview.getMissingParameters(result); 		//<- overwrites old parameters
			String res ="";
			if (iResult.isComplete()){
				///*
				res = interview.getServiceResults(iResult).getResultJSON();
				System.out.println("Interview result: " + res);
				//*/
			}else{
				res = iResult.getApiComment().getResultJSON();
				System.out.println("Interview result: " + res);
			}
			System.out.println("After interview: ");
			System.out.println("---parameters:");
			for (Map.Entry<String, String> entry : result.parameters.entrySet()) {
				System.out.println("----" + entry.getKey() + " = " + entry.getValue());
			}
		}

	}

}
