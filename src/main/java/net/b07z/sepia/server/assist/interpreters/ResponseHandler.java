package net.b07z.sepia.server.assist.interpreters;

import java.util.HashMap;
import java.util.List;

import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.parameters.Confirm;
import net.b07z.sepia.server.assist.parameters.DateAndTime;
import net.b07z.sepia.server.assist.parameters.ParameterConfig;
import net.b07z.sepia.server.assist.parameters.Parameter_Handler;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.ConfigServices;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.ClassBuilder;

/**
 * This interpreter handles responses to questions the server (assistant) asked the client (user).
 * It first reconstructs the last result (last_cmd) and then tries to replace the missing parameter using 
 * the client answer stored in input.text.<br>
 * Handling the response can be arbitrary complex, especially if the user does not follow strict ways of responding,
 * so this NLP might also include a growing number of helper methods to interpret responses.<br>
 * <br>
 * Note: Never use this as a static interpreter! Always create new instances of it when needed (compared to the sentence matchers
 * that can be used globally).
 * 
 * @author Florian Quirin
 *
 */
public class ResponseHandler implements NluInterface{
	
	//double certainty_lvl = 1.0d;		//how certain is ILA about a result
	//TODO: the response handler has no certainty level ...
	
	public NluResult interpret(NluInput input) {
		String response = input.text;
		String missing_input = input.inputMiss;
		String cmd_summary = input.lastCmd;
		String language = input.language;
		//String context = input.context;
		//String environment = input.environment;
		//int mood = input.mood;
		
		//normalize text, e.g.: all lowerCase - remove all ',!? - handle ä ö ü ß ... trim
		//TODO: think about this again, does it make sense to use the same normalizer as for normal input?
		Normalizer normalizer = Config.inputNormalizers.get(language);
		if (normalizer != null){
			response = normalizer.normalize_text(response);
		}
		
		NluResult result = NluTools.cmdSummaryToResult(input, cmd_summary);
		result.normalizedText = response;
		
		//abort?
		if (RegexParameterSearch.is_abort(response, language)){
			//abort result
			result.setCommand(CMD.ABORT);
			result.setParameter(input.inputMiss, "");
			return result;
		}
		
		//TODO: add a "wait!" command that gives user time to think about the answer
		
		//handle response - could be: yes_no, good_bad, location start/end, travel type, etc. p p...
		String cmd = result.getCommand();
		/*
		//websearch
		if (!cmd.matches("") && cmd.matches(CMD.WEB_SEARCH)){
			//search
			if (missing_input.matches(PARAMETER.SEARCH)){
				response = identify_search(response, language);
			}
			
		*/	
		//open_link
		if (!cmd.matches("") && cmd.matches(CMD.OPEN_LINK)){
			//parameter_set
			if (missing_input.matches("parameter_set")){
				String p_set = result.getParameter("parameter_set");
				if (!p_set.matches("")){
					//get parameter type if specified
					String p_type = p_set.replaceFirst(".*?(\\*\\*\\*)(.*?)(&&|$).*", "$2");
					response = tweak_parameters(response, p_type, language, cmd, input);
					response = p_set.replaceFirst("(\\*\\*\\*).*?(&&|$)", response + "$2");
				}else{
					//leave it alone and take the full response 
				}
			}
			//set
			result.setParameter(input.inputMiss, response);
		
		//else: automatic tweaking relies on parameter names
		}else{
			//System.out.println("response: " + response); 						//DEBUG
			//conventional parameter
			if (ParameterConfig.hasHandler(missing_input)){
				result = tweakResult(result, response, missing_input, language, cmd, input);
			
			//check for special (dynamic) parameters
			}else if (missing_input.startsWith(Confirm.PREFIX)){
				result = confirmResponse(result, response, missing_input, language, cmd, input);
			
			//legacy parameters
			}else{
				response = tweak_parameters(response, missing_input, language, cmd, input);
				//set
				result.setParameter(input.inputMiss, response);
			}
			//System.out.println("result: " + result.getBestResultJSON()); 		//DEBUG
		}
		
		//return modified nlu_result
		return result;
	}

	public double getCertaintyLevel(NluResult result) {
		return result.certaintyLvl;
	}
	
	//-------HELPER METHODS--------
	
