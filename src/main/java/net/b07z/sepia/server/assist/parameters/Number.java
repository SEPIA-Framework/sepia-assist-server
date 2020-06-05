package net.b07z.sepia.server.assist.parameters;

import java.util.regex.Pattern;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.CURRENCY;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interpreters.RegexParameterSearch;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Parameter class to find first number in input and classify it into several different types, e.g. plain, percent, weight, length etc.
 * This class is also the basis for variations of NUMBER like smart-device-value (that accepts only a part of the types defined here).
 * 
 * @author Florian Quirin
 *
 */
public class Number implements ParameterHandler{

	//-----data-----
	
	//Parameter types
	public static enum Types{
		plain,
		percent,
		temperature,
		currency,
		timespan,
		weight,
		energy,
		length,
		//volume,
		//area,
		//power,
		//current,
		persons,
		letterend,
		//letterstart,
		//...
		other,
		custom		//this is usually only used in predefined sentences/results (e.g. via Teach-UI)
	}
	//sub-types
	public static final String SUBTYPE_TEMP_F = "F";
	public static final String SUBTYPE_TEMP_C = "C";
	
	public static final String PLAIN_NBR_REGEXP = "(\\-|\\+|\\.|,|)\\d+(\\.|,|)\\d*";
	
	//----------------
	
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
	
	/**
	 * Search normalized string for raw type.
	 */
	public static String getTypeString(String input, String language){
		String type = "";
		//Handle abbreviations
		input = input.replaceAll("\\.", "");
		//German
		if (language.matches(LANGUAGES.DE)){
			type = NluTools.stringFindFirst(input, "(\\s+|)(%|prozent|"
					+ "(°|grad)( |)(celsius|c|fahrenheit|f|)|celsius|fahrenheit|f|"
					+ CURRENCY.TAGS_DE + "|"
					+ "person(en|)|leute|"
					+ "jahr(e|)|monat(e|)|tag(e|)|stunde(n|)|minute(n|)|sekunde(n|)|"
					+ "(kilo|milli|mikro|)gramm|kg|mg|µg|g|tonne(n|)|"
					+ "(kilo|milli|mikro|mega|)joule|kj|mj|µj|j|kcal|"
					+ "(kilo|milli|mikro|zenti|dezi|)meter|km|mm|µm|cm|m"
				+ ")");
			
		//English and other
		}else{
			type = NluTools.stringFindFirst(input, "(\\s+|)(%|percent|"
					+ "(°|degree(s|))( |)(celsius|c|fahrenheit|f|)|celsius|fahrenheit|f|"
					+ CURRENCY.TAGS_EN + "|"
					+ "person(s|)|people|"
					+ "year(s|)|month(s|)|day(s|)|hour(s|)|minute(s|)|second(s|)|"
					+ "(kilo|milli|micro|)gram(me|s)|kg|mg|µg|g|ton(s|)|" 				//TODO: what about pound?
					+ "(kilo|milli|micro|mega|)joule|kj|mj|µj|j|kcal|"
					+ "(kilo|milli|micro|centi|deci|)(metre|meter)(s|)|km|mm|µm|cm|m"
				+ ")");
			
		}
		//System.out.println("Type: " + type); 		//debug
		return type;
	}
	/**
	 * Search extracted type-string for type class and return class.
	 */
	public static String getTypeClass(String input, String language){
		
		if (input.matches(PLAIN_NBR_REGEXP)){		//matches exactly just a number, e.g. 3.14156
			return "<" + Types.plain.name() + ">";
			
		}else if (NluTools.stringContains(input, "%|prozent|percent")){
				return "<" + Types.percent.name() + ">";
			
		}else if (NluTools.stringContains(input, "(\\d|)(°|(grad|degree(s|)))( |)(celsius|c|fahrenheit|f|)|celsius|fahrenheit|f")){
			return "<" + Types.temperature.name() + ">";
			
		}else if (NluTools.stringContains(input, CURRENCY.TAGS_DE + "|" + CURRENCY.TAGS_EN)){
			return "<" + Types.currency.name() + ">";
			
		}else if (NluTools.stringContains(input, "person(s|en|)|people|leute")){
			return "<" + Types.persons.name() + ">";
			
		}else if (NluTools.stringContains(input, "jahr(e|)|monat(e|)|tag(e|)|stunde(n|)|minute(n|)|sekunde(n|)|"
				+ "year(s|)|month(s|)|day(s|)|hour(s|)|minute(s|)|second(s|)")){
			return "<" + Types.timespan.name() + ">";
			
		}else if (NluTools.stringContains(input, "(kilo|milli|mikro|micro|)gram(me|m|s)|kg|mg|µg|g|tonne(n|)|ton(s|)")){
			return "<" + Types.weight.name() + ">";
			
		}else if (NluTools.stringContains(input, "(kilo|milli|mikro|micro|mega|)joule|kj|mj|µj|j|kcal")){
			return "<" + Types.energy.name() + ">";
			
		}else if (NluTools.stringContains(input, "(kilo|milli|mikro|micro|zenti|centi|dezi|deci|)(metre|meter)(s|)|km|mm|µm|cm|m")){
			return "<" + Types.length.name() + ">";
			
		}else if (NluTools.stringContains(input, "(kilo)")){
			return "<" + Types.weight.name() + ">";		//fallback after kilo wasn't used for other types (typical in German)
		
		}else if (input.matches(".*[a-z]$")){
			return "<" + Types.letterend.name() + ">";
			
		}else{
			return "<" + Types.other.name() + ">";
		}
	}
	
