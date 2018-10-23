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
		heater;
	}
	
	//Parameter local type names
	public static HashMap<String, String> types_de = new HashMap<>();
	public static HashMap<String, String> types_en = new HashMap<>();
	static {
		types_de.put("light", "das Licht");
		types_de.put("heater", "die Heizung");
		
		types_en.put("light", "the light");
		types_en.put("heater", "the heater");
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
			return "";
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
			type = NluTools.stringFindFirst(input, "(licht(er|)|lampe(n|)|beleuchtung|leuchte(n|)|helligkeit|"
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
		String type = getType(input, language);
		if (type.isEmpty()){
			return "";
		}else{
			//we should take device names (tag/number..) into account, like "Lamp 1", "Light A" or "Desk-Lights" etc.
			String deviceWithNumber = NluTools.stringFindFirst(input, type + " \\d+");
			if (!deviceWithNumber.isEmpty()){
				this.found = deviceWithNumber;
			}else{
				this.found = type;
			}
			//System.out.println("found: " + this.found); 		//DEBUG
			
			//TODO: needs further work ->  create an additional parameter called SMART_DEVICE_NAME
		}
		
		//classify into types:
		
		if (NluTools.stringContains(type, "licht(er|)|lampe(n|)|beleuchtung|leuchte(n|)|helligkeit|"
				+ "light(s|)|lighting|lamp(s|)|illumination|brightness")){
			return "<" + Types.light.name() + ">";
			
		}else if (NluTools.stringContains(type, "heiz(er|ungen|ung|koerper|luefter|strahler)|thermostat(s|)|temperatur(regler|en|)|"
				+ "heater(s|)|temperature(s|)")){
			return "<" + Types.heater.name() + ">";
		
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
			return input.replaceAll(".*\\b(einen|einem|einer|eine|ein|der|die|das|den|ne|ner)\\b", "").trim();
		}else{
			return input.replaceAll(".*\\b(a|the)\\b", "").trim();
		}
	}

	@Override
	public String build(String input) {
		
		//expects a type!
		String commonValue = input.replaceAll("^<|>$", "").trim();
		String localValue = getLocal(input, language);
		
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.VALUE, commonValue);
			JSON.add(itemResultJSON, InterviewData.VALUE_LOCAL, localValue);
			//JSON.add(itemResultJSON, InterviewData.SMART_DEVICE_NAME, this.found); 	//not working because this.found is not set in build
		
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
