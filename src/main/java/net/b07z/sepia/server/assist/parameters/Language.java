package net.b07z.sepia.server.assist.parameters;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.tools.JSON;

public class Language implements Parameter_Handler{

	//-----data-----
	public static final String languageClasses_de = 
			"(deutsch|englisch|britisch|irisch|italienisch|chinesisch|griechisch|portugiesisch|spanisch|tuerkisch|indisch|thai|thailaendisch|franzoesisch|"
			+ "russisch|vietnamesisch|afrikanisch|hollaendisch|niederlaendisch|belgisch|daenisch|polnisch|kroatisch|"
			+ "japanisch|mongolisch|mexikanisch)(e|es|)";
	public static final String languageClasses_en = 
			"(german|english|brtish|irish|italian|chinese|greek|portuguese|spanish|turkish|indian|thai|french|"
			+ "russian|vietnamese|african|dutch|belgian|danish|polish|croatian|"
			+ "japanese|mongolian|mexican)";
	/*
	public static HashMap<String, String> colors_de = new HashMap<>();
	public static HashMap<String, String> colors_en = new HashMap<>();
	static {
		colors_de.put("<blue>", "blau");
				
		colors_en.put("<blue>", "blue");
	}
	*/
	/**
	 * Translate generalized news value (e.g. &ltscience&gt) to local name (e.g. Wissenschaft).
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
		String lang = "";
		if (language.equals(LANGUAGES.DE)){
			lang = NluTools.stringFindFirst(input, languageClasses_de);
		}else{
			lang = NluTools.stringFindFirst(input, languageClasses_en);
		}
		this.found = lang;
		return lang;
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
			found = "\\b(in |auf |nach |ins |in das |)\\b" + found;
		}else{
			found = "\\b(in |)\\b" + found;
		}
		return NluTools.stringRemoveFirst(input, found);
	}
	
	@Override
	public String responseTweaker(String input){
		if (language.equals(LANGUAGES.DE)){
			input = input.replaceAll(".*\\b(nach|in|auf|ins|in das)\\b", "").trim();
			return input;
		}else{
			input = input.replaceAll(".*\\b(to|in|into)\\b", "").trim();
			return input.replaceAll(".*\\b(the)\\b", "").trim();
		}
	}

	@Override
	public String build(String input) {
		//expects a color tag!
		String commonValue = input.replaceAll("^<|>$", "").trim();
		String localValue = getLocal(input, language);
		
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.VALUE, commonValue);
			JSON.add(itemResultJSON, InterviewData.VALUE_LOCAL, localValue);
			JSON.add(itemResultJSON, InterviewData.GENERALIZED, "??");			//TODO: add abstract value, e.g. language code ISO-xy 		
		
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
