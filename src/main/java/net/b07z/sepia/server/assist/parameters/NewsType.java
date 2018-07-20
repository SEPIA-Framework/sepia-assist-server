package net.b07z.sepia.server.assist.parameters;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

public class NewsType implements ParameterHandler{

	//-----data-----
	public static enum NType{
		overview,
		topic,
		results,
		table;
	} 
	
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
		String typeFound = "";
		if (language.equals(LANGUAGES.DE)){
			typeFound = NluTools.stringFindFirst(input, "(\\w*(ergebnis(se|)|resultat(e|)|tabelle|spieltag|spielstand)|gespielt|spielt|spiele|wie steht es|wie stehts|"
					+ "gewonnen|verloren|gespielt|tor (gemacht|geschossen))");
			if (NluTools.stringContains(typeFound, "\\w*(ergebnis(se|)|resultat(e|)|spieltag|spielstand)|gespielt|spielt|spiele|wie steht es|wie stehts|"
					+ "gewonnen|verloren|gespielt|tor (gemacht|geschossen)")){
				typeFound = "<results>";
			}else if (NluTools.stringContains(typeFound, "\\w*(tabelle)")){
				typeFound = "<table>";
			}else{
				typeFound = "";
			}
		}else if (language.equals(LANGUAGES.EN)){
			typeFound = NluTools.stringFindFirst(input, "(result(s|)|played|play|game(s|)|standings|table|score|scored|won|winning|lost|losing)");
			if (NluTools.stringContains(typeFound, "result(s|)|played|play|game(s|)|score|scored|won|winning|lost|losing")){
				typeFound = "<results>";
			}else if (NluTools.stringContains(typeFound, "standings|table")){
				typeFound = "<table>";
			}else{
				typeFound = "";
			}
		}else{
			Debugger.println("NewsType.java - extract() has no language support for '" + language + "'", 3);
			return "";
		}
		//System.out.println("NewsType: " + typeFound); 		//debug
		this.found = typeFound;
		return typeFound;
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
		found = found + "( von | zu | ueber |)";
		return NluTools.stringRemoveFirst(input, found);
	}
	
	@Override
	public String responseTweaker(String input){
		if (language.equals(LANGUAGES.DE)){
			input = input.replaceAll(".*\\b(nach)\\b", "").trim();
			return input;
		}else{
			input = input.replaceAll(".*\\b(for)\\b", "").trim();
			return input;
		}
	}

	@Override
	public String build(String input) {
		
		//expects a news tag!
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
