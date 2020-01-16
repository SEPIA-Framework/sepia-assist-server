package net.b07z.sepia.server.assist.parameters;

import java.util.HashMap;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.RegexParameterSearch;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Basically this is the same as YesNo but sometimes it makes a) sense to have 2 and b) we can treat this specially maybe (like no auto-extraction as optional or so).
 * 
 * @author Florian Quirin
 *
 */
public class Confirm implements ParameterHandler{

	//-----data-----
	public static final String PREFIX = "confirm_"; 		//prefix for special confirmation procedure
	public static final String OK = "ok"; 			//check parameter value against this and ...
	public static final String CANCEL = "cancel"; 	//..this
	
	public static HashMap<String, String> confirm_de = new HashMap<>();
	public static HashMap<String, String> confirm_en = new HashMap<>();
	static {
		confirm_de.put("<ok>", "ok");
		confirm_de.put("<cancel>", "abbrechen");
		
		confirm_en.put("<ok>", "ok");
		confirm_en.put("<cancel>", "cancel");
	}
	/**
	 * Translate generalized value (e.g. &lt;yes&gt;) to local speakable name (e.g. "Ja").
	 * If generalized value is unknown returns empty string.
	 * @param input - generalized value 
	 * @param language - ISO language code
	 */
	public static String getLocal(String input, String language){
		String value = input;
		if (language.equals(LANGUAGES.DE)){
			value = confirm_de.get(value);
		}else if (language.equals(LANGUAGES.EN)){
			value = confirm_en.get(value);
		}
		if (value == null){
			Debugger.println("Confirm.java - getLocal() has no '" + language + "' version for '" + input + "'", 3);
			return "";
		}
		return value;
	}
	//----------------
		
	User user;
	String language;
	boolean buildSuccess = false;
	NluInput nlu_input;
	
	String found = "";
	
	@Override
	public void setup(NluInput nluInput) {
		this.user = nluInput.user;
		this.language = nluInput.language;
		this.nlu_input = nluInput;
	}
	@Override
	public void setup(NluResult nluResult) {
		this.user = nluResult.input.user;
		this.language = nluResult.language;
		this.nlu_input = nluResult.input;
	}
	
	@Override
	public String extract(String input) {
		//TODO: implement found
		String value = RegexParameterSearch.yes_or_no(input, language);
		if (value.equals("yes")){
			return ("<" + "ok" + ">");
		}else if (value.equals("no")){
			return ("<" + "cancel" + ">");
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
		//TODO: implement
		return "";
	}

	@Override
	public String remove(String input, String found) {
		if (language.equals(LANGUAGES.DE)){
			input = input.replaceFirst("\\b()(" + Pattern.quote(found) + ")", "").trim();
		}else{
			input = input.replaceFirst("\\b()(" + Pattern.quote(found) + ")", "").trim();
		}
		return null;
	}
	
	@Override
	public String responseTweaker(String input){
		return input;
	}

	@Override
	public String build(String input) {
		//expects a yes/no tag!
		String commonValue = input.replaceAll("^<|>$", "").trim();
		String localValue = getLocal(input, language);
		
		//is accepted result?
		if (localValue.isEmpty()){
			return "";
		}
		
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.VALUE, commonValue);
			JSON.add(itemResultJSON, InterviewData.VALUE_LOCAL, localValue);
		
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
