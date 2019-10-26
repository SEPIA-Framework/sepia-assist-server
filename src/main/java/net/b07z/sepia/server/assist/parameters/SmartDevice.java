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
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Parameter to find any smart device like smart home lights, heater, tv, music player etc. 
 * 
 * @author Florian Quirin
 * 
 */
public class SmartDevice implements ParameterHandler{

	//-----data-----
	
	/**
	 * Smart devices typically found in a smart home like lights, heater, tv, music player etc. 
	 */
	public static enum Types {
		light,
		heater,
		tv,
		music_player,
		fridge,
		oven,
		coffee_maker,
		device;
	}
	/**
	 * Get generalized 'Types' value in format of extraction method.
	 */
	public static String getExtractedValueFromType(Types deviceType, String name){
		return ("<" + deviceType.name() + ">;;" + name);
	}
	
	//Parameter local type names
	public static HashMap<String, String> types_de = new HashMap<>();
	public static HashMap<String, String> types_en = new HashMap<>();
	static {
		types_de.put("light", "das Licht");
		types_de.put("heater", "die Heizung");
		types_de.put("tv", "der Fernseher");
		types_de.put("music_player", "die Musikanlage");
		types_de.put("fridge", "der Kühlschrank");
		types_de.put("oven", "der Ofen");
		types_de.put("coffee_maker", "die Kaffeemaschine");
		types_de.put("device", "das Gerät");
		
		types_en.put("light", "the light");
		types_en.put("heater", "the heater");
		types_en.put("tv", "the TV");
		types_en.put("music_player", "the music player");
		types_en.put("fridge", "the fridge");
		types_en.put("oven", "the oven");
		types_en.put("coffee_maker", "the coffee maker");
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
	
	public static final String lightRegEx_en = "light(s|)|lighting|lamp(s|)|illumination|brightness";
	public static final String heaterRegEx_en = "heater(s|)|temperature(s|)|thermostat(s|)";
	public static final String tvRegEx_en = "tv|television";
	public static final String musicPlayerRegEx_en = "(stereo|music)( |)(player)|stereo|bluetooth(-| )(speaker|box)|speaker(s|)";
	public static final String fridgeRegEx_en = "fridge|refrigerator";
	public static final String ovenRegEx_en = "oven|stove";
	public static final String coffeeMakerRegEx_en = "coffee (maker|brewer|machine)";
	
	public static final String lightRegEx_de = "licht(er|es|)|lampe(n|)|beleuchtung|leuchte(n|)|helligkeit";
	public static final String heaterRegEx_de = "heiz(er|ungen|ung|koerper(s|)|luefter(s|)|strahler(s|))|thermostat(es|s|)|temperatur(regler(s|)|en|)";
	public static final String tvRegEx_de = "tv(s|)|television(s|)|fernseh(er(s|)|geraet(es|s|)|apparat(es|s|))";
	public static final String musicPlayerRegEx_de = "(stereo|musi(k|c))( |)(anlage|player(s|))|bluetooth(-| )(lautsprecher(s|)|box)|lautsprecher(s|)|boxen";
	public static final String fridgeRegEx_de = "kuehlschrank(s|)";
	public static final String ovenRegEx_de = "ofen(s|)|herd(es|s)";
	public static final String coffeeMakerRegEx_de = "kaffeemaschine";
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
			type = NluTools.stringFindFirst(input, "("
					+ lightRegEx_de + "|"
					+ heaterRegEx_de + "|"
					+ tvRegEx_de + "|"
					+ musicPlayerRegEx_de + "|"
					+ fridgeRegEx_de + "|"
					+ ovenRegEx_de + "|"
					+ coffeeMakerRegEx_de
				+ ")");
			
		//English and other
		}else{
			type = NluTools.stringFindFirst(input, "("
					+ lightRegEx_en + "|"
					+ heaterRegEx_en + "|"
					+ tvRegEx_en + "|"
					+ musicPlayerRegEx_en + "|"
					+ fridgeRegEx_en + "|"
					+ ovenRegEx_en + "|"
					+ coffeeMakerRegEx_en
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
		
		String deviceTypeTag = null;
		
		if (language.matches(LANGUAGES.DE)){
			if (NluTools.stringContains(device, lightRegEx_de)){
				deviceTypeTag = "<" + Types.light.name() + ">";
				
			}else if (NluTools.stringContains(device, heaterRegEx_de)){
				deviceTypeTag = "<" + Types.heater.name() + ">";
				
			}else if (NluTools.stringContains(device, tvRegEx_de)){
				deviceTypeTag = "<" + Types.tv.name() + ">";
				
			}else if (NluTools.stringContains(device, musicPlayerRegEx_de)){
				deviceTypeTag = "<" + Types.music_player.name() + ">";
				
			}else if (NluTools.stringContains(device, fridgeRegEx_de)){
				deviceTypeTag = "<" + Types.fridge.name() + ">";
				
			}else if (NluTools.stringContains(device, ovenRegEx_de)){
				deviceTypeTag = "<" + Types.oven.name() + ">";
				
			}else if (NluTools.stringContains(device, coffeeMakerRegEx_de)){
				deviceTypeTag = "<" + Types.coffee_maker.name() + ">";
			
			}else{
				deviceTypeTag = "<" + Types.device.name() + ">";
			}
		}else{
			if (NluTools.stringContains(device, lightRegEx_en)){
				deviceTypeTag = "<" + Types.light.name() + ">";
				
			}else if (NluTools.stringContains(device, heaterRegEx_en)){
				deviceTypeTag = "<" + Types.heater.name() + ">";
				
			}else if (NluTools.stringContains(device, tvRegEx_en)){
				deviceTypeTag = "<" + Types.tv.name() + ">";
				
			}else if (NluTools.stringContains(device, musicPlayerRegEx_en)){
				deviceTypeTag = "<" + Types.music_player.name() + ">";
				
			}else if (NluTools.stringContains(device, fridgeRegEx_en)){
				deviceTypeTag = "<" + Types.fridge.name() + ">";
				
			}else if (NluTools.stringContains(device, ovenRegEx_en)){
				deviceTypeTag = "<" + Types.oven.name() + ">";
				
			}else if (NluTools.stringContains(device, coffeeMakerRegEx_en)){
				deviceTypeTag = "<" + Types.coffee_maker.name() + ">";
			
			}else{
				deviceTypeTag = "<" + Types.device.name() + ">";
			}
		}
		return (deviceTypeTag + ";;" + this.found);
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
		//extract again/first? - this should only happen via predefined parameters (e.g. from direct triggers)
		if (Is.notNullOrEmpty(input) && !input.startsWith("<")){
			input = extract(input);
			if (Is.nullOrEmpty(input)){
				return "";
			}
		}
		
		//expects a type!
		String deviceName = "";
		if (input.contains(";;")){
			String[] typeAndName = input.split(";;");
			if (typeAndName.length == 2){
				deviceName = typeAndName[1];
				input = typeAndName[0];
			}else{
				input = typeAndName[0];
			}
		}
		String commonValue = input.replaceAll("^<|>$", "").trim();
		String localValue = getLocal(commonValue, language);
		
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
