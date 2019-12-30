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

public class YesNo implements ParameterHandler{

	//-----data-----
	public static HashMap<String, String> yesNo_de = new HashMap<>();
	public static HashMap<String, String> yesNo_en = new HashMap<>();
	static {
		yesNo_de.put("<yes>", "Ja");
		yesNo_de.put("yes", "Ja");
		yesNo_de.put("<no>", "Nein");
		yesNo_de.put("no", "Nein");
		
		yesNo_en.put("<yes>", "yes");
		yesNo_en.put("yes", "yes");
		yesNo_en.put("<no>", "no");
		yesNo_en.put("no", "no");
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
			value = yesNo_de.get(value);
		}else if (language.equals(LANGUAGES.EN)){
			value = yesNo_en.get(value);
		}
		if (value == null){
			Debugger.println("YesNo.java - getLocal() has no '" + language + "' version for '" + input + "'", 3);
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
		if (value.equals("yes") || value.equals("no")){		//old version: if (!value.isEmpty()){
			return ("<" + value + ">");
		}else{
			return value;
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
