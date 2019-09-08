package net.b07z.sepia.server.assist.parameters;

import java.util.HashMap;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

public class SmartDevice implements ParameterHandler{

	//-----data-----
	
	//Parameter types
	public static enum Types{
		light,
		heater,
		device;
	}
	
	//Parameter local type names
	public static HashMap<String, String> types_de = new HashMap<>();
	public static HashMap<String, String> types_en = new HashMap<>();
	static {
		types_de.put("light", "das Licht");
		types_de.put("heater", "die Heizung");
		types_de.put("device", "das Gerät");
		
		types_en.put("light", "the light");
		types_en.put("heater", "the heater");
		types_en.put("device", "the device");
	}
	
	/**
	 * Translate generalized value (e.g. &ltlight&gt) to a context based, useful local name (e.g. das Licht).
	 * If generalized value is unknown returns empty string
	 * @param type - generalized type value 
	 * @param language - ISO language code
	 */
	public static String getLocal(String type, String language){
		type = type.replaceAll("^<|>$", "").trim();
		String localName = "";
		if (language.equals(LANGUAGES.DE)){
			localName = types_de.get(type);
		}else if (language.equals(LANGUAGES.EN)){
			localName = types_en.get(type);
		}
		if (localName == null){
			Debugger.println(SmartDevice.class.getSimpleName() + " - getLocal() has no '" + language + "' version for '" + type + "'", 3);
			return getLocal(Types.device.name(), language);		//fallback
		}
		return localName;
	}
	//----------------
	
	User user;
	String language;
	boolean buildSuccess = false;
	
	//keep that in mind
	String found = "";		//exact (not generalized) string found during extraction (or guess?)
	
	@Override
	public void setup(NluInput nluInput) {
		this.user = nluInput.user;
		this.language = nluInput.language;
	}
	@Override
	public void setup(NluResult nluResult) {
		this.user = nluResult.input.user;
		this.language = nluResult.language;
	}
	
	/**
	 * Search normalized string for raw type.
	 */
	public static String getType(String input, String language){
		String type = "";
		//German
		if (language.matches(LANGUAGES.DE)){
			type = NluTools.stringFindFirst(input, "(licht(er|es|)|lampe(n|)|beleuchtung|leuchte(n|)|helligkeit|"
					+ "heiz(er|ungen|ung|koerper|luefter|strahler)|thermostat|temperatur(regler|en|)"
				+ ")");
			
		//English and other
		}else{
			type = NluTools.stringFindFirst(input, "(light(s|)|lighting|lamp(s|)|illumination|brightness|"
					+ "heater(s|)|temperature(s|)|thermostat(s|)"
				+ ")");
			
		}
		//System.out.println("Type: " + type); 		//debug
		return type;
	}

	@Override
	public String extract(String input) {
		String device = getType(input, language);
		if (device.isEmpty()){
			//no known type so let's check some general constructions
			if (language.matches(LANGUAGES.DE)){
				String re = "(von (der |dem |des |meine(r|m) |)|vom |des |meine(r|s) )";
				device = NluTools.stringFindFirst(input, "\\w*status " + re + "\\w+");
				if (!device.isEmpty()){
					device = device.replaceFirst(".*\\b" + re, "").trim();
				}
			}else{
				String re = "(status|state) of (the |a |my |)";
				device = NluTools.stringFindFirst(input, re + "\\w+");
				if (!device.isEmpty()){
					device = device.replaceFirst(".*\\b" + re, "").trim();
				}
			}
		}
		if (device.isEmpty()){
			//Still empty
			return "";
		}else{
			//we should take device names (tag/number..) into account, like "Lamp 1", "Light A" or "Desk-Lights" etc.
			String deviceWithNumber = NluTools.stringFindFirst(input, device + " \\d+");
			if (!deviceWithNumber.isEmpty()){
				this.found = deviceWithNumber;
			}else{
				this.found = device;
			}
			//System.out.println("found: " + this.found); 		//DEBUG
			
			//TODO: needs further work ->  create an additional parameter called SMART_DEVICE_NAME
		}
		
		//classify into types:
		
		if (NluTools.stringContains(device, "licht(er|es|)|lampe(n|)|beleuchtung|leuchte(n|)|helligkeit|"
				+ "light(s|)|lighting|lamp(s|)|illumination|brightness")){
			return "<" + Types.light.name() + ">" + this.found;
			
		}else if (NluTools.stringContains(device, "heiz(er|ungen|ung|koerper|luefter|strahler)|thermostat(s|)|temperatur(regler|en|)|"
				+ "heater(s|)|temperature(s|)")){
			return "<" + Types.heater.name() + ">" + this.found;
		
		}else{
			return "<" + Types.device.name() + ">" + this.found;
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
		if (language.equals(LANGUAGES.DE)){
			found = "(der |die |das |)" + found;
		}else{
			found = "(a |the |)" + found;
		}
		return NluTools.stringRemoveFirst(input, found);
	}
	
	@Override
	public String responseTweaker(String input){
		if (language.equals(LANGUAGES.DE)){
			return input.replaceAll(".*\\b(einen|einem|einer|eine|ein|der|die|das|den|dem|des|ne|ner|meine(r|s|m|))\\b", "").trim();
		}else{
			return input.replaceAll(".*\\b(a|the|my)\\b", "").trim();
		}
	}

	@Override
	public String build(String input) {
		
		//expects a type!
		String deviceName = "";
		if (NluTools.stringContains(input, "^<\\w+>")){
			String[] typeAndName = input.split(">", 2);
			if (typeAndName.length == 2){
				deviceName = typeAndName[1];
				input = typeAndName[0];
			}else{
				input = typeAndName[0];
			}
			input = input.replaceAll("^<|>$", "");
		}
		String commonValue = input.replaceAll("^<|>$", "").trim();
		String localValue = getLocal(input, language);
		
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.VALUE, commonValue);
			JSON.add(itemResultJSON, InterviewData.VALUE_LOCAL, localValue);
			JSON.add(itemResultJSON, InterviewData.FOUND, deviceName); 		//Note: we can't use this.found here because it is not set in build
		
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
