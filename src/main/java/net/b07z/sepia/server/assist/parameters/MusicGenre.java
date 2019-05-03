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

/**
 * Parameter handler to search for a music genre like rock.
 * 
 * @author Florian Quirin
 *
 */
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
		
		String common = "pop|"
				+ "((hard|blues|classic)(-| |)|)rock|"
				+ "hardcore|"
				+ "((heavy|death)(-| |)|)metal|"
				+ "alternative|"
				+ "indie|"
				+ "disco|"
				+ "(acid(-| |)|)jazz|"
				+ "hip(-| |)hop|"
				+ "r(n|&)b|"
				+ "(deep(-| |)|)house|"
				+ "(euro(-| |)|)dance|"
				+ "blues|trance|electro|gabba";
		
		//TODO: this parameter need a little upgrade ... we should generalize the names better (see below normalizeGenreName)
		
		//German
		if (language.matches(LANGUAGES.DE)){
			//split 'radio' if common
			input = input.replaceFirst("(ein) (\\w+)(radio)", "$1 $2 $3")		//We keep the "ein" to distinguish "a rockradio" and "delta rockradio" (name)
						.replaceFirst("(" + common + ")(musik)", "$1 $2");		//Note: this will mess with 'found' 
			
			genre = NluTools.stringFindFirst(input, "(mein(s|en|e|) |)(" 
					+ common + "|"
					+ "klassik)|mein(s|en|e|)");
			
		//English and other
		}else{
			input = input.replaceFirst("(a) (\\w+)(radio|station)", "$1 $2 $3")	//We keep the "a" to distinguish "a rockradio" and "delta rockradio" (name)
						.replaceFirst("(" + common + ")(music)", "$1 $2");		//Note: this will mess with 'found'
			
			genre = NluTools.stringFindFirst(input, "(my |)("
					+ common + "|"
					+ "classic)|my");
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
		//extend found due to split in 'extract'
		if (language.equals(LANGUAGES.DE)){
			found = "(mit |)" + Pattern.quote(found) + "(radio|musik|)";
		}else{
			found = "(with |)" + Pattern.quote(found) + "(radio|music|)";
		}
		return NluTools.stringRemoveFirst(input, found);
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
			JSON.add(itemResultJSON, InterviewData.INPUT, input);
			JSON.add(itemResultJSON, InterviewData.VALUE, normalizeGenreName(input));
		
		buildSuccess = true;
		return itemResultJSON.toJSONString();
	}
	/**
	 * Convert a genre name to normalized format (e.g. Heavy-Metal -> heavy_metal).
	 */
	public static String normalizeGenreName(String genreIn){
		//optimize stations
		genreIn = genreIn
				.replaceFirst("(?i)\\b(mein(s|en|e|))\\b", "my") 		//TODO: this is inefficient!
				.replaceFirst("(?i)\\b(klassik)\\b", "classic")
				.replaceFirst("(?i)\\b(alternativ)\\b", "alternative")
				;
		//clean
		return genreIn.toLowerCase().replaceAll("(\\s+|-|_)", "").trim();
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