	/**
	 * Search input for temperature unit.
	 * @param userInput - normalized full text user input (to find temp. unit)
	 * @param language - code for search language
	 * @return "C", "F" or empty string
	 */
	public static String getTemperatureUnit(String userInput, String language){
		boolean isCelsius = false;
		boolean isFarenheit = false;
		//German
		if (language.matches(LANGUAGES.DE)){
			if (NluTools.stringContains(userInput, "(\\d+|)(°(?!f)|celsius|c)")){
				isCelsius = true;
			}else if (NluTools.stringContains(userInput, "(\\d+|)(°f|fahrenheit|f)")){
				isFarenheit = true;
			}
		//English and other
		}else{
			if (NluTools.stringContains(userInput, "(\\d+|)(°(?!f)|celsius|c)")){
				isCelsius = true;
			}else if (NluTools.stringContains(userInput, "(\\d+|)(°f|fahrenheit|f)")){
				isFarenheit = true;
			}
		}
		if (isCelsius){
			return "C";
		}else if (isFarenheit){
			return "F";
		}else{
			return "";
		}
	}
	
	/**
	 * Convert a number found in user input to a preferred temperature unit (or simply return value as double if no conversion required).
	 * Fails if source unit cannot be identified (either by input or preferred unit).
	 * @param value - temperature number previously extracted (String)
	 * @param givenUnit - unit given e.g. by device or input ("C" or "F"). If not known you can use {@link Number#getTemperatureUnit}.
	 * @param userPrefUnit - preferred user unit ("C" or "F")
	 * @param targetUnit - convert to "C" or "F"
	 * @return temperature as double in target unit. Can throw exception if source or target are unclear.
	 */
	public static double convertTemperature(String value, String givenUnit, String userPrefUnit, String targetUnit) throws Exception {
		boolean isCelsius = false;
		boolean isFarenheit = false;
		double val = Double.parseDouble(value);
		if (givenUnit.equals("C")){
			isCelsius = true;
		}else if (givenUnit.equals("F")){
			isFarenheit = true;
		}
		//use user preference
		if (!isCelsius && !isFarenheit && userPrefUnit != null){
			if (userPrefUnit.equals("C")){
				isCelsius = true;
			}else if (userPrefUnit.equals("F")){
				isFarenheit = true;
			}
		}
		//check current and target
		if (targetUnit.equals("F")){
			if (isFarenheit){
				return val;
			}else if (isCelsius){
				double valF = Math.round(((val * 1.8d + 32.0d))*1000.0d)/1000.0d;
				return valF;
			}
		}else if (targetUnit.equals("C")){
			if (isFarenheit){
				double valC = Math.round(((val - 32.0d)/1.8d)*1000.0d)/1000.0d;
				return valC;
			}else if (isCelsius){
				return val;
			}
		}else{
			throw new RuntimeException("Number.convertTemperature - invalid target unit (must be C or F)!");
		}
		throw new RuntimeException("Number.convertTemperature - result not clear! Missing source unit.");
	}
	
