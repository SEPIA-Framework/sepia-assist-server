package net.b07z.sepia.server.assist.parameters;

import java.util.regex.Pattern;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.CURRENCY;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
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
		letterend,
		//letterstart,
		//...
		other;
	}
	
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
		//German
		if (language.matches(LANGUAGES.DE)){
			type = NluTools.stringFindFirst(input, "(\\s+|)(%|prozent|"
					+ "(°|grad)( |)(celsius|c|fahrenheit|f|)|celsius|fahrenheit|f|"
					+ CURRENCY.TAGS_DE + "|"
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
		
		if (input.matches(PLAIN_NBR_REGEXP)){
			return "<" + Types.plain.name() + ">";
			
		}else if (NluTools.stringContains(input, "%|prozent|percent")){
				return "<" + Types.percent.name() + ">";
			
		}else if (NluTools.stringContains(input, "(°|(grad|degree(s|)))( |)(celsius|c|fahrenheit|f|)|celsius|fahrenheit|f")){
			return "<" + Types.temperature.name() + ">";
			
		}else if (NluTools.stringContains(input, CURRENCY.TAGS_DE + "|" + CURRENCY.TAGS_EN)){
			return "<" + Types.currency.name() + ">";
			
		}else if (NluTools.stringContains(input, "jahr(e|)|monat(e|)|tag(e|)|stunde(n|)|minute(n|)|sekunde(n|)|"
				+ "year(s|)|month(s|)|day(s|)|hour(s|)|minute(s|)|second(s|)")){
			return "<" + Types.timespan.name() + ">";
			
		}else if (NluTools.stringContains(input, "(kilo|milli|mikro|micro|)gram(me|m|s)|kg|mg|µg|g|tonne(n|)|ton(s|)")){
			return "<" + Types.weight.name() + ">";
			
		}else if (NluTools.stringContains(input, "(kilo|milli|mikro|micro|mega|)joule|kj|mj|µj|j|kcal")){
			return "<" + Types.energy.name() + ">";
			
		}else if (NluTools.stringContains(input, "(kilo|milli|mikro|micro|zenti|centi|dezi|deci|)(metre|meter)(s|)|km|mm|µm|cm|m")){
			return "<" + Types.length.name() + ">";
		
		}else if (input.matches(".*[a-z]$")){
			return "<" + Types.letterend.name() + ">";
			
		}else{
			return "<" + Types.other.name() + ">";
		}
	}
	
	/**
	 * Static version of extract method to be used in other variations of the number parameter.
	 */
	public static String extract(String input, NluInput nluInput){
		
		String number = NluTools.stringFindFirst(input, "(\\W|)" + PLAIN_NBR_REGEXP + "(\\w|\\W|)"); 	
						//the \\W at start is for $5 and the \\w at the end is for street numbers e.g. 3b
		
		if (number.isEmpty()){
			return "";
		}
		//System.out.println("PARAMETER-NUMBER - number: " + number); 							//DEBUG
		
		String type = "";
		String relevantTypeSearchString = "";
		
		//ends with number?
		if (number.matches(".*?\\d+$")){
			relevantTypeSearchString = NluTools.getStringAndNextWords(input, number, 3);
		}else{
			relevantTypeSearchString = NluTools.getStringAndNextWords(input, number, 2);
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
				
		number = extract(input, this.nluInput);
		if (number.trim().isEmpty()){
			return "";
		}
		
		this.found = number;
		//System.out.println("PARAMETER-NUMBER - found: " + this.found);					//DEBUG
		
		//store it
		pr = new ParameterResult(PARAMETERS.NUMBER, found, this.found);
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
		String type = getTypeClass(input, language).replaceAll("^<|>$", "").trim();
		String value = input.replaceFirst(".*?(" + PLAIN_NBR_REGEXP + ").*", "$1").trim();
		
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
