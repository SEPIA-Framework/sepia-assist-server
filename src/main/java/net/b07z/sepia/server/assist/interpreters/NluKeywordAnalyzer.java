package net.b07z.sepia.server.assist.interpreters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interviews.InterviewServicesMap;
import net.b07z.sepia.server.assist.parameters.AbstractParameterSearch;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.ConfigServices;
import net.b07z.sepia.server.assist.services.OpenCustomLink;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Simple (at least from the idea) yet effective keyword analyzer to interpret user input.<br>
 * This is the language independent version.<br>
 * The order of checks does matter, put commands with less priority at the bottom. If commands score the same probability the first is taken.<br>
 * Note: Don't reuse this, always create new instances of it when needed (compared to the sentence matchers that can be used globally). 
 *   
 * @author Florian Quirin
 *
 */
public class NluKeywordAnalyzer implements NluInterface {

	private double certainty_lvl = 0.0d;		//how certain is ILA about a result (I mean SEPIA ^^)
	
	//Cache:
	private static List<ServiceInterface> systemServicesWithRexX; 		//this will be filled on first interpretation call and used until deleted.
	
	/**
	 * This is an abstract regular expression analyzer that can be used to evaluate a custom service that has a defined regular expression trigger.
	 * @param text - the text to search
	 * @param input - NluInput
	 * @param service - a service (custom or system) that has all the necessary apiInfo
	 * @param possibleCMDs - required list of the parent NLU method that collects possible commands
	 * @param possibleScore - required list that stores the scores of the possible commands
	 * @param possibleParameters - required list that stores the parameter-value pairs of the possible commands
	 * @param index - integer indicating the current position in the possibleCMDs list
	 * @return new index position in possibleCMDs, will be unchanged to input if custom service regEx is not found in 'text'
	 */
	public static int abstractRegExAnalyzer(String text, NluInput input, ServiceInterface service,
						List<String> possibleCMDs, List<Integer> possibleScore, List<Map<String, String>> possibleParameters,
						int index){
		//get service info
		ServiceInfo serviceInfo = service.getInfoFreshOrCache(input, service.getClass().getCanonicalName());
		String regEx = serviceInfo.getCustomTriggerRegX(input.language);
		
		//has regEx at all?
		if (regEx == null || regEx.isEmpty()){
			return index;
		}
		
		//check the trigger
		if (NluTools.stringContains(text, regEx)){
			
			if (Is.nullOrEmpty(serviceInfo.intendedCommand)){
				throw new RuntimeException("Service is missing command name!"); 	//make sure custom service is properly set up
			}
			possibleCMDs.add(serviceInfo.intendedCommand);
			possibleScore.add(1 + serviceInfo.getCustomTriggerRegXscoreBoost());	index++;
			
			//get all parameters
			List<Parameter> paramsList = serviceInfo.getAllParameters();
			Map<String, String> pv = new HashMap<>(); 		//TODO: pass this down to avoid additional checking? Is it still relevant with parameter cache?
			if (!paramsList.isEmpty()){
				/*
				String[] params = new String[paramsList.size()];
				int i = 0;
				for (Parameter p : paramsList){
					params[i] = p.getName();
					//System.out.println("RegX Analyzer, param: " + params[i]); 		//DEBUG
					i++;
				}
				*/
				AbstractParameterSearch aps = new AbstractParameterSearch()
						//.setParameters(params)
						.setParameters(paramsList.toArray(new Parameter[paramsList.size()]))
						.setup(input, pv);
				aps.getParameters();
				possibleScore.set(index, possibleScore.get(index) + aps.getScore());
			}
			possibleParameters.add(pv);
		}
		return index;
	}
	
