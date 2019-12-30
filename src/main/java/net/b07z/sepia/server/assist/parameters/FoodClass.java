package net.b07z.sepia.server.assist.parameters;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.tools.JSON;

public class FoodClass implements ParameterHandler{

	//-----data-----
	public static final String foodClasses_de = 
			"(vegetarisch|vegan)(e|es|)";
	public static final String foodClasses_en = 
			"(vegetarian|vegan)";
	/*
	public static HashMap<String, String> colors_de = new HashMap<>();
	public static HashMap<String, String> colors_en = new HashMap<>();
	static {
		colors_de.put("<blue>", "blau");
				
		colors_en.put("<blue>", "blue");
	}
	*/
	/**
	 * Translate generalized news value (e.g. &lt;science&gt;) to local name (e.g. Wissenschaft).
	 * If generalized value is unknown returns empty string
	 * @param newsValue - generalized value 
	 * @param language - ISO language code
	 */
	public static String getLocal(String newsValue, String language){
		String localName = "";
		/*
		if (language.equals(LANGUAGES.DE)){
			localName = colors_de.get(colorValue);
		}else if (language.equals(LANGUAGES.EN)){
			localName = colors_en.get(colorValue);
		}
		if (localName == null){
			Debugger.println("Color.java - getLocal() has no '" + language + "' version for '" + colorValue + "'", 3);
			return "";
		}
		*/
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

	@Override
	public String extract(String input) {
		//TODO: fix this for language and more cases
		String foodClass = "";
		String languageClass = "";
		if (language.equals(LANGUAGES.DE)){
			foodClass = NluTools.stringFindFirst(input, foodClasses_de);
			languageClass = NluTools.stringFindFirst(input, Language.languageClasses_de);
			this.found = foodClass;						//TODO: FIX THIS - MAYBE MAKE found and ARRAY!
		}else{
			foodClass = NluTools.stringFindFirst(input, foodClasses_en);
			languageClass = NluTools.stringFindFirst(input, Language.languageClasses_en);
			this.found = foodClass;						//TODO: FIX THIS - MAYBE MAKE found and ARRAY!
		}
		return (foodClass + " " + languageClass).trim();
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
			found = "\\b(nach |auf |)(einen|einem|einer|eine|ein|der|die|das|den|ne|ner|)\\b" + found;
		}else{
			found = "\\b(for |)(a|the|)\\b" + found;
		}
		return NluTools.stringRemoveFirst(input, found);
	}
	
	@Override
	public String responseTweaker(String input){
		if (language.equals(LANGUAGES.DE)){
			input = input.replaceAll(".*\\b(nach)\\b", "").trim();
			return input.replaceAll(".*\\b(einen|einem|einer|eine|ein|der|die|das|den|ne|ner)\\b", "").trim();
		}else{
			input = input.replaceAll(".*\\b(for)\\b", "").trim();
			return input.replaceAll(".*\\b(a|the)\\b", "").trim();
		}
	}

	@Override
	public String build(String input) {
		//TODO: check for my favorite food?
		
		//expects a color tag!
		String commonValue = input.replaceAll("^<|>$", "").trim();
		String localValue = getLocal(input, language);
		
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