	public static NluResult tweakResult(NluResult nluResult, String response, String parameter, String language, String command, NluInput input){
		String tweaked = response;		//by default it is the pure response
		
		//get all non-final, optional parameters and remove them
		List<ServiceInterface> services = ConfigServices.getCustomOrSystemServices(input, input.user, command);
		ServiceInfo info = services.get(0).getInfo(nluResult.language);
		for (Parameter p : info.optionalParameters){
			//is parameter already final?
			if (nluResult.isParameterFinal(p.getName())){
				continue;
			}
			//else try to find it
			Parameter_Handler handler = p.getHandler();
			handler.setup(input);
			//check if its a generic handler, because that one would not search for stuff if its not asked explicitly
			if (handler.isGeneric() && !input.inputMiss.equals(p.getName())){
				//System.out.println("ResponseHandler - tweakResult - skipped to search response of '" + parameter + "' for: " + p.getName()); 		//debug
				continue;
			}
			//First try the normal 'extract' method:
			String value = handler.extract(response);
			//Then do some special stuff:
			if (!value.isEmpty()){
				//System.out.println("ResponseHandler - tweakResult - p: " + p.getName() + ", value: " + value); 					//debug
				//nluResult.remove_final(p.getName());
				nluResult.setParameter(p.getName(), value);
				//TODO: use the remove method?
				if (command.equals(CMD.FASHION)){
					if (p.getName().equals(PARAMETERS.COLOR)){
						tweaked = handler.remove(tweaked, handler.getFound());
					}else if (p.getName().equals(PARAMETERS.FASHION_SIZE)){
						tweaked = handler.remove(tweaked, handler.getFound());
					}else if (p.getName().equals(PARAMETERS.GENDER)){
						tweaked = handler.remove(tweaked, handler.getFound());
					}
				}
			}else{
				//TODO: guess or not? I think no guessing in answer is better
			}
		}
		//now get the missing one:

		Parameter_Handler handler = (Parameter_Handler) ClassBuilder.construct(ParameterConfig.getHandler(parameter));
		handler.setup(input);
		
		//First try the normal 'extract' method:
		String value = handler.extract(response);
		if (!value.isEmpty()){
			nluResult.setParameter(parameter, value);
			//TODO: use the remove method?
			
		//If 'extract' failed try responseTweaker:
		}else{
			tweaked = handler.responseTweaker(tweaked);
			//TODO: guess or not? I think no guessing in answer is better
			nluResult.setParameter(parameter, (tweaked.isEmpty())? response : tweaked);
		}
		
		return nluResult;
	}
	
	//check for special (dynamic) parameters
	public static NluResult confirmResponse(NluResult nluResult, String response, String parameter, String language, String command, NluInput input){
		//Confirmation request
		Parameter_Handler handler = (Parameter_Handler) ClassBuilder.construct(ParameterConfig.getHandler(PARAMETERS.CONFIRMATION));
		handler.setup(input);
		String value = handler.extract(response);
		nluResult.setParameter(parameter, value);
		
		return nluResult;
	}
	
