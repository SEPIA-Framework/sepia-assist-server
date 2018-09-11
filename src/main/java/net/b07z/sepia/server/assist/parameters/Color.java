package net.b07z.sepia.server.assist.parameters;

import java.util.HashMap;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interpreters.RegexParameterSearch;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

public class Color implements ParameterHandler{

	//-----data-----
	public static HashMap<String, String> colors_de = new HashMap<>();
	public static HashMap<String, String> colors_en = new HashMap<>();
	static {
		colors_de.put("<blue>", "blau");
		colors_de.put("<white>", "weiß");
		colors_de.put("<red>", "rot");
		colors_de.put("<black>", "schwarz");
		colors_de.put("<yellow>", "gelb");
		colors_de.put("<green>", "grün");
		colors_de.put("<orange>", "orange");
		colors_de.put("<purple>", "lila");
		colors_de.put("<violet>", "violett");
		colors_de.put("<magenta>", "magenta");
		colors_de.put("<cyan>", "cyan");
		colors_de.put("<silver>", "silber");
		colors_de.put("<gold>", "gold");
		colors_de.put("<pink>", "rosa");
		colors_de.put("<brown>", "braun");
		
		colors_en.put("<blue>", "blue");
		colors_en.put("<white>", "white");
		colors_en.put("<red>", "red");
		colors_en.put("<black>", "black");
		colors_en.put("<yellow>", "yellow");
		colors_en.put("<green>", "green");
		colors_en.put("<orange>", "orange");
		colors_en.put("<purple>", "purple");
		colors_en.put("<violet>", "violet");
		colors_en.put("<magenta>", "magenta");
		colors_en.put("<cyan>", "cyan");
		colors_en.put("<silver>", "silver");
		colors_en.put("<gold>", "gold");
		colors_en.put("<pink>", "pink");
		colors_en.put("<brown>", "brown");
				
		//TODO: add more
	}
	/**
	 * Translate generalized color value (e.g. &ltblue&gt) to local name (e.g. blau).
	 * If generalized value is unknown returns empty string
	 * @param colorValue - generalized value 
	 * @param language - ISO language code
	 */
	public static String getLocal(String colorValue, String language){
		String localName = "";
		if (language.equals(LANGUAGES.DE)){
			localName = colors_de.get(colorValue);
		}else if (language.equals(LANGUAGES.EN)){
			localName = colors_en.get(colorValue);
		}
		if (localName == null){
			Debugger.println("Color.java - getLocal() has no '" + language + "' version for '" + colorValue + "'", 3);
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

	@Override
	public String extract(String input) {
		String colorFound = RegexParameterSearch.get_color(input, language);	//result e.g.: "<blue> blaue"
		String colorTag = colorFound.replaceFirst("\\s.*", "").trim();
		this.found = colorFound.replaceFirst(".*?\\s", "").trim();
		return colorTag;
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
		found = "(in |)" + found;
		return NluTools.stringRemoveFirst(input, found);
	}
	
	@Override
	public String responseTweaker(String input){
		return input.replaceAll(".*\\b(in)\\b", "").trim();
	}

	@Override
	public String build(String input) {
		//is accepted result?
		String inputLocal = getLocal(input, language);
		if (inputLocal.isEmpty()){
			return "";
		}
		
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.VALUE, input);
			//TODO: should use GENERALIZED
			JSON.add(itemResultJSON, InterviewData.VALUE_LOCAL, inputLocal);
			//TODO: add ACCOUNT here?
		
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