	/**
	 * Run keyword analyzer for system services that have defined custom regX triggers. Uses only master services (first service per cmd).<br>
	 * Note: traditionally system service regExp are defined in the language specific keyword-analyzer methods but can be moved entirely to 
	 * the services when this method is active. The order of execution is the order of commands in {@link InterviewServicesMap} (important if 
	 * scores for services are equal).
	 * @param input - NluInput with normalized .text
	 * @param possibleCMDs - Empty or pre-filled list that gets updated
	 * @param possibleScore - Empty or pre-filled list that gets updated
	 * @param possibleParameters - Empty or pre-filled list that gets updated
	 * @param index - integer indicating the current position in the possibleCMDs list
	 * @return new index position in possibleCMDs, will be unchanged compared to input if no custom service regEx triggers
	 */
	public static int runSystemServices(NluInput input, List<String> possibleCMDs, 
				List<Integer> possibleScore, List<Map<String, String>> possibleParameters, int index){
		
		String text = input.text;
		
		//Fill cache if necessary
		if (systemServicesWithRexX == null){
			systemServicesWithRexX = new CopyOnWriteArrayList<>();
			//get all master services
			List<ServiceInterface> systemServices = ConfigServices.getAllSystemMasterServices();
			for (ServiceInterface service : systemServices){
				//search the ones with regX triggers
				ServiceInfo serviceInfo = service.getInfoFreshOrCache(input, service.getClass().getCanonicalName());
				if (serviceInfo.hasCustomTriggerRegX()){
					systemServicesWithRexX.add(service);
				}
			}
		}
		//Run
		for (ServiceInterface service : systemServicesWithRexX){
			//System.out.println("Custom regX for service: " + service.getClass().getSimpleName()); 		//DEBUG
			index = abstractRegExAnalyzer(text, input, service,
					possibleCMDs, possibleScore, possibleParameters, index);
		}
		
		return index;
	}
	/**
	 * Run keyword analyzer for custom SDK made services using user-ID and assistant-ID.
	 * @param input - NluInput with normalized .text
	 * @param possibleCMDs - Empty or pre-filled list that gets updated
	 * @param possibleScore - Empty or pre-filled list that gets updated
	 * @param possibleParameters - Empty or pre-filled list that gets updated
	 * @param index - integer indicating the current position in the possibleCMDs list
	 * @return new index position in possibleCMDs, will be unchanged compared to input if no custom service regEx triggers
	 */
	public static int runCustomSdkServices(NluInput input, List<String> possibleCMDs, 
				List<Integer> possibleScore, List<Map<String, String>> possibleParameters, int index){
		//SDK allowed?
		if (Config.enableSDK){
			
			String text = input.text;
			
			//----- USER SDK SERVICES -----
			List<ServiceInterface> customServices = ConfigServices.getCustomServicesList(input, input.user);
			for (ServiceInterface service : customServices){
				index = abstractRegExAnalyzer(text, input, service,
						possibleCMDs, possibleScore, possibleParameters, index);
			}
			
			//----- ASSISTANT SDK SERVICES -----
			List<ServiceInterface> assistantServices = ConfigServices.getCustomServicesList(input, Config.getAssistantUser());
			for (ServiceInterface service : assistantServices){
				index = abstractRegExAnalyzer(text, input, service,
						possibleCMDs, possibleScore, possibleParameters, index);
			}
		}
		return index;
	}
	
	//----------- Interface -----------
	
	@Override
	public NluResult interpret(NluInput input) {
		
		//get parameters from input
		String text = input.text;
		String language = input.language;
		/*
		context = input.context;
		environment = input.environment;
		mood = input.mood;
		*/
		
		//Normalize text, e.g.:
		// all lowerCase - remove all ',!? - handle ä ö ü ß ... trim
		Normalizer normalizer = Config.inputNormalizers.get(language);
		if (normalizer != null){
			text = normalizer.normalizeText(text);
		}
		
		//Prepare results
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
		
		//----- SYSTEM SERVICES -----
		
		index = runSystemServices(input, possibleCMDs, possibleScore, possibleParameters, index);
				
		//----- CUSTOM SERVICES -----
		
		index = runCustomSdkServices(input, possibleCMDs, possibleScore, possibleParameters, index);
		
		//---------------------------
								
		//SLASH Commands and specials - will overwrite all other commands! - TODO: we should put all slash-commands in one extra place
		//NOTE: use 'RegexParameterSearch.containsSlashCMD' (again - this class was triggered by it) here?
		
		//Repeat me
		if (NluTools.stringContains(text, "(^saythis|^\\\\saythis|" + Pattern.quote(Config.assistantName) + " saythis)")){
			String thisText = input.textRaw.replaceFirst("(?i).*?\\bsaythis", "").trim();
			//make it THE command
			possibleCMDs.add(CMD.REPEAT_ME);
			possibleScore.add(1);	index++;
			possibleScore.set(index, 1000); 	//definitely this!
			Map<String, String> pv = new HashMap<String, String>();
				pv.put(PARAMETERS.REPEAT_THIS, thisText);
			possibleParameters.add(pv);
		
		//Link share
		}else if (NluTools.stringContains(text, "(^linkshare|^\\\\linkshare|" + Pattern.quote(Config.assistantName) + " linkshare)")){
			String link = input.textRaw.replaceFirst("(?i).*?\\blinkshare", "").trim().replaceFirst("\\s.*", "");
			String title = link.substring(0, Math.min(link.length(), 22));
			if (link.length() > 21) title += "...";
			possibleCMDs.add(CMD.OPEN_LINK);
			possibleScore.add(1);	index++;
			possibleScore.set(index, 1000); 	//definitely this!
			Map<String, String> pv = new HashMap<String, String>();
				pv.put(PARAMETERS.URL, link);
				pv.put(OpenCustomLink.TITLE, title);
				pv.put(PARAMETERS.DATA, JSON.make("source", "linkshare").toJSONString());
			possibleParameters.add(pv);
		
		//Link open
		}else if (NluTools.stringContains(text, "^http(s|)://.*")){
			String link = input.textRaw.trim().replaceFirst("\\s.*", "");
			String title = link.substring(0, Math.min(link.length(), 22));
			if (link.length() > 21) title += "...";
			possibleCMDs.add(CMD.OPEN_LINK);
			possibleScore.add(1);	index++;
			possibleScore.set(index, 1000); 	//definitely this!
			Map<String, String> pv = new HashMap<String, String>();
				pv.put(PARAMETERS.URL, link);
				pv.put(OpenCustomLink.TITLE, title);
				pv.put(PARAMETERS.DATA, JSON.make("source", "chat").toJSONString());
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
	@Override
	public double getCertaintyLevel(NluResult result) {
		return result.certaintyLvl;
	}
}
