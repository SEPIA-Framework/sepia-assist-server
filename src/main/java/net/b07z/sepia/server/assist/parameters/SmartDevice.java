package net.b07z.sepia.server.assist.parameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interpreters.Normalizer;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.smarthome.SmartHomeHub;
import net.b07z.sepia.server.assist.tools.StringCompare;
import net.b07z.sepia.server.assist.tools.StringCompare.StringCompareResult;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
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
	
	private static final long deviceNameScanToolsId = ParameterTools.getNewIdForPerformanceProfiling("SmartDevice#deviceNamesScan");

	//-----data-----
	
	/**
	 * Smart devices typically found in a smart home like lights, heater, tv, music player etc. 
	 */
	public static enum Types {
		light,
		heater,
		air_conditioner,
		temperature_control,
		tv,
		music_player,
		roller_shutter,
		power_outlet,
		sensor,
		fridge,
		oven,
		coffee_maker,
		garage_door,
		fan,
		device,
		//no extract methods:
		other,
		hidden,
		unknown 	//NOTE: this should only be used if instead the device tag/name is known
		//TODO: window, door or use device?
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
		types_de.put("air_conditioner", "die Klimaanlage");
		types_de.put("temperature_control", "die Temperatur");
		types_de.put("tv", "der Fernseher");
		types_de.put("music_player", "die Musikanlage");
		types_de.put("roller_shutter", "der Rollladen");
		types_de.put("power_outlet", "die Steckdose");
		types_de.put("sensor", "der Sensor");
		types_de.put("fridge", "der Kühlschrank");
		types_de.put("oven", "der Ofen");
		types_de.put("coffee_maker", "die Kaffeemaschine");
		types_de.put("garage_door", "das Garagentor");
		types_de.put("fan", "der Lüfter");
		types_de.put("device", "das Gerät");
		types_de.put("other", "");
		types_de.put("hidden", "");
		types_de.put("unknown", "");
		
		types_en.put("light", "the light");
		types_en.put("heater", "the heater");
		types_en.put("air_conditioner", "the air conditioner");
		types_en.put("temperature_control", "the temperature");
		types_en.put("tv", "the TV");
		types_en.put("music_player", "the music player");
		types_en.put("roller_shutter", "the roller shutter");
		types_en.put("power_outlet", "the outlet");
		types_en.put("sensor", "the sensor");
		types_en.put("fridge", "the fridge");
		types_en.put("oven", "the oven");
		types_en.put("coffee_maker", "the coffee maker");
		types_en.put("garage_door", "the garage door");
		types_en.put("fan", "the fan");
		types_en.put("device", "the device");
		types_en.put("other", "");
		types_en.put("hidden", "");
		types_en.put("unknown", "");
	}
	
	/**
	 * Translate generalized value (e.g. &lt;light&gt;) to a context based, useful local name (e.g. das Licht).
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
	
	public static final String sensorRegEx_en = "(electricity|gas|temperature)(-| )(meter|gauge)|"
											+ "(electricity |gas |temperature |)sensor(s|)";
	public static final String lightRegEx_en = "light(s|)|lighting|lamp(s|)|"
											+ "illumination|"
											+ "brightness";
	public static final String heaterRegEx_en = "heat(er(s|)|ing)|radiator(s|)|heat pump(s|)";
	public static final String airConditionerRegEx_en = "air condition(er(s|)|ing)( unit|)";
	public static final String tempControlRegEx_en = "temperature(s|)( control|)|"
											+ "thermostat(s|)|thermometer(s|)|hvac|a/c";
	public static final String tvRegEx_en = "tv|television";
	public static final String musicPlayerRegEx_en = "(stereo|music)( |)(player)|stereo|bluetooth(-| )(speaker|box)|speaker(s|)";
	public static final String rollerShutterRegEx_en = "((roller|window|sun)( |-|)|)(shutter(s|)|blind(s|)|louver(s|))|jalousie(s|)";
	public static final String powerOutletRegEx_en = "((wall|power)( |-|)|)(socket(s|)|outlet(s|))";
	public static final String fridgeRegEx_en = "fridge|refrigerator";
	public static final String ovenRegEx_en = "oven|stove";
	public static final String coffeeMakerRegEx_en = "coffee (maker|brewer|machine)";
	public static final String garageDoorRegEx_en = "garage (door|gate)";
	public static final String fanRegEx_en = "fan|ventilator(s|)|ventilation|venting|blower(s|)";
	public static final String genericDeviceRegEx_en = "device(s|)";
	
	public static final String sensorRegEx_de = "gaszaehler|stromzaehler|"
											+ "(gas( |-|)|strom( |-|)|temperatur( |-|)|)sensor(en|s|)";
	public static final String lightRegEx_de = "\\w*(licht(er|es|)|lampe(n|)|beleuchtung|leuchte(n|))|"
											+ "helligkeit";
	public static final String heaterRegEx_de = "\\w*(heiz(er|ungen|ung|koerper(s|)|luefter(s|)|strahler(s|)))|"
											+ "waerme( |-|)pumpe";
	public static final String airConditionerRegEx_de = "klima(-| |)(anlage(n|)|geraet(es|e|))";
	public static final String tempControlRegEx_de = "\\w*(thermostat(es|s|))|"
											+ "temperatur(regler(s|)|( |)steuerung|en|)|"
											+ "thermometer(s|)";
	public static final String tvRegEx_de = "tv(s|)|television(s|)|fernseh(er(s|)|geraet(es|s|)|apparat(es|s|))";
	public static final String musicPlayerRegEx_de = "(stereo|musi(k|c))( |)(anlage|player(s|))|"
											+ "bluetooth(-| )(lautsprecher(s|)|box)|"
											+ "\\w*(lautsprecher(s|))|"
											+ "\\w*(boxen)";
	public static final String rollerShutterRegEx_de = "(fenster|rol(l|))(l(a|ae)den)|jalousie(n|)|rollo(s|)|markise";
	public static final String powerOutletRegEx_de = "(steck|strom)( |-|)dose(n|)|stromanschluss(es|)";
	public static final String fridgeRegEx_de = "kuehlschrank(s|)";
	public static final String ovenRegEx_de = "ofen(s|)|herd(es|s)";
	public static final String coffeeMakerRegEx_de = "kaffeemaschine";
	public static final String garageDoorRegEx_de = "garagen( |-|)(tor|tuer)";
	public static final String fanRegEx_de = "\\w*(luefter|ventilator(en|)|geblaese|(be|)lueftung)";
	public static final String genericDeviceRegEx_de = "geraet(e|)";
	//----------------
	
	User user;
	String language;
	NluInput nluInput;
	boolean buildSuccess = false;
	
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
	
	/**
	 * Search normalized string for raw type.
	 */
	public static String getType(String input, String language){
		String type = "";
		//German
		if (language.matches(LANGUAGES.DE)){
			type = NluTools.stringFindFirst(input, "("
					+ sensorRegEx_de + "|"	//NOTE: we put this first to catch sensors like "temperature sensor"
					+ heaterRegEx_de + "|"	//NOTE: we put this before temp. control to catch ...
					+ airConditionerRegEx_de	//... things like "temperature of the heater"
				+ ")");
			if (type.isEmpty()){
				type = NluTools.stringFindFirst(input, "("
						+ lightRegEx_de + "|"
						+ tempControlRegEx_de + "|"
						+ tvRegEx_de + "|"
						+ musicPlayerRegEx_de + "|"
						+ rollerShutterRegEx_de + "|"
						+ powerOutletRegEx_de + "|"
						+ fridgeRegEx_de + "|"
						+ ovenRegEx_de + "|"
						+ coffeeMakerRegEx_de + "|"
						+ garageDoorRegEx_de + "|"
						+ fanRegEx_de + "|"
						+ genericDeviceRegEx_de
					+ ")");
			}
			
		//English and other
		}else{
			type = NluTools.stringFindFirst(input, "("
					+ sensorRegEx_en + "|"
					+ lightRegEx_en + "|"
					+ heaterRegEx_en
				+ ")");
			if (type.isEmpty()){
				type = NluTools.stringFindFirst(input, "("
						+ lightRegEx_en + "|"
						+ tempControlRegEx_en + "|"
						+ tvRegEx_en + "|"
						+ musicPlayerRegEx_en + "|"
						+ rollerShutterRegEx_en + "|"
						+ powerOutletRegEx_en + "|"
						+ fridgeRegEx_en + "|"
						+ ovenRegEx_en + "|"
						+ coffeeMakerRegEx_en + "|"
						+ garageDoorRegEx_en + "|"
						+ fanRegEx_en + "|"
						+ genericDeviceRegEx_en
					+ ")");
			}
			
		}
		//System.out.println("Type: " + type); 		//debug
		return type;
	}

	@Override
	public String extract(String input) {
		String typeAndTag;
		
		//check storage first
		ParameterResult pr = nluInput.getStoredParameterResult(PARAMETERS.SMART_DEVICE);
		if (pr != null){
			typeAndTag = pr.getExtracted();
			this.found = pr.getFound();
			
			return typeAndTag;
		}
				
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
			this.found = device;
			//System.out.println("found 1: " + this.found); 		//DEBUG
		}
		
		//classify into generalized types:
		
		final String deviceType;
		
		if (language.matches(LANGUAGES.DE)){
			if (NluTools.stringContains(device, sensorRegEx_de)){
				deviceType = Types.sensor.name();
				
			}else if (NluTools.stringContains(device, lightRegEx_de)){
				deviceType = Types.light.name();
				
			}else if (NluTools.stringContains(device, heaterRegEx_de)){
				deviceType = Types.heater.name();
				
			}else if (NluTools.stringContains(device, airConditionerRegEx_de)){
				deviceType = Types.air_conditioner.name();
				
			}else if (NluTools.stringContains(device, tempControlRegEx_de)){
				deviceType = Types.temperature_control.name();
				
			}else if (NluTools.stringContains(device, tvRegEx_de)){
				deviceType = Types.tv.name();
				
			}else if (NluTools.stringContains(device, musicPlayerRegEx_de)){
				deviceType = Types.music_player.name();
				
			}else if (NluTools.stringContains(device, rollerShutterRegEx_de)){
				deviceType = Types.roller_shutter.name();
				
			}else if (NluTools.stringContains(device, powerOutletRegEx_de)){
				deviceType = Types.power_outlet.name();
				
			}else if (NluTools.stringContains(device, fridgeRegEx_de)){
				deviceType = Types.fridge.name();
				
			}else if (NluTools.stringContains(device, ovenRegEx_de)){
				deviceType = Types.oven.name();
				
			}else if (NluTools.stringContains(device, coffeeMakerRegEx_de)){
				deviceType = Types.coffee_maker.name();
			
			}else if (NluTools.stringContains(device, garageDoorRegEx_de)){
				deviceType = Types.garage_door.name();
			
			}else if (NluTools.stringContains(device, fanRegEx_de)){
				deviceType = Types.fan.name();
			
			}else{
				deviceType = Types.device.name();
			}
		}else{
			if (NluTools.stringContains(device, sensorRegEx_en)){
				deviceType = Types.sensor.name();
				
			}else if (NluTools.stringContains(device, lightRegEx_en)){
				deviceType = Types.light.name();
				
			}else if (NluTools.stringContains(device, heaterRegEx_en)){
				deviceType = Types.heater.name();
				
			}else if (NluTools.stringContains(device, airConditionerRegEx_en)){
				deviceType = Types.air_conditioner.name();
				
			}else if (NluTools.stringContains(device, tempControlRegEx_en)){
				deviceType = Types.temperature_control.name();
				
			}else if (NluTools.stringContains(device, tvRegEx_en)){
				deviceType = Types.tv.name();
				
			}else if (NluTools.stringContains(device, musicPlayerRegEx_en)){
				deviceType = Types.music_player.name();
				
			}else if (NluTools.stringContains(device, rollerShutterRegEx_en)){
				deviceType = Types.roller_shutter.name();
				
			}else if (NluTools.stringContains(device, powerOutletRegEx_en)){
				deviceType = Types.power_outlet.name();
				
			}else if (NluTools.stringContains(device, fridgeRegEx_en)){
				deviceType = Types.fridge.name();
				
			}else if (NluTools.stringContains(device, ovenRegEx_en)){
				deviceType = Types.oven.name();
				
			}else if (NluTools.stringContains(device, coffeeMakerRegEx_en)){
				deviceType = Types.coffee_maker.name();
				
			}else if (NluTools.stringContains(device, garageDoorRegEx_en)){
				deviceType = Types.garage_door.name();
				
			}else if (NluTools.stringContains(device, fanRegEx_en)){
				deviceType = Types.fan.name();
				
			}else{
				deviceType = Types.device.name();
			}
		}
		
		//can we improve the result with known smart device names for the found type?
		SmartHomeHub smartHomeHUB = SmartHomeHub.getHubFromSeverConfig(); 
		String[] deviceNameMatchResult = null;
		String deviceNameMatch = null;
		if (smartHomeHUB != null){
			//make sure this procedure does not seriously influence NLU chain performance: 
			deviceNameMatchResult = findMatchInKnownDeviceNamesFastOrSkip(input, deviceType, smartHomeHUB, this.language);
			if (deviceNameMatchResult != null && deviceNameMatchResult.length == 2){
				//remember match
				if (Is.notNullOrEmpty(deviceNameMatchResult[0])){
					deviceNameMatch = deviceNameMatchResult[0];
				}
				//can we combine the found name with previous result?
				if (!deviceNameMatchResult[1].contains(device)){	//NOTE: this is the norm. 'name' because 'device' is norm.
					if (input.contains(deviceNameMatchResult[1] + " " + device)){
						this.found = deviceNameMatchResult[1] + " " + device;
					}else if (input.contains(device + " " + deviceNameMatchResult[1])){
						this.found = device + " " + deviceNameMatchResult[1];
					}else{
						this.found = deviceNameMatchResult[1];
					}
				}else{
					this.found = deviceNameMatchResult[1];
				}
				//System.out.println("found 2: " + this.found); 		//DEBUG
			}
		}
		
		//we should take device names (tag/number..) into account like "Lamp 1", "Light A" etc.
		String deviceWithNumber;
		if (language.matches(LANGUAGES.DE)){
			deviceWithNumber = NluTools.stringFindFirst(input, Pattern.quote(this.found) + "( (mit der |mit |)nummer|) \\d+");
		}else{
			deviceWithNumber = NluTools.stringFindFirst(input, Pattern.quote(this.found) + "( (with the |with |)number|) \\d+");
		}
		if (!deviceWithNumber.isEmpty()){
			//String deviceNumber = deviceWithNumber.replaceFirst(".* (\\d+)", "$1")
			this.found = deviceWithNumber;
			//System.out.println("found 3: " + this.found); 		//DEBUG
		}
		
		//reconstruct original phrase to get proper item names
		Normalizer normalizer = Config.inputNormalizers.get(this.language);
		String fullFoundTag = normalizer.reconstructPhrase(nluInput.textRaw, this.found);
		
		if (deviceNameMatch != null){
			typeAndTag = "<" + deviceType + ">;;" + fullFoundTag + ";;" + deviceNameMatch;
		}else{
			typeAndTag = "<" + deviceType + ">;;" + fullFoundTag;
		}
		
		//store it
		pr = new ParameterResult(PARAMETERS.SMART_DEVICE, typeAndTag, this.found);
		nluInput.addToParameterResultStorage(pr);
		
		return typeAndTag;
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
	public String remove(String input, String found){
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
			input = input.replaceAll(".*\\b(einen|einem|einer|eine|ein|der|die|das|den|dem|des|ne|ner|meine(r|s|m|))\\b", "").trim();
		}else{
			input = input.replaceAll(".*\\b(a|the|my)\\b", "").trim();
		}
		return input;
	}

	@Override
	public String build(String input){
		//extract again/first? - this should only happen via predefined parameters (e.g. from direct triggers) OR as response (via responseTweaker)
		if (Is.notNullOrEmpty(input) && !input.startsWith("<")){
			String exIn = extract(input);
			if (Is.nullOrEmpty(exIn)){
				//final try is to look for device name in all (cached) names
				SmartHomeHub smartHomeHUB = SmartHomeHub.getHubFromSeverConfig();
				String[] deviceNameMatchResult = findMatchInKnownDeviceNamesFastOrSkip(input, null, smartHomeHUB, this.language);
				if (deviceNameMatchResult != null && deviceNameMatchResult.length == 2){
					input = "<" + Types.unknown.name() + ">;;" + deviceNameMatchResult[1] + ";;" + deviceNameMatchResult[0];
				}else{
					return "";
				}
			}else{
				input = exIn;
			}
		}
		
		//expects a type!
		String deviceNameFound = "";
		String deviceNameFoundClean = null;
		String deviceIndexStr = "";
		if (input.contains(";;")){
			String[] typeAndName = input.split(";;");
			if (typeAndName.length == 3){
				deviceNameFound = typeAndName[1];
				deviceNameFoundClean = typeAndName[2];
				deviceIndexStr = NluTools.stringFindFirst(deviceNameFound, "\\b\\d+\\b");
				input = typeAndName[0];
			}else if (typeAndName.length == 2){
				deviceNameFound = typeAndName[1];
				deviceIndexStr = NluTools.stringFindFirst(deviceNameFound, "\\b\\d+\\b");
				input = typeAndName[0];
			}else{
				input = typeAndName[0];
			}
		}
		String commonValue = input.replaceAll("^<|>$", "").trim();
		String localValue = getLocal(commonValue, language);
		
		if (deviceNameFoundClean == null){
			if (language.matches(LANGUAGES.DE)){
				deviceNameFoundClean = deviceNameFound.replaceFirst("(?i)( (mit der |mit |)nummer|) \\d+", "").trim();
			}else{
				deviceNameFoundClean = deviceNameFound.replaceFirst("(?i)( (with the |with |)number|) \\d+", "").trim();
			}
		}
		
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.VALUE, commonValue);
			JSON.add(itemResultJSON, InterviewData.VALUE_LOCAL, localValue);
			JSON.add(itemResultJSON, InterviewData.SMART_DEVICE_TAG, deviceNameFoundClean);
			JSON.add(itemResultJSON, InterviewData.FOUND, deviceNameFound);
		//add device index
		if (!deviceIndexStr.isEmpty()){
			int deviceIndex = Integer.parseInt(deviceIndexStr);
			JSON.add(itemResultJSON, InterviewData.ITEM_INDEX, deviceIndex);
		}
		
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

	@Override
	public boolean buildSuccess() {
		return buildSuccess;
	}
	
	//------------------------ helpers ------------------------
	
	/**
	 * Find a match for the input by comparing it to all known (cached) smart device names.<br>
	 * NOTE: Since this method is performance critical it can occasionally been skipped when taking too long (>250ms).
	 * @param input - sentence or string to search for device name
	 * @param deviceType - known device type filter or null (= all types)
	 * @param smartHomeHUB - HUB that holds the device name info
	 * @param language - service language code
	 * @return String array with {bestMatch, bestMatchNorm}
	 */
	public static String[] findMatchInKnownDeviceNamesFastOrSkip(String input, String deviceType, SmartHomeHub smartHomeHUB, String language){
		long thresholdTime = 250; 		//this method has to be super fast!
		String[] deviceNameMatchResult = ParameterTools.runOrSkipPerformanceCriticalMethod(deviceNameScanToolsId, thresholdTime, (in) -> {
			Map<String, Set<String>> deviceNamesByType = smartHomeHUB.getBufferedDeviceNamesByType();
			if (deviceNamesByType != null){
				Set<String> deviceNames;
				if (Is.nullOrEmpty(deviceType)){
					deviceNames = new HashSet<>();
					deviceNamesByType.values().forEach(set -> {
						deviceNames.addAll(set);
					});
				}else{
					deviceNames = deviceNamesByType.get(deviceType);
				}
				if (Is.notNullOrEmpty(deviceNames)){
					//System.out.println("input: " + input); 					//DEBUG
					//System.out.println("possible tags: " + deviceNames); 		//DEBUG
					StringCompareResult scr = StringCompare.scanSentenceForBestPhraseMatch(
							input, new ArrayList<>(deviceNames), language
					);
					int bestScore = scr.getResultPercent();
					//System.out.println("bestScore: " + bestScore); 				//DEBUG
					//System.out.println("bestMatch: " + scr.getResultString()); 	//DEBUG
					if (bestScore >= 100){			//allow more "fuzziness" ?? - NOTE: we can't do this unless we fix smart device value too
						String bestMatch = scr.getResultString();
						String bestMatchNorm = scr.getResultStringNormalized();
						//System.out.println("Best tag: " + bestMatch); 		//DEBUG
						return new String[]{bestMatch, bestMatchNorm};
					}
				}
				return new String[]{};		//return empty to prevent error
			}else{
				return null;				//return null to indicate error
			}
		}, null);
		return deviceNameMatchResult;
	}

}
