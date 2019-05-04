package net.b07z.sepia.server.assist.parameters;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * This is the parameter that receives the current selection request. 
 * It is basically a generic parameter that holds some extras to find the correct selection.<br>
 * Note: Generic parameters do not increase matching score in NLU process.
 * 
 * @author Florian Quirin
 *
 */
public class Select extends CustomParameter implements ParameterHandler{
	
	//-----data-----
	public static final String PREFIX = "select_"; 				//prefix for custom select parameter
	public static final String OPTIONS_PREFIX = "options_"; 	//prefix for options to custom select parameter

	//--- Methods to evaluate selection ---
	
	/**
	 * Try to match the selection options to the response.
	 * @param response - User response given
	 * @param customParameterName - parameter that will store the result
	 * @param nluResult - NluResult given
	 * @return null or JSONObject with match and number of match like this {"value": "green", "selection": 1}
	 */
	public static JSONObject matchSelection(String response, String customParameterName, NluResult nluResult){
		String optionsToParameterString = nluResult.getParameter(customParameterName.replace(PREFIX, OPTIONS_PREFIX));
		if (Is.nullOrEmpty(optionsToParameterString) || !optionsToParameterString.startsWith("{")){
			//User forgot to submit options .. this should not happen when using service-result builder properly
			Debugger.println("Missing or invalid options for select parameter '" + customParameterName + "': " + optionsToParameterString, 1);
			return null;
		}else{
			String match = "";
			int matchingKey = 0;
			try {
				JSONObject options = JSON.parseString(optionsToParameterString); 	//NOTE: Has to be built like this {"1":"Song", "2":"Playlist", "3":"Artist" ...}
				for (int i=0; i<options.size(); i++){
					String key = Integer.toString(i+1);
					String optionRegExp = JSON.getString(options, key);
					match = NluTools.stringFindFirst(response, optionRegExp);
					if (!match.isEmpty()){
						matchingKey = Integer.parseInt(key);
						break;
					}
				}
			}catch (Exception e){
				match = "";
				matchingKey = 0;
				Debugger.println("Missing or invalid options for select parameter '" + customParameterName + "': " + optionsToParameterString, 1);
			}
			return JSON.make(InterviewData.VALUE, match, "selection", matchingKey, InterviewData.INPUT, response);
		}
	}
	
	//--- Handler Interface ---
	
	@Override
	public boolean isGeneric(){
		return true;
	}

	@Override
	public String extract(String input) {
		return input;
	}
	
	@Override
	public String build(String input) {
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.INPUT_RAW, nluInput.textRaw);
			JSON.add(itemResultJSON, InterviewData.VALUE, input);
			JSON.add(itemResultJSON, "isGeneric", true);
		
		buildSuccess = true;
		return itemResultJSON.toJSONString();
	}

	@Override
	public boolean validate(String input) {
		if (input.matches("^\\{\".*\":.+\\}$") && input.contains("\"" + InterviewData.VALUE + "\"")){
			//System.out.println("IS VALID: " + input); 		//debug
			return true;
		}else{
			return false;
		}
	}
}
