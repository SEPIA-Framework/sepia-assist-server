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
 * A parameter handler that searches for playlist names. 
 * 
 * @author Florian Quirin
 *
 */
public class PlaylistName implements ParameterHandler {

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
		ParameterResult pr = nluInput.getStoredParameterResult(PARAMETERS.PLAYLIST_NAME);
		if (pr != null){
			String item = pr.getExtracted();
			this.found = pr.getFound();
			
			return item;
		}
		
		String playlistName;
		if (language.equals(LANGUAGES.DE)){
			//GERMAN
			playlistName = NluTools.stringFindFirst(input, "playlist(e|) ((mit |)namen|namens|genannt) .*");
			if (!playlistName.isEmpty()){
				playlistName = playlistName.replaceAll(".*?\\b((mit |)namen|namens|genannt)\\b", "");
			}else{
				playlistName = NluTools.stringFindFirst(input, ".* playlist(e|)");
				if (!playlistName.isEmpty()){
					playlistName = playlistName.replaceFirst(".*?\\b((von|auf) (der |meiner |einer |))(?<name>.*)( playlist(e|))\\b", "${name}").trim();
					playlistName = playlistName.replaceFirst(".*?\\b(starte|oeffne|spiel(e|)|such(e|)|finde|zeig(e|))\\b", "").trim();
					playlistName = playlistName.replaceFirst("^(von |auf |)(der|meine(r|)|die|eine(r|))\\b", "");
				}
			}
			playlistName = playlistName.replaceAll("\\b(playlist(e|))\\b", "").trim();
			//clean actions
			playlistName = playlistName.trim().replaceFirst("^(stoppe(n|)|stop|naechste(\\w|)|vorherige(\\w|)|cancel|abbrechen|zurueck|vor)$", "");
		}else{
			//ENGLISH
			playlistName = NluTools.stringFindFirst(input, "playlist (called|named|(with (the |)|)name) .*");
			if (!playlistName.isEmpty()){
				playlistName = playlistName.replaceAll(".*?\\b(called|named|(with (the |)|)name)\\b", "");
			}else{
				playlistName = NluTools.stringFindFirst(input, ".* playlist");
				if (!playlistName.isEmpty()){
					playlistName = playlistName.replaceFirst(".*?\\b((from|of|on) (the |my |a |))(?<name>.*)( playlist)\\b", "${name}").trim();
					playlistName = playlistName.replaceFirst(".*?\\b(start|open|play|search|find|show)\\b", "").trim();
					playlistName = playlistName.replaceFirst("^(on |from |of |)(my|the|a)\\b", "");
				}
			}
			playlistName = playlistName.replaceAll("\\b(playlist)\\b", "").trim();
			//clean actions
			playlistName = playlistName.trim().replaceFirst("^(stop|next|previous|clear|cancel|abort|back|forward)$", "");
		}
		this.found = playlistName;
		
		//reconstruct original phrase to get proper item names
		if (!playlistName.isEmpty()){
			Normalizer normalizer = Config.inputNormalizers.get(this.language);
			playlistName = normalizer.reconstructPhrase(nluInput.textRaw, playlistName);
		}
		
		//store it
		pr = new ParameterResult(PARAMETERS.PLAYLIST_NAME, playlistName.trim(), found);
		nluInput.addToParameterResultStorage(pr);
		
		return playlistName;
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
			found = "(von |auf |)(der |)" + Pattern.quote(found) + "(| playlist(e|))";
		}else{
			found = "(of |from |on |)(the |)" + Pattern.quote(found) + "(| playlist)";
		}
		return NluTools.stringRemoveFirst(input, found);
	}
	
	@Override
	public String responseTweaker(String input){
		if (language.equals(LANGUAGES.DE)){
			input = input.replaceFirst(".* (heisst|name ist)\\b", "");
		}else{
			input = input.replaceFirst(".* (called|name is)\\b", "");
		}
		return input.trim();
	}

	@Override
	public String build(String input) {
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.INPUT_RAW, nluInput.textRaw);
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