	/**
	 * Tweak response parameters by cleaning up the string (database check is done by APIs). Also replaces here etc. with [user_home] etc..
	 * @param response - response given by user
	 * @param p_type - parameter type declared in parameter_set (e.g.: ***place) or somewhere else
	 * @param language - language code
	 * @param command - the tweaking might depend on a command
	 * @return cleaned up parameter
	 */
	public static String tweak_parameters(String response, String p_type, String language, String command, NluInput input){
		String tweaked = response;		//by default it is the pure response
		p_type = p_type.toLowerCase().trim();
		//System.out.println("p_type: " + p_type + ", response: " + response); 		//debug
		
		//locations general
		if (p_type.matches("(" + PARAMETERS.PLACE + ")")){
			//tweaked = identify_location_general(response, language);
			HashMap<String, String> locations = RegexParameterSearch.get_locations(response, language);
			String loc = locations.get("location");
			String loc_s = locations.get("location_start");			String loc_e = locations.get("location_end");
			if (!loc.isEmpty()){
				tweaked = loc;
			}else if (!loc_e.isEmpty()){
				tweaked = loc_e;
			}else if (!loc_s.isEmpty()){
				tweaked = loc_s;
			}
		//locations start/end
		}else if (p_type.matches(PARAMETERS.LOCATION_END)){
			//tweaked = identify_location_end(response, language);
			HashMap<String, String> locations = RegexParameterSearch.get_locations(response, language);
			String loc = locations.get("location");			String loc_e = locations.get("location_end");
			if (!loc_e.isEmpty()){
				tweaked = loc_e;
			}else if (!loc.isEmpty()){
				tweaked = loc;
			}
		}else if (p_type.matches(PARAMETERS.LOCATION_START)){
			//tweaked = identify_location_general(response, language);			//improve?
			HashMap<String, String> locations = RegexParameterSearch.get_locations(response, language);
			String loc = locations.get("location");			String loc_s = locations.get("location_start");
			if (!loc_s.isEmpty()){
				tweaked = loc_s;
			}else if (!loc.isEmpty()){
				tweaked = loc;
			}
			
		//time
		}else if (p_type.matches(PARAMETERS.TIME) || p_type.matches(PARAMETERS.TIME_END)){
			HashMap<String, String> dateMap = RegexParameterSearch.get_date(response, language);
			String[] dateRes = DateAndTime.convertTagToDate(dateMap, input);
			tweaked = dateRes[0] + Config.defaultSdfSeparator + dateRes[1];
			
		//search
		//TODO: music, tickets (optimize: suche nach tickets für ...)
		}else if (p_type.matches(PARAMETERS.SEARCH)){
			tweaked = identify_search(response, language);
		//number
		}else if (p_type.matches(PARAMETERS.NUMBER)){
			tweaked = RegexParameterSearch.get_number(response);
		//smart devices
		}else if (p_type.matches(PARAMETERS.SMART_DEVICE)){
			tweaked = RegexParameterSearch.get_control_type(response, language);	
		}else if (p_type.matches(PARAMETERS.SMART_LOCATION)){
			tweaked = RegexParameterSearch.get_control_location(response, language);
		
		//command specific:
		}else if (command.equals(CMD.CONTROL) || command.equals(CMD.SMARTDEVICE)){
			//-control and smart devices
			if (p_type.matches(PARAMETERS.INFO)){
				tweaked = RegexParameterSearch.get_control_location(response, language);
			}else if (p_type.matches(PARAMETERS.TYPE)){
				tweaked = RegexParameterSearch.get_control_type(response, language);
			}else if (p_type.matches(PARAMETERS.ACTION)){
				tweaked = RegexParameterSearch.get_control_action(response, language)[0];
			}
		}else if (command.equals(CMD.BANKING)){
			//-banking
			if (p_type.matches(PARAMETERS.ACTION)){
				tweaked = RegexParameterSearch.get_banking_action(response, language);
			}
		}else if (command.equals(CMD.MY_FAVORITE)){
			//-my info/favorites
			if (p_type.matches(PARAMETERS.TYPE)){
				tweaked = RegexParameterSearch.get_my_info_item(response, language);
			}else if (p_type.matches(PARAMETERS.INFO)){
				tweaked = RegexParameterSearch.get_my_info(response, language);
			}
		}
		//System.out.println("tweaked: " + tweaked); 		//debug
		
		return tweaked;
	}
	
	//TODO: is this really necessary to make its own methods? Can't we just use NLU_parameter_search
	//		hmmmm, I think we should make it a little less strict, thats why ...
	
	/**
	 * Identify a destination (end) for location services like navigation
	 * @param input - what the user said, e.g. "I want to go to Paris" (result=Paris).
	 * @param language - ... its clear or?
	 * @return the destination as string would be nice :-)
	 */
	public static String identify_location_end(String input, String language){
		String end = "";
		
		//German
		if (language.matches("de")){
			input = input.replaceFirst("(zu hause)", "hause");
			end = input.replaceFirst(".*?\\b(nach|zur|zu)\\b(.*)", "$2").trim().toLowerCase();
			end = end.replaceAll("\\b^(der|die|das|einer|einen)\\b", "").trim();
			end = end.replaceAll("\\b(^zuhause|^hause)\\b", "<user_home>");
			end = end.replaceAll("\\b(^hier|^diesem ort|^meinem standort|^standort)\\b", "<user_location>");
			end = end.replaceAll("\\b(^meine arbeit|^meiner arbeit|^arbeit)\\b", "<user_work>");
			
		//English
		}else if (language.matches("en")){
			input = input.replaceFirst("(i need to|i have to|to go|to drive|to walk|to find|to do|to bring|to search|start to)", "");
			end = input.replaceFirst(".*?\\b(to)\\b(.*)", "$2").trim().toLowerCase();
			end = end.replaceAll("\\b^(a|the)\\b", "").trim();
			end = end.replaceAll("\\b(^my place|^my home|^home)\\b", "<user_home>");
			end = end.replaceAll("\\b(^here|^this place|^my location|^my position)\\b", "<user_location>");
			end = end.replaceAll("\\b(^my work|^work)\\b", "<user_work>");
		
		//no result/missing language support ...
		}else{
			end = input;
		}
		return end; 
	}
	
