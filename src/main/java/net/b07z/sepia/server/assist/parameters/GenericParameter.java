package net.b07z.sepia.server.assist.parameters;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * The generic parameters simply returns the input. Should be used for parameters that have no handler yet
 * or for those that can only be called by direct-commands (where you parse the data yourself).
 * 
 * @author Florian Quirin
 *
 */
public class GenericParameter extends CustomParameter implements ParameterHandler{

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
