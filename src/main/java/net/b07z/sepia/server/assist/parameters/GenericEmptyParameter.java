package net.b07z.sepia.server.assist.parameters;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * The generic-empty parameter simply returns an empty string as VALUE or a user defined string via direct command. 
 * Should be used for parameters that have no handler yet, are optional and should only contain data if explicitly set via direct commands.<br>
 * Note: Generic parameters do not increase matching score in NLU process.
 * 
 * @author Florian Quirin
 *
 */
public class GenericEmptyParameter extends CustomParameter implements ParameterHandler{

	@Override
	public boolean isGeneric(){
		return true;
	}

	@Override
	public String extract(String input) {
		return "";
	}
	
	@Override
	public String build(String input) {
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.VALUE, input);
			JSON.add(itemResultJSON, "isGeneric", true);
		
		buildSuccess = true;
		return itemResultJSON.toJSONString();
	}

	@Override
	public boolean validate(String input) {
		if (input.matches("^\\{\".*\"(\\s|):.+\\}$") && input.contains("\"" + InterviewData.VALUE + "\"")){
			//System.out.println("IS VALID: " + input); 		//debug
			return true;
		}else{
			return false;
		}
	}
}