	/**
	 * Identify a place (general) for location services like navigation
	 * @param input - what the user said, e.g. "I want to go to Paris" (result=Paris).
	 * @param language - ... its clear or?
	 * @return the place/location as string would be nice :-)
	 */
	public static String identify_location_general(String input, String language){
		String location = "";
		
		//German
		if (language.matches("de")){
			location = input.replaceFirst(".*?\\b(von|ab|nach|zur|zu|in|^am|^auf)\\b(.*)", "$2").trim().toLowerCase();
			location = location.replaceAll("\\b(^der|^die|^das|^ein|^eine|^einen)\\b", "").trim();
			location = location.replaceAll("\\b(^naechste|^naechsten)\\b", "").trim();
			location = location.replaceAll("\\b(^zu hause|^hause)\\b", "<user_home>");
			location = location.replaceAll("\\b(^hier|^diesem ort)\\b", "<user_location>");
			location = location.replaceAll("\\b(^meine arbeit|^meiner arbeit|^arbeit)\\b", "<user_work>");
			
		//English
		}else if (language.matches("en")){
			location = input.replaceFirst(".*?\\b(from|of|to|in|^at)\\b(.*)", "$2").trim().toLowerCase();
			location = location.replaceAll("\\b(^the|^a)\\b", "").trim();
			location = location.replaceAll("\\b(^closest)\\b", "").trim();
			location = location.replaceAll("\\b(^my place|^my home|^home)\\b", "<user_home>");
			location = location.replaceAll("\\b(^here|^this place|^my location|^my position)\\b", "<user_location>");
			location = location.replaceAll("\\b(^my work|^work)\\b", "<user_work>");
		
		//no result/missing language support ...
		}else{
			location = input;
		}
		return location; 
	}
	
	/**
	 * Identify a search
	 * @param input - what the user said, e.g. "I want to go to Paris" (result=Paris).
	 * @param language - ... its clear or?
	 * @return the tweaked search
	 */
	public static String identify_search(String input, String language){
		String search = "";
		
		//German
		if (language.matches("de")){
			search = input.replaceFirst(".*?\\b(^suche nach|^nach|^finde|^zeig mir|^zeig|^fuer|^fuers)\\b(.*)", "$2").trim();
			search = search.replaceAll("\\b(^der|^die|^das|^ein|^eine|^einen)\\b", "").trim();
			
		//English
		}else if (language.matches("en")){
			search = input.replaceFirst(".*?\\b(^search for|^for|^find|^show me|^show)\\b(.*)", "$2").trim().toLowerCase();
			search = search.replaceAll("\\b(^the|^a|^an)\\b", "").trim();
		
		//no result/missing language support ...
		}else{
			search = input;
		}
		return search; 
	}
	
	/**
	 * Identify list info item. For now it just splits the "and".
	 * @param input - what the user said, e.g. "put milk and bread on the list" (result=milk, bread).
	 * @param language - ... its clear or?
	 * @return the tweaked info
	 */
	public static String identify_list_info(String input, String language){
		String item = "";
		
		//German
		if (language.matches("de")){
			item = input.replaceFirst("\\b(und)\\b", "&&").trim();
			
		//English
		}else if (language.matches("en")){
			item = input.replaceFirst("\\b(and)\\b", "&&").trim();
		
		//no result/missing language support ...
		}else{
			item = input;
		}
		return item; 
	}

}
