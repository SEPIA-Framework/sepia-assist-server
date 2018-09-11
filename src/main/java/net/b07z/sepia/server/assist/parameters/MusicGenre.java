package net.b07z.sepia.server.assist.parameters;

import java.util.regex.Pattern;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interpreters.Normalizer;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.JSON;

public class MusicGenre implements ParameterHandler{
	
	//-------data-------
	/*
	public static HashMap<String, String> musicGenres_de = new HashMap<>();
	public static HashMap<String, String> musicGenres_en = new HashMap<>();
	static {
		musicGenres_de.put("<rock>", "Rock");
		
		musicGenres_en.put("<rock>", "rock");
	}
	*/
	/**
	 * Translate generalized value.
	 * If generalized value is unknown returns empty string.
	 * @param value - generalized value 
	 * @param language - ISO language code
	 */
	public static String getLocal(String value, String language){
		return "";
		/*
		String localName = "";
		if (language.equals(LANGUAGES.DE)){
			localName = musicGenres_de.get(value);
		}else if (language.equals(LANGUAGES.EN)){
			localName = musicGenres_en.get(value);
		}
		if (localName == null){
			Debugger.println("MusicGenre.java - getLocal() has no '" + language + "' version for '" + value + "'", 3);
			return "";
		}
		return localName;
		*/
	}
	//------------------

	User user;
	NluInput nluInput;
	String language;
	boolean buildSuccess = false;
	
	//keep that in mind
	String found = "";		//exact (not generalized) string found during extraction (or guess?)
	
	@Override
	public void setup(NluInput nluInput) {
		this.nluInput = nluInput;
		this.user = nluInput.user;
		this.language = nluInput.language;
	}
	@Override
	public void setup(NluResult nluResult) {
		this.nluInput = nluResult.input;
		this.user = nluResult.input.user;
		this.language = nluResult.language;
	}
	
	@Override
	public String extract(String input) {
		String genre = "";
		
		//check storage first
		ParameterResult pr = nluInput.getStoredParameterResult(PARAMETERS.MUSIC_GENRE);
		if (pr != null){
			genre = pr.getExtracted();
			this.found = pr.getFound();
			
			return genre;
		}
		
		//German
		if (language.matches("de")){
			genre = NluTools.stringFindFirst(input, "klassik|pop|hard-rock|hardrock|hardcore|rock|metal|"
					+ "disco|acid jazz|jazz|hip-hop|hiphop|hip hop|rnb|r&b|blues|trance|elektro|deep house|"
					+ "house|eurodance|dance|gabba");
			
		//English and other
		}else{
			genre = NluTools.stringFindFirst(input, "classic|pop|hard-rock|hardrock|hardcore|rock|metal|"
					+ "disco|acid jazz|jazz|hip-hop|hiphop|hip hop|rnb|r&b|blues|trance|electro|deep house|"
					+ "house|eurodance|dance|gabba");
		}
		this.found = genre;
		
		//reconstruct original phrase to get proper item names
		if (!genre.isEmpty()){
			Normalizer normalizer = Config.inputNormalizers.get(language);
			genre = normalizer.reconstructPhrase(nluInput.textRaw, genre);
		}
		
		//store it
		pr = new ParameterResult(PARAMETERS.MUSIC_GENRE, genre, found);
		nluInput.addToParameterResultStorage(pr);
				
		return genre;
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
		return NluTools.stringRemoveFirst(input, Pattern.quote(found));
	}
	
	@Override
	public String responseTweaker(String input){
		if (language.equals(LANGUAGES.DE)){
			input = input.replaceAll("(?i).*\\b(starte|spiele|oeffne)\\b", "").trim();
			return input.replaceAll("(?i)\\b(starten|spielen|oeffnen|hoeren)\\b", "").trim();
		}else{
			input = input.replaceAll("(?i).*\\b(start|play|open|listen to)\\b", "").trim();
			return input;
		}
	}

	@Override
	public String build(String input) {
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.VALUE, input);
		
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
		return buildSuccess = true;
	}

}
