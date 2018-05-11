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
import net.b07z.sepia.server.core.tools.JSON;

public class SearchSection implements Parameter_Handler{

	//-----data-----
	public static HashMap<String, String> sections_de = new HashMap<>();
	public static HashMap<String, String> sections_en = new HashMap<>();
	static {
		sections_de.put("<pictures>", "Bilder");
		sections_de.put("<recipes>", "Rezepte");
		sections_de.put("<videos>", "Videos");
		sections_de.put("<movies>", "Filme");
		sections_de.put("<shares>", "Aktien");
		sections_de.put("<books>", "Bücher");
				
		sections_en.put("<pictures>", "pictures");
		sections_en.put("<recipes>", "recipes");
		sections_en.put("<videos>", "videos");
		sections_en.put("<movies>", "movies");
		sections_en.put("<shares>", "shares");
		sections_en.put("<books>", "books");
	}
	/**
	 * Translate generalized value (e.g. &ltscience&gt) to local name (e.g. Wissenschaft).
	 * If generalized value is unknown returns empty string
	 * @param value - generalized value 
	 * @param language - ISO language code
	 */
	public static String getLocal(String value, String language){
		String localName = "";
		if (language.equals(LANGUAGES.DE)){
			localName = sections_de.get(value);
		}else if (language.equals(LANGUAGES.EN)){
			localName = sections_en.get(value);
		}
		if (localName == null){
			Debugger.println("SerachSection.java - getLocal() has no '" + language + "' version for '" + value + "'", 3);
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
	
	/**
	 * Search string for news type.
	 */
	public static String getSearchSection(String input, String language){
		String type = "";
		//German
		if (language.matches(LANGUAGES.DE)){
			type = NluTools.stringFindFirst(input, "bild(ern|er|)|rezept(en|e|)|video(s|)|movie(s|)|film(en|e|)|aktie(n|)|aktien(kurs|wert)|buecher(n|)|buch");
			
		//English and other
		}else{
			type = NluTools.stringFindFirst(input, "picture(s|)|recipe(s|)|video(s|)|movie(s|)|film(s|)|share(s|)|stock(s|)|book(s|)");			
		}
		//System.out.println("searchType: " + type); 		//debug
		return type;
	}

	@Override
	public String extract(String input) {
		String valueFound = getSearchSection(input, language);
		this.found = valueFound;
		if (NluTools.stringContains(valueFound, "bild(ern|er|)|picture(s|)")){
			return "<pictures>";
		}else if (NluTools.stringContains(valueFound, "rezept(en|e|)|recipe(s|)")){
			return "<recipes>";
		}else if (NluTools.stringContains(valueFound, "video(s|)")){
			return "<videos>";
		}else if (NluTools.stringContains(valueFound, "movie(s|)|film(s|)|film(en|e|)")){
			return "<movies>";
		}else if (NluTools.stringContains(valueFound, "aktie(n|)|aktien(kurs|wert)|share(s|)|stock(s|)")){
			return "<shares>";
		}else if (NluTools.stringContains(valueFound, "buecher(n|)|buch|book(s|)")){
			return "<books>";
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
		return found;
	}

	@Override
	public String remove(String input, String found) {
		if (language.equals(LANGUAGES.DE)){
			found = found + "( von| zu| ueber|)( der| die| das|)";
		}else{
			found = found + "( of| about| for|)( a| the|)";
		}
		return NluTools.stringRemoveFirst(input, found);
	}
	
	@Override
	public String responseTweaker(String input){
		if (language.equals(LANGUAGES.DE)){
			input = input.replaceAll(".*\\b(von|zu|ueber|nach)\\b", "").trim();
			return input.replaceAll(".*\\b(einen|einem|einer|eine|ein|der|die|das|den|ne|ner)\\b", "").trim();
		}else{
			input = input.replaceAll(".*\\b(of|about|for)\\b", "").trim();
			return input.replaceAll(".*\\b(a|the)\\b", "").trim();
		}
	}

	@Override
	public String build(String input) {
		//TODO: check for my favorite color?
		
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
