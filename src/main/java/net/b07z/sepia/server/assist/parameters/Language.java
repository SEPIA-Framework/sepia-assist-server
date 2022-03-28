package net.b07z.sepia.server.assist.parameters;

import java.util.Locale;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.tools.JSON;

public class Language implements ParameterHandler{

	//-----data-----
	public static final String languageClasses_de =	"("
				+ "oesterreichisch|"
				+ "schweizer|"
				+ "deutsch|"
				+ "britisch|"
				+ "amerikanisch|"
				+ "irisch|"
				+ "schottisch|"
				+ "australisch|"
				+ "englisch|"
				+ "italienisch|"
				+ "chinesisch|"
				+ "griechisch|"
				+ "brasilianisch|"
				+ "portugiesisch|"
				+ "mexikanisch|"
				+ "spanisch|"
				+ "tuerkisch|"
				+ "indisch|"
				+ "thai|"
				+ "thailaendisch|"
				+ "kanadisch|"
				+ "franzoesisch|"
				+ "russisch|"
				+ "vietnamesisch|"
				+ "afrikanisch|"
				+ "hollaendisch|niederlaendisch|"
				+ "belgisch|"
				+ "daenisch|"
				+ "schwedisch|"
				+ "polnisch|"
				+ "kroatisch|"
				+ "japanisch|"
				+ "koreanisch|"
				+ "mongolisch"
			+ ")(e|es|er|en|)";
	public static final String languageClasses_en =	"("
				+ "austrian|"
				+ "swiss|" 		//TODO: could be French swiss etc. ... the order here might matter
				+ "german|"
				+ "british|"
				+ "american|"
				+ "irish|"
				+ "scottish|scotch|"
				+ "australian|"
				+ "english|"
				+ "italian|"
				+ "chinese|"
				+ "greek|"
				+ "brasilian|"
				+ "portuguese|"
				+ "mexican|"
				+ "spanish|"
				+ "turkish|"
				+ "indian|"
				+ "thai|"
				+ "canadian|"
				+ "french|"
				+ "russian|"
				+ "vietnamese|"
				+ "african|"
				+ "belgian|"
				+ "dutch|"
				+ "danish|"
				+ "swedish|"
				+ "polish|"
				+ "croatian|"
				+ "japanese|"
				+ "korean|"
				+ "mongolian"
			+ ")";
	
	/*
	public static Map<String, String> languages_de = new HashMap<>();
	public static Map<String, String> languages_en = new HashMap<>();
	static {
		languages_de.put("<en>", "Englisch");
				
		languages_en.put("<en>", "English");
	}
	*/
	
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
		//String commonValue = input.replaceAll("^<|>$", "").trim();
				
		//Find generalized value - language (ISO 639-1) + country (ISO 3166)
		String iso639 = "";
		String iso3166 = "";
		String bcp47 = "";
		String localName = "";
		
		if (NluTools.stringContains(input, "american|(amerikanisch)\\w*")) {
			iso639 = "en";
			iso3166 = "US";
			
		}else if (NluTools.stringContains(input, "english|british|(englisch|britisch)\\w*")) {
			iso639 = "en";
			iso3166 = "GB";
		
		}else if (NluTools.stringContains(input, "german|(deutsch)\\w*")) {
			iso639 = "de";
			iso3166 = "DE";
			
		}else if (NluTools.stringContains(input, "spanish|(spanisch)\\w*")) {
			iso639 = "es";
			iso3166 = "ES";
			
		}else if (NluTools.stringContains(input, "portuguese|(portugiesisch)\\w*")) {
			iso639 = "pt";
			iso3166 = "PT";
		
		}else if (NluTools.stringContains(input, "french|(franzoesisch)\\w*")) {
			iso639 = "fr";
			iso3166 = "FR";
			
		}else if (NluTools.stringContains(input, "turkish|(tuerkisch)\\w*")) {
			iso639 = "tr";
			iso3166 = "TR";
			
		}else if (NluTools.stringContains(input, "dutch|(hollaendisch|niederlaendisch)\\w*")) {
			iso639 = "nl";
			iso3166 = "NL";
			
		}else if (NluTools.stringContains(input, "italian|(italienisch)\\w*")) {
			iso639 = "it";
			iso3166 = "IT";
			
		}else if (NluTools.stringContains(input, "japanese|(japanisch)\\w*")) {
			iso639 = "ja";
			iso3166 = "JP";
			
		}else if (NluTools.stringContains(input, "russian|(russisch)\\w*")) {
			iso639 = "ru";
			iso3166 = "RU";
			
		}else if (NluTools.stringContains(input, "swedish|(schwedisch)\\w*")) {
			iso639 = "sv";
			iso3166 = "SE";
			
		}else if (NluTools.stringContains(input, "greek|(griechisch)\\w*")) {
			iso639 = "el";
			iso3166 = "GR";
		}
		
		//TODO: add more
		
		//BCP47 and locale name
		if (!iso639.isEmpty() && !iso3166.isEmpty()) {
			bcp47 = iso639 + "-" + iso3166;
			
			if (this.language.equals(LANGUAGES.DE)){
				localName = Locale.forLanguageTag(bcp47).getDisplayLanguage(Locale.GERMAN);
			
			}else if (this.language.equals(LANGUAGES.EN)){
				localName = Locale.forLanguageTag(bcp47).getDisplayLanguage(Locale.ENGLISH);
			}
		}
		
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.VALUE, bcp47);
			JSON.add(itemResultJSON, InterviewData.VALUE_LOCAL, localName);
			JSON.add(itemResultJSON, InterviewData.FOUND, input); 		
		
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
