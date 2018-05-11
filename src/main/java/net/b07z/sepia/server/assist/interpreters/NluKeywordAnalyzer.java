package net.b07z.sepia.server.assist.interpreters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import net.b07z.sepia.server.assist.apis.ApiInfo;
import net.b07z.sepia.server.assist.apis.ApiInterface;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.parameters.AbstractParameterSearch;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.ConfigServices;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.assistant.PARAMETERS;

/**
 * Simple (at least from the idea) yet effective keyword analyzer to interpret user input.<br>
 * This is the language independent version.<br>
 * The order of checks does matter, put commands with less priority at the bottom. If commands score the same probability the first is taken.<br>
 * Note: Never use this as a static interpreter! Always create new instances of it when needed (compared to the sentence matchers
 * that can be used globally). 
 *   
 * @author Florian Quirin
 *
 */
public class NluKeywordAnalyzer implements NluInterface {

	private double certainty_lvl = 0.0d;		//how certain is ILA about a result
	
	/**
	 * This is an abstract regular expression analyzer that can be used to evaluate a custom service that has a defined regular expression trigger.
	 * @param text - the text to search
	 * @param input - NLU_Input
	 * @param service - a service (custom or system) that has all the necessary apiInfo
	 * @param possibleCMDs - required list of the parent NLU method that collects possible commands
	 * @param possibleScore - required list that stores the scores of the possible commands
	 * @param possibleParameters - required list that stores the parameter-value pairs of the possible commands
	 * @param index - integer indicating the current position in the possibleCMDs list
	 * @return new index position in possibleCMDs, will be unchanged to input if custom service regEx is not found in 'text'
	 */
	public static int abstractRegExAnalyzer(String text, NluInput input, ApiInterface service,
						List<String> possibleCMDs, List<Integer> possibleScore, List<Map<String, String>> possibleParameters,
						int index){
		//get service info
		ApiInfo serviceInfo = service.getInfo(input.language);
		String regEx = serviceInfo.getCustomTriggerRegX(input.language);
		
		//has regEx at all?
		if (regEx == null || regEx.isEmpty()){
			return index;
		}
		
		//check the trigger
		if (NluTools.stringContains(text, regEx)){
			
			possibleCMDs.add(serviceInfo.intendedCommand);
			possibleScore.add(1 + serviceInfo.getCustomTriggerRegXscoreBoost());	index++;
			
			//get all parameters
			List<Parameter> paramsList = serviceInfo.getAllParameters();
			Map<String, String> pv = new HashMap<>(); 		//TODO: pass this down to avoid additional checking?
			if (!paramsList.isEmpty()){
				String[] params = new String[paramsList.size()];
				int i = 0;
				for (Parameter p : paramsList){
					params[i] = p.getName();
					//System.out.println("RegX Analyzer, param: " + params[i]); 		//DEBUG
					i++;
				}
				AbstractParameterSearch aps = new AbstractParameterSearch()
						.setParameters(params)
						.setup(input, pv);
				aps.getParameters();
				possibleScore.set(index, possibleScore.get(index) + aps.getScore());
			}
			possibleParameters.add(pv);
		}
		return index;
	}
	
	public NluResult interpret(NluInput input) {
		
		//get parameters from input
		String text = input.text;
		String language = input.language;
		/*
		context = input.context;
		environment = input.environment;
		mood = input.mood;
		*/
		
		//normalize text, e.g.:
		// all lowerCase - remove all ',!? - handle ä ö ü ß ... trim
		Normalizer normalizer = Config.inputNormalizers.get(language);
		if (normalizer != null){
			text = normalizer.normalize_text(text);
		}
		
		//first rough check for main keywords
		List<String> possibleCMDs = new ArrayList<>();			//make a list of possible interpretations of the text
		List<Map<String, String>> possibleParameters = new ArrayList<>();		//possible parameters
		List<Integer> possibleScore = new ArrayList<>();		//make scores to decide which one is correct command
		int index = -1;
		
		
		//EXAMPLE: Timer (german)
		/*
		if (NLU_Tools.stringContains(text, "timer|counter|countdown|zeitnehmer|zeitgeber|zeitmesser")){
			possibleCMDs.add(CMD.TIMER);
			possibleScore.add(1);	index++;

			HashMap<String, String> pv = new HashMap<String, String>();
			AbstractParameterSearch aps = new AbstractParameterSearch()
					.setParameters(PARAMETERS.TIME)
					.setup(input, pv);
			aps.getParameters();
			possibleScore.set(index, possibleScore.get(index) + aps.getScore());
			possibleParameters.add(pv);
		}
		*/
		
		//Abstract analyzer (should come at the end because of lower priority?)
		List<ApiInterface> services = ConfigServices.getCustomServicesList(input, input.user);
		for (ApiInterface service : services){
		index = abstractRegExAnalyzer(text, input, service,
				possibleCMDs, possibleScore, possibleParameters, index);
		}
						
		//Repeat me - overwrites all other commands!
		if (NluTools.stringContains(text, "(^saythis|" + Pattern.quote(Config.assistantName) + " saythis)")){
			String this_text = input.text_raw.replaceFirst(".*?\\bsaythis|.*?\\bSaythis", "").trim();
			
			//make it THE command
			possibleCMDs.add(CMD.REPEAT_ME);
			possibleScore.add(1);	index++;
			possibleScore.set(index, 1000); 	//definitely this!
			
			Map<String, String> pv = new HashMap<String, String>();
				pv.put(PARAMETERS.REPEAT_THIS, this_text);
			possibleParameters.add(pv);
		}	
		
		//--set certainty_lvl--
		int bestScoreIndex = 0;
		if (possibleScore.size()>0){
			int bestScore = Collections.max(possibleScore);
			int totalScore = 0;
			//kind'a stupid double loop but I found no better way to first get total score 
			for (int i=0; i<possibleScore.size(); i++){
				totalScore += possibleScore.get(i);
				//System.out.println("CMD: " + possibleCMDs.get(i)); 		//debug
				//System.out.println("SCORE: " + possibleScore.get(i)); 	//debug
			}
			for (int i=0; i<possibleScore.size(); i++){
				if (possibleScore.get(i) == bestScore){
					bestScoreIndex = i;
					break;		//take the first if scores are equal
				}
			}
			certainty_lvl = Math.round(((double) bestScore)/((double) totalScore)*100.0d)/100.0d;
		}else{
			certainty_lvl = 0.0d;
		}
		
		//check if there was any result - if not add the no_result API
		if (possibleCMDs.isEmpty()){
			possibleCMDs.add(CMD.NO_RESULT);
			possibleScore.add(1);
			HashMap<String, String> pv = new HashMap<String, String>();
				pv.put("text", text);
			possibleParameters.add(pv);
			certainty_lvl = 1.0d;
		}
		
		//create the result with default constructor and add specific variables:
		NluResult result = new NluResult(possibleCMDs, possibleParameters, possibleScore, bestScoreIndex);
		result.certaintyLvl = certainty_lvl;
		//copy the default variables from input (like environment, mood etc.) and add input to result:
		result.setInput(input);
		result.normalizedText = text;	//input has the real text, result has the normalized text
		//you can set some of the default result variables manually if the interpreter changes them:
		result.language = language;		// might well be analyzed and changed by the interpreter, in this case here it must be English
		//result.context = context;		// is auto-set inside the constructor to best command 
		//result.mood = mood;			// typically only APIs change the mood
		
		return result;
	}

	//certainty
	public double getCertaintyLevel(NluResult result) {
		return result.certaintyLvl;
	}
}
