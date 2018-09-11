package net.b07z.sepia.server.assist.parameters;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * The generic parameters simply returns the input. Should be used for parameters that have no handler yet.
 * 
 * @author Florian Quirin
 *
 */
public class GenericParameter implements ParameterHandler{

	User user;
	NluInput nluInput;
	String language;
	boolean buildSuccess = false;
	
	//keep that in mind
	String found = "";		//exact (not generalized) string found during extraction (or guess?)
	
	@Override
	public void setup(NluInput nluInput) {
		this.nluInput = nluInput;
		this.user = nluInput.user;
		this.language = nluInput.language;
	}
	@Override
	public void setup(NluResult nluResult) {
		this.nluInput = nluResult.input;
		this.user = nluResult.input.user;
		this.language = nluResult.language;
	}
	
	@Override
	public boolean isGeneric(){
		return true;
	}

	@Override
	public String extract(String input) {
		return input;
	}
	
	@Override
	public String guess(String input) {
		return "";
	}
	
	@Override
	public String getFound() {
		return found;
	}

	@Override
	public String remove(String input, String found) {
		return input;
	}
	
	@Override
	public String responseTweaker(String input){
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

	@Override
	public boolean buildSuccess() {
		return buildSuccess;
	}

}
