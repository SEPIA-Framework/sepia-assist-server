package net.b07z.sepia.server.assist.parameters;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.smarthome.SmartHomeDevice;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Variation of NUMBER parameter that only finds values that can be applied to devices like plain, percent and temperature.
 * 
 * @author Florian Quirin
 *
 */
public class SmartDeviceValue implements ParameterHandler {
	
	//--- data ---
	
	//conversion map to generalized state from other parameter results
	public static Map<String, String> numberTypeDeviceStateTypeMap = new HashMap<>();
	static {
		numberTypeDeviceStateTypeMap.put(Number.Types.plain.name(), SmartHomeDevice.STATE_TYPE_NUMBER_PLAIN);
		numberTypeDeviceStateTypeMap.put(Number.Types.percent.name(), SmartHomeDevice.STATE_TYPE_NUMBER_PERCENT);
		numberTypeDeviceStateTypeMap.put(Number.Types.temperature.name(), SmartHomeDevice.STATE_TYPE_NUMBER_TEMPERATURE_C); 	//default: celsius!
	}
		
	//-----------

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
		//if we don't have any number ..
		if (number.trim().isEmpty()){
			return "";
		}else{
			//store first common number for other parameters (there might be more though)
			pr = new ParameterResult(PARAMETERS.NUMBER, number, foundCommon);
			nluInput.addToParameterResultStorage(pr);
		}

		//get number type
		TypeAndNumber typeNum = checkTypeAndReturnNumber(input, number, null);
		String type = typeNum.type;
		number = typeNum.number;
		
		//TODO: convert temperature from farenheit to celsius?
		
		if (number.isEmpty()){
			return "";
		}else{
			this.found = number;
			//System.out.println("PARAMETER-NUMBER - found: " + this.found);					//DEBUG
			
			//store type of this - NOTE: we use this for 'type' here, we got the rest in NUMBER already
			pr = new ParameterResult(PARAMETERS.SMART_DEVICE_VALUE, type, this.found);
			nluInput.addToParameterResultStorage(pr);

			return number;
		}
	}
	
	/**
	 * Recursively run type check again until we got a valid number or no matches anymore.
	 * @param input
	 * @param number
	 * @param deviceStringFound
	 * @return
	 */
	private TypeAndNumber checkTypeAndReturnNumber(String input, String number, String deviceStringFound){
		//get number type
		String type = Number.getTypeClass(number, language).replaceAll("^<|>$", "").trim();
		
		//check plain type first because it could be a device tag like "light 1"
		if (NluTools.stringContains(type, 
				Number.Types.plain.name())){
			
			//load device to better evaluate plain numbers - Note: usually this is already extracted before so we don't store it
			if (Is.nullOrEmpty(deviceStringFound)){
				ParameterResult devicePr = ParameterResult.getResult(nluInput, PARAMETERS.SMART_DEVICE, input);
				deviceStringFound = devicePr.getFound();
			}
			
			//The device string can be something like "lamp 1" ...
			if (Is.notNullOrEmpty(deviceStringFound) && NluTools.stringContains(deviceStringFound, number)){
				//In this case we need to search again
				String filteredInput = NluTools.stringRemoveFirst(input, Pattern.quote(deviceStringFound));
				number = Number.extract(filteredInput, this.nluInput);
				if (Is.notNullOrEmpty(number)){
					return checkTypeAndReturnNumber(filteredInput, number, null);
				}else{
					return new TypeAndNumber(type, "");
				}
				
			}else{
				return new TypeAndNumber(type, number);
			}
		
		//then check other accepted types
		}else if (NluTools.stringContains(type,  
				Number.Types.percent.name() + "|" + 
				Number.Types.temperature.name())){
			
			return new TypeAndNumber(type, number);
		
		//no type fits
		}else{
			return new TypeAndNumber(type, "");
		}
	}
	/**
	 * Helper class for passing through type and number at the same time in method above.
	 */
	private static class TypeAndNumber {
		String type;
		String number;
		public TypeAndNumber(String type, String number){
			this.type = type;
			this.number = number;
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
