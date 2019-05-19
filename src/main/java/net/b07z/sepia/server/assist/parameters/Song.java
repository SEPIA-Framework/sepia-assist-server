package net.b07z.sepia.server.assist.parameters;

import java.util.regex.Pattern;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interpreters.Normalizer;
import net.b07z.sepia.server.assist.interpreters.RegexParameterSearch;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * A parameter handler that searches for song names. 
 * 
 * @author Florian Quirin
 *
 */
public class Song implements ParameterHandler {

	public User user;
	public NluInput nluInput;
	public String language;
	public boolean buildSuccess = false;
	
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
		//check storage first
		ParameterResult pr = nluInput.getStoredParameterResult(PARAMETERS.SONG);
		if (pr != null){
			String item = pr.getExtracted();
			this.found = pr.getFound();
			
			return item;
		}
		String optimizedInput = input;
		//clean some parameters
		ParameterResult prMusicService = ParameterResult.getResult(nluInput, PARAMETERS.MUSIC_SERVICE, optimizedInput);
		if (prMusicService != null){
			optimizedInput = ParameterResult.cleanInputOfFoundParameter(nluInput, PARAMETERS.MUSIC_SERVICE, prMusicService, input);
		}
		ParameterResult prMusicGenre = ParameterResult.getResult(nluInput, PARAMETERS.MUSIC_GENRE, optimizedInput);
		if (prMusicGenre != null){
			optimizedInput = ParameterResult.cleanInputOfFoundParameter(nluInput, PARAMETERS.MUSIC_GENRE, prMusicGenre, optimizedInput);
		}
		ParameterResult prMusicAlbum = ParameterResult.getResult(nluInput, PARAMETERS.MUSIC_ALBUM, optimizedInput);
		if (prMusicAlbum != null){
			optimizedInput = ParameterResult.cleanInputOfFoundParameter(nluInput, PARAMETERS.MUSIC_ALBUM, prMusicAlbum, optimizedInput);
		}
		
		String song = RegexParameterSearch.get_startable(optimizedInput, this.language);
		//TODO: this will fail for 'play castles made of sand by Jimi Hendrix'
		
		//some filters
		if (song.contains("playlist")){
			song = "";
		}
		if (!song.isEmpty()){
			if (language.equals(LANGUAGES.DE)){
				song = song.replaceFirst("^(ein(en|ige|) |den |das |etwas )", "").trim();
				song = song.replaceFirst("^(naechste(\\w|)|vorherige(\\w|))", "").trim();
				song = song.replaceFirst("^(song(s|)|lied(er|)|musik|titel|(irgend|)etwas|was|egal was)\\b", "").trim();
				song = song.replaceFirst("^((mit (dem |)|)(titel|namen)|namens)\\b", "").trim();
				song = song.replaceFirst(".*? (songs|lieder|musik( titel|)|titel)$", "").trim();
			}else{
				song = song.replaceFirst("^(the |a |any |some )", "").trim();
				song = song.replaceFirst("^(next|previous)", "").trim();
				song = song.replaceFirst("^(song(s|)|music|title(s|)|track(s|)|something|anything)\\b", "").trim();
				song = song.replaceFirst("^((with (the |)|)(title|name)|named|)\\b", "").trim();
				song = song.replaceFirst(".*? (songs|music|titles)$", "").trim();
			}
		}
		this.found = song;
		
		//reconstruct original phrase to get proper item names
		if (!song.isEmpty()){
			Normalizer normalizer = Config.inputNormalizers.get(this.language);
			song = normalizer.reconstructPhrase(nluInput.textRaw, song);
		}
		
		//store it
		pr = new ParameterResult(PARAMETERS.SONG, song.trim(), found);
		nluInput.addToParameterResultStorage(pr);
		
		return song;
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
		return input.trim();
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
		return buildSuccess;
	}

}
