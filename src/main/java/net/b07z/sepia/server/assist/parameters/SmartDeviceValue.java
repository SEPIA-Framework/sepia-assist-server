package net.b07z.sepia.server.assist.parameters;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.smarthome.SmartHomeDevice;
import net.b07z.sepia.server.assist.smarthome.SmartHomeDevice.StateType;
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
	public static Map<String, SmartHomeDevice.StateType> numberTypeDeviceStateTypeMap = new HashMap<>();
	static {
		numberTypeDeviceStateTypeMap.put(Number.Types.plain.name(), SmartHomeDevice.StateType.number_plain);
		numberTypeDeviceStateTypeMap.put(Number.Types.percent.name(), SmartHomeDevice.StateType.number_percent);
		numberTypeDeviceStateTypeMap.put(Number.Types.temperature.name(), SmartHomeDevice.StateType.number_temperature); 	//default is unitless
	}
	
	/**
	 * Helper class for passing through type and number at the same time in method above. (NOTE: we could use SimpleEntry here)
	 */
	private static class TypeAndNumber {
		String type;
		String number;
		public TypeAndNumber(String type, String number){
			this.type = type;
			this.number = number;
		}
	}
		
	//-----------

	User user;
	String language;
	boolean buildSuccess = false;
	NluInput nluInput;
	
	//This parameter is an extension of the Number parameter
	ParameterHandler masterHandler;
	
	private void setMaster(NluInput nluInput){
		masterHandler = new Parameter(PARAMETERS.NUMBER).getHandler();
		masterHandler.setup(nluInput);
	}
	private void setMaster(NluResult nluResult){
		masterHandler = new Parameter(PARAMETERS.NUMBER).getHandler();
		masterHandler.setup(nluResult);
	}
	
	//keep that in mind
	String found = "";		//exact (not generalized) string found during extraction (or guess?)
	
	@Override
	public void setup(NluInput nluInput) {
		this.user = nluInput.user;
		this.language = nluInput.language;
		this.nluInput = nluInput;
		
		setMaster(nluInput);
	}
	@Override
	public void setup(NluResult nluResult) {
		this.user = nluResult.input.user;
		this.language = nluResult.language;
		this.nluInput = nluResult.input;
		
		setMaster(nluResult);
	}
	
	@Override
	public String extract(String input) {
		String number;
		String smartDeviceVal;
		
		//get result from master first
		number = masterHandler.extract(input).trim();
		this.found = masterHandler.getFound();
		
		//if we don't have any number ..
		if (number.isEmpty()){
			this.found = "";
			return "";
		}else{
			//check storage
			ParameterResult pr = nluInput.getStoredParameterResult(PARAMETERS.SMART_DEVICE_VALUE);
			if (pr != null){
				smartDeviceVal = pr.getExtracted();
				this.found = pr.getFound();
				
				return smartDeviceVal;
			}
		}
		
		//check if number type fits to smart devices and exclude things like "Lamp 1" or "Room 212"
		TypeAndNumber typeNum = checkTypeAndReturnNumber(input, number);
		String type = typeNum.type;
		number = typeNum.number;
		
		//still there?
		if (number.isEmpty()){
			this.found = "";
			return "";
		}else{
			smartDeviceVal = "<" + type + ">;;" + number;
		}
		
		//store type of this - NOTE: we use this for 'type' here, we got the rest in NUMBER already
		nluInput.addToParameterResultStorage(new ParameterResult(PARAMETERS.SMART_DEVICE_VALUE, smartDeviceVal, this.found));

		return smartDeviceVal;
	}
	
	/**
	 * Recursively run type check again until we got a valid number for smart device or no matches anymore.
	 * If a new match is found "this.found" will be updated as well.
	 * @param input - original input number was extracted from (e.g. set heater 1 in room 2 to 10°C)
	 * @param number - number string found in input (e.g. 1, 2 or 10°C)
	 * @return
	 */
	private TypeAndNumber checkTypeAndReturnNumber(String input, String number){
		//get number type
		String type = Number.getTypeClass(number, language).replaceAll("^<|>$", "").trim();
		
		//check plain type first because it could be a device tag like "light 1" or "room 212"
		if (NluTools.stringContains(type, 
				Number.Types.plain.name())){
			
			//load device and room to better evaluate plain numbers - Note: usually this is already extracted before so we don't store it
			ParameterResult devicePr = ParameterResult.getResult(this.nluInput, PARAMETERS.SMART_DEVICE, input);
			String deviceStringFound = devicePr.getFound();
			ParameterResult roomPr = ParameterResult.getResult(this.nluInput, PARAMETERS.ROOM, input);
			String roomStringFound = roomPr.getFound();
			String filteredInput = input;
			boolean needToSearchDeeper = false;
			
			//The device string can be something like "lamp 1" ...
			if (Is.notNullOrEmpty(deviceStringFound) && NluTools.stringContains(deviceStringFound, number)){
				filteredInput = NluTools.stringRemoveFirst(filteredInput, Pattern.quote(deviceStringFound));
				needToSearchDeeper = true;
			
			//The room string can be something like "room 212" ...
			}else if (Is.notNullOrEmpty(roomStringFound) && NluTools.stringContains(roomStringFound, number)){
				filteredInput = NluTools.stringRemoveFirst(filteredInput, Pattern.quote(roomStringFound));
				needToSearchDeeper = true;
			}
			
			//Do we need to search again?
			if (needToSearchDeeper){
				number = Number.extract(filteredInput, this.nluInput);
				if (Is.notNullOrEmpty(number)){
					this.found = number;
					return checkTypeAndReturnNumber(filteredInput, number);
				}else{
					return new TypeAndNumber(type, "");
				}
			}else{
				return new TypeAndNumber(type, number);
			}
			
		//then check common number + letter (we accept 20F as Fahrenheit)
		}else if (NluTools.stringContains(type, 
				Number.Types.letterend.name()) && number.endsWith("f")){
			
			type = Number.Types.temperature.name();			
			return new TypeAndNumber(type, number);
		
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
		//extract again/first? - this should only happen via predefined parameters (e.g. from direct triggers)
		if (Is.notNullOrEmpty(input) && !input.startsWith("<")){
			input = extract(input);
			if (Is.nullOrEmpty(input)){
				return "";
			}
		}
		
		//expects a type!
		String type = "";
		String value = "";
		if (input.contains(";;")){
			String[] typeAndValue = input.split(";;", 2);
			if (typeAndValue.length == 2){
				type = typeAndValue[0].replaceAll("^<|>$", "").trim();
				value = typeAndValue[1];
			}else{
				value = typeAndValue[0];
			}
		}
		
		//remove any type tag
		input = value; 	//the original "found"
		value = value.replaceFirst(".*?(" + Number.PLAIN_NBR_REGEXP + ").*", "$1").trim();
		
		//default decimal format is "1.00"
		if (!Is.typeEqual(type, Number.Types.custom)){
			value = value.replaceAll(",", ".");
		}
		
		//convert Number.Types to device SmartHomeDevice.StateType
		if (Is.notNullOrEmpty(type)){
			StateType stateType = numberTypeDeviceStateTypeMap.get(type);
			if (stateType.equals(StateType.number_temperature)){
				String foundUnit = Number.getTemperatureUnit(input, language);
				if (foundUnit.equals("C")){
					stateType = StateType.number_temperature_c;
				}else if (foundUnit.equals("F")){
					stateType = StateType.number_temperature_f;
				}
			}
			type = stateType.name();
			//TODO: convert temperature from fahrenheit to celsius? I think we leave this to any service
		}
		
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.INPUT, input);
			JSON.add(itemResultJSON, InterviewData.VALUE, value);
			JSON.add(itemResultJSON, InterviewData.SMART_DEVICE_VALUE_TYPE, type);
		
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
