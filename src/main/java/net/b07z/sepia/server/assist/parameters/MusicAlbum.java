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
 * A parameter handler that searches for music albums. 
 * 
 * @author Florian Quirin
 *
 */
public class MusicAlbum implements ParameterHandler {

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
		ParameterResult pr = nluInput.getStoredParameterResult(PARAMETERS.MUSIC_ALBUM);
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
		
		String albumTitle = "";
		//German
		if (this.language.matches(LANGUAGES.DE)){
			albumTitle = NluTools.stringFindFirst(optimizedInput, "(von (dem|der) |vom )(album|platte) .*");
			if (!albumTitle.isEmpty()){
				albumTitle = albumTitle.replaceFirst("(von dem |vom )(album|platte) ", "");
				albumTitle = albumTitle.replaceFirst(" (den |das |)(song|lied|titel) .*", "").trim();
			}else{
				albumTitle = NluTools.stringFindFirst(optimizedInput, "(album|platte) .*");
				albumTitle = albumTitle.replaceFirst("^(album|platte) ", "").trim();
				albumTitle = albumTitle.replaceFirst(" (von) .*?$", "").trim();
			}
			
		//Other languages
		}else{
			albumTitle = NluTools.stringFindFirst(optimizedInput, "(from (the |))(album|record) .*");
			if (!albumTitle.isEmpty()){
				albumTitle = albumTitle.replaceFirst("(from (the |))(album|record) ", "");
				albumTitle = albumTitle.replaceFirst(" (the |)(song|title) .*", "").trim();
			}else{
				albumTitle = NluTools.stringFindFirst(optimizedInput, "(album|record) .*");
				albumTitle = albumTitle.replaceFirst("^(album|record) ", "").trim();
				albumTitle = albumTitle.replaceFirst(" (from|by|of) .*?$", "").trim();
			}
		}
		this.found = albumTitle;
		
		//reconstruct original phrase to get proper item names
		if (!albumTitle.isEmpty()){
			Normalizer normalizer = Config.inputNormalizers.get(this.language);
			albumTitle = normalizer.reconstructPhrase(nluInput.textRaw, albumTitle);
		}
		
		//store it
		pr = new ParameterResult(PARAMETERS.MUSIC_ALBUM, albumTitle.trim(), found);
		nluInput.addToParameterResultStorage(pr);
		
		return albumTitle;
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
			found = "(vom |von |)(dem |der |das |die |)(album|platte) " + Pattern.quote(found);
		}else{
			found = "(of |from |)(the |a |)(album|record) " + Pattern.quote(found);
		}
		return NluTools.stringRemoveFirst(input, found);
	}
	
	@Override
	public String responseTweaker(String input) {
		if (language.equals(LANGUAGES.DE)){
			input = input.replaceFirst(".*? (dem |der |)(album|platte) ", "");
			input = input.replaceFirst("^(von|vom) ", "");
			return input.trim();
		}else{
			input = input.replaceFirst(".*? (the |)(album|record) ", "");
			input = input.replaceFirst("^(from|of) ", "");
			return input.trim();
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
