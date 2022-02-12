package net.b07z.sepia.server.assist.parameters;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * The generic-empty parameter simply returns an empty string as extracted VALUE or a user defined string via direct command. 
 * Should be used for parameters that have no handler yet, are optional and should only contain data if explicitly set via
 * direct commands or questions (via response handler).<br>
 * Note 1: Works well with {@link ServiceBuilder#setIncompleteAndAsk} if you add it as optional parameter.<br>
 * Note 2: Generic parameters do not increase matching score during NLU process.
 * 
 * @author Florian Quirin
 *
 */
public class GenericEmptyParameter extends CustomParameter implements ParameterHandler {

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
			//JSON.add(itemResultJSON, InterviewData.INPUT_RAW, this.nluInput.textRaw);
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