	//-----------------------------------------------
	
	/**
	 * Static version of extract method to be used in other variations of the number parameter.
	 */
	public static String extract(String inputOrg, NluInput nluInput){
		
		String input = RegexParameterSearch.replace_text_number(inputOrg, nluInput.language);
		String number = NluTools.stringFindFirst(input, "(\\W|)" + PLAIN_NBR_REGEXP + "(\\w|\\W|)"); 	
						//the \\W at start is for $5 and the \\w at the end is for street numbers e.g. 3b
		
		if (number.isEmpty()){
			return "";
		}
		//System.out.println("PARAMETER-NUMBER - number (1): " + number); 					//DEBUG
		
		String type = "";
		String relevantTypeSearchString = "";
		
		//ends with number?
		if (number.matches(".*?\\d+$")){
			//we check result + 3 (1 for safety)
			relevantTypeSearchString = NluTools.getStringAndNextWords(input, number, 3); 	//20 degrees celsius, 20 dollar
		}else{
			//we check result + 2 (1 for safety)
			relevantTypeSearchString = NluTools.getStringAndNextWords(input, number, 2);	//20° celsius, 20$
		}
		//System.out.println("PARAMETER-NUMBER - relevantTypeSearchString: " + relevantTypeSearchString); 		//DEBUG
		
		//get type
		type = getTypeString(relevantTypeSearchString, nluInput.language);
		//System.out.println("PARAMETER-NUMBER - type: " + type); 							//DEBUG
		
		//classify into types:
		
		String found = "";
		if (type.trim().isEmpty()){
			found = number.trim();
		}else{
			number = NluTools.stringFindFirst(number, PLAIN_NBR_REGEXP).trim();
			found = NluTools.stringFindFirst(input, Pattern.quote((number + type).trim()) + "|" + Pattern.quote((type + number).trim()));
			if (found.isEmpty()){
				//this can happen when number and type don't belong together, e.g. "light 1 to 50%" will find 1 and % but its not 1%
				found = number;
			}
		}		
		return found;
	}
	
	@Override
	public String extract(String input) {
		String number;
		
		//check storage first
		ParameterResult pr = nluInput.getStoredParameterResult(PARAMETERS.NUMBER);
		if (pr != null){
			number = pr.getExtracted();
			this.found = pr.getFound();
			
			return number;
		}
		
		//remove dates if we found any before
		ParameterResult prDateTime = nluInput.getStoredParameterResult(PARAMETERS.TIME);
		if (prDateTime != null){
			input = ParameterResult.cleanInputOfFoundParameter(nluInput, PARAMETERS.TIME, prDateTime, input);
		}
				
		//get first (remaining) number
		number = extract(input, this.nluInput);
		if (number.trim().isEmpty()){
			return "";
		}
		
		this.found = number;
		//System.out.println("PARAMETER-NUMBER - found: " + this.found);					//DEBUG
		
		//store it
		pr = new ParameterResult(PARAMETERS.NUMBER, number, this.found);
		nluInput.addToParameterResultStorage(pr);
		
		return found;
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
	public String remove(String inputOrg, String found) {
		String input = RegexParameterSearch.replace_text_number(inputOrg, language);
		return NluTools.stringRemoveFirst(input, found);
	}
	
	@Override
	public String responseTweaker(String input){
		return RegexParameterSearch.replace_text_number(input, language);
	}

	@Override
	public String build(String input) {
		//extracted any number?
		if (!NluTools.stringContains(input, ".*\\d.*")){
			return "";
		}
		
		//expects a number including type as string
		String type = getTypeClass(input, language).replaceAll("^<|>$", "").trim();
		String value = input.replaceFirst(".*?(" + PLAIN_NBR_REGEXP + ").*", "$1").trim();
		
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.INPUT, input);
			JSON.add(itemResultJSON, InterviewData.VALUE, value.replaceAll(",", "."));	//default decimal format is "1.00"
			JSON.add(itemResultJSON, InterviewData.NUMBER_TYPE, type);
		
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

}
