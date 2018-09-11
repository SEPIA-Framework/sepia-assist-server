package net.b07z.sepia.server.assist.parameters;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Variation of NUMBER parameter that only finds values that can be applied to devices like plain, percent and temperature.
 * 
 * @author Florian Quirin
 *
 */
public class SmartDeviceValue implements ParameterHandler{

	User user;
	String language;
	boolean buildSuccess = false;
	NluInput nluInput;
	
	//keep that in mind
	String found = "";		//exact (not generalized) string found during extraction (or guess?)
	
	@Override
	public void setup(NluInput nluInput) {
		this.user = nluInput.user;
		this.language = nluInput.language;
		this.nluInput = nluInput;
	}
	@Override
	public void setup(NluResult nluResult) {
		this.user = nluResult.input.user;
		this.language = nluResult.language;
		this.nluInput = nluResult.input;
	}
	
	@Override
	public String extract(String input) {
		String number;
		String foundCommon;
		
		//check storage first - use universal NUMBER storage
		ParameterResult pr = nluInput.getStoredParameterResult(PARAMETERS.NUMBER);
		if (pr != null){
			number = pr.getExtracted();
			foundCommon = pr.getFound();
		}else{
			number = Number.extract(input, this.nluInput);
			foundCommon = number;
		}
		
		//store common value?
		if (number.trim().isEmpty()){
			return "";
		}else{
			//store common
			pr = new ParameterResult(PARAMETERS.NUMBER, number, foundCommon);
			nluInput.addToParameterResultStorage(pr);
		}
		
		//check accepted types
		String type = Number.getTypeClass(number, language).replaceAll("^<|>$", "").trim();
		if (NluTools.stringContains(type, 
				Number.Types.plain.name() + "|" + 
				Number.Types.percent.name() + "|" + 
				Number.Types.temperature.name())){
			
			this.found = number;
			//System.out.println("PARAMETER-NUMBER - found: " + this.found);					//DEBUG
			
			//store type of this - NOTE: we use this for 'type' here, we got the rest in NUMBER already
			pr = new ParameterResult(PARAMETERS.SMART_DEVICE_VALUE, type, this.found);
			nluInput.addToParameterResultStorage(pr);
			
			return number;
		
		}else{
			return "";
		}
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
		return NluTools.stringRemoveFirst(input, found);
	}
	
	@Override
	public String responseTweaker(String input){
		return input;
	}

	@Override
	public String build(String input) {
		//expects a number including type as string
		String type;
		
		//check storage first - use this NUMBER variation this time: SMART_DEVICE_VALUE
		ParameterResult pr = nluInput.getStoredParameterResult(PARAMETERS.SMART_DEVICE_VALUE);
		if (pr != null){
			type = pr.getExtracted(); 		//NOTE: it's type not number here
			input = pr.getFound();			//... and we overwrite input with the already extracted "number + type string"
		
		//extract if not stored
		}else{
			type = Number.getTypeClass(input, language).replaceAll("^<|>$", "").trim();
		}
		
		String value = input.replaceFirst(".*?(" + Number.PLAIN_NBR_REGEXP + ").*", "$1").trim();
		
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.INPUT, input);
			JSON.add(itemResultJSON, InterviewData.VALUE, value.replaceAll(",", "."));
			JSON.add(itemResultJSON, InterviewData.NUMBER_TYPE, type);
		
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
