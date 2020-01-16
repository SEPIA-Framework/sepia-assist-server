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
 * A parameter handler that searches for creators like music artists. 
 * 
 * @author Florian Quirin
 *
 */
public class MusicArtist implements ParameterHandler {

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
		ParameterResult pr = nluInput.getStoredParameterResult(PARAMETERS.MUSIC_ARTIST);
		if (pr != null){
			String item = pr.getExtracted();
			this.found = pr.getFound();
			
			return item;
		}
		String optimizedInput = input;
		//clean some parameters
		ParameterResult prMusicService = ParameterResult.getResult(nluInput, PARAMETERS.MUSIC_SERVICE, optimizedInput);
		if (prMusicService != null){
			optimizedInput = ParameterResult.cleanInputOfFoundParameter(nluInput, PARAMETERS.MUSIC_SERVICE, prMusicService, optimizedInput);
		}
		ParameterResult prMusicAlbum = ParameterResult.getResult(nluInput, PARAMETERS.MUSIC_ALBUM, optimizedInput);
		if (prMusicAlbum != null){
			optimizedInput = ParameterResult.cleanInputOfFoundParameter(nluInput, PARAMETERS.MUSIC_ALBUM, prMusicAlbum, optimizedInput);
		}
		ParameterResult prMusicPlaylist = ParameterResult.getResult(nluInput, PARAMETERS.PLAYLIST_NAME, optimizedInput);
		if (prMusicPlaylist != null){
			optimizedInput = ParameterResult.cleanInputOfFoundParameter(nluInput, PARAMETERS.PLAYLIST_NAME, prMusicPlaylist, optimizedInput);
		}
		
		String creator = "";
		//German
		if (this.language.matches(LANGUAGES.DE)){
			if (!optimizedInput.matches(".*\\b(von|vom) .+ (nach|bis)\\b.*") 
					|| NluTools.stringContains(optimizedInput, "spiel(en|e|)|start(en|e|)|oeffne(n|)")){
				//one or two matches (e.g. castles made of sand by Jimi Hendrix)
				String m1 = NluTools.stringFindFirst(optimizedInput, "(von|vom) .*");
				String m2 = (!m1.isEmpty())? NluTools.stringFindFirst(m1.replaceFirst("\\w+ ", ""), "(von|vom) .*") : "";
				creator = (!m2.isEmpty())? m2 : m1;
				creator = creator.replaceFirst("^(von|vom) ", "");
				creator = creator.replaceFirst("^(der|die|das|dem|den|einer|eine|einem)\\b", "").trim();
				creator = creator.replaceFirst("^(artist|musiker|kuenstler)\\b", "").trim();
				creator = creator.replaceFirst("(spielen|oeffnen|starten)$", "");
			}
			
		//English and other
		}else{
			if (!optimizedInput.matches(".*\\b(from|of|by) .+ (to|till|until)\\b.*")
					|| NluTools.stringContains(optimizedInput, "play|start|open")){
				String m1 = NluTools.stringFindFirst(optimizedInput, "(from|of|by) .*");
				String m2 = (!m1.isEmpty())? NluTools.stringFindFirst(m1.replaceFirst("\\w+ ", ""), "(from|of|by) .*") : "";
				creator = (!m2.isEmpty())? m2 : m1;
				creator = creator.replaceFirst("^(from|of|by) ", "");
				creator = creator.replaceFirst("^(the|a|an)\\b", "").trim();
				creator = creator.replaceFirst("^(artist|musician)\\b", "").trim();
			}
		}
		//Phase2:
		if (creator.isEmpty()){
			if (this.language.matches(LANGUAGES.DE)){
				creator = NluTools.stringFindFirst(optimizedInput, ".*? (songs|lieder|titel|musik)");
				creator = creator.replaceFirst(" (songs|lieder|musik|titel)$", "");
				creator = creator.replaceFirst(".*\\b(spiel(e|)|oeffne|start(e|))\\b", "");
				//clean actions
				creator = creator.trim().replaceFirst("^(stoppe(n|)|stop|naechste(\\w|)|vorherige(\\w|)|anhalten|schliessen|cancel|abbrechen|zurueck|vor)$", "");
			}else{
				creator = NluTools.stringFindFirst(optimizedInput, ".*? (songs|titles|tracks|music)");
				creator = creator.replaceFirst(" (songs|music|titles|tracks)$", "");
				creator = creator.replaceFirst(".*\\b(play|open|start)\\b", "");
				//clean actions
				creator = creator.trim().replaceFirst("^(stop|next|previous|clear|cancel|abort|close|end|back|forward)$", "");
			}
			if (!creator.trim().isEmpty()){
				//clean genre parameter
				ParameterResult prMusicGenre = ParameterResult.getResult(nluInput, PARAMETERS.MUSIC_GENRE, optimizedInput);
				if (prMusicGenre != null){
					creator = ParameterResult.cleanInputOfFoundParameter(nluInput, PARAMETERS.MUSIC_GENRE, prMusicGenre, creator);
				}
				creator = creator.trim();
			}
		}
		this.found = creator;
		
		//reconstruct original phrase to get proper item names
		if (!creator.isEmpty()){
			Normalizer normalizer = Config.inputNormalizers.get(this.language);
			creator = normalizer.reconstructPhrase(nluInput.textRaw, creator);
		}
		
		//store it
		pr = new ParameterResult(PARAMETERS.MUSIC_ARTIST, creator.trim(), found);
		nluInput.addToParameterResultStorage(pr);
		
		return creator;
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
			found = "(von |vom |)" + Pattern.quote(found);
		}else{
			found = "(by |of |from |)" + Pattern.quote(found);
		}
		return NluTools.stringRemoveFirst(input, found);
	}
	
	@Override
	public String responseTweaker(String input){
		if (language.equals(LANGUAGES.DE)){
			input = input.replaceFirst("^(von|vom) ", "");
			input = input.replaceFirst("^(der|die|das|dem|den|einer|eine|einem) ", "");
			return input.trim();
		}else{
			input = input.replaceFirst("^(from|of|by) ", "");
			input = input.replaceFirst("^(the|a|an) ", "");
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
