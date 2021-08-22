package net.b07z.sepia.server.assist.parameters;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Parameter handler to search for a music service like Spotify or Apple Music
 * 
 * @author Florian Quirin
 *
 */
public class MusicService implements ParameterHandler{
	
	public static enum Service {
		embedded,		//handled in client custom widget (not part of NLU atm)
		spotify,
		spotify_link,
		spotify_embedded,
		apple_music,
		apple_music_link,
		apple_music_embedded,
		amazon_music,
		deezer,
		soundcloud,
		soundcloud_embedded,
		youtube,
		youtube_link,
		youtube_embedded,
		vlc_media_player
	}
	
	//-------data-------
	public static Map<String, String> musicServices = new HashMap<>();
	static {
		musicServices.put("<embedded>", "Media Player");
		musicServices.put("<spotify>", "Spotify");
		musicServices.put("<spotify_link>", "Spotify");
		musicServices.put("<spotify_embedded>", "Spotify");
		musicServices.put("<apple_music>", "Apple Music");
		musicServices.put("<apple_music_link>", "Apple Music");
		musicServices.put("<apple_music_embedded>", "Apple Music");
		musicServices.put("<amazon_music>", "Amazon Music");
		musicServices.put("<deezer>", "Deezer");
		musicServices.put("<soundcloud>", "SoundCloud");
		musicServices.put("<soundcloud_embedded>", "SoundCloud");
		musicServices.put("<youtube>", "YouTube");
		musicServices.put("<youtube_link>", "YouTube");
		musicServices.put("<youtube_embedded>", "YouTube");
		musicServices.put("<vlc_media_player>", "VLC Media Player");
	}
	/**
	 * Translate generalized value.
	 * If generalized value is unknown returns empty string.
	 * @param value - generalized value 
	 * @param language - ISO language code
	 */
	public static String getLocal(String value, String language){
		String localName = musicServices.get(value);
		if (localName == null){
			Debugger.println("MusicService.java - getLocal() has no '" + language + "' version for '" + value + "'", 3);
			return "";
		}
		return localName;
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
		String service = "";
		
		//check storage first
		ParameterResult pr = nluInput.getStoredParameterResult(PARAMETERS.MUSIC_SERVICE);
		if (pr != null){
			service = pr.getExtracted();
			this.found = pr.getFound();
			
			return service;
		}
		
		String spotify = "spotify";
		String appleMusic = "apple music|apple|itunes";
		String amazonMusic = "amazon( music|)";
		String deezer = "deezer";
		String soundCloud = "sound( |-|)cloud";
		String youTube = "you( |-|)tube";
		String vlc = "vlc( media|)( player|)";
		
		if (language.equals(LANGUAGES.DE)){
			//GERMAN
			service = NluTools.stringFindFirst(input, 
				"(mittels |mit |ueber |via |auf |durchsuche )(dem |der |die |)(" + 
					spotify + "|" +
					appleMusic + "|" +
					amazonMusic + "|" +
					deezer + "|" +
					soundCloud + "|" +
					youTube + "|" +
					vlc +
				")( link| embedded|)"
			);
		}else{
			//OTHER LANGUAGES
			service = NluTools.stringFindFirst(input, 
				"(with |via |using |on |over |search )(the |)(" + 
					spotify + "|" +
					appleMusic + "|" +
					amazonMusic + "|" +
					deezer + "|" +
					soundCloud + "|" +
					youTube + "|" +
					vlc +
				")( link| embedded|)"
			);
		}
		
		this.found = service; 
		
		if (!service.isEmpty()){
			//URL based variant of service?
			if (NluTools.stringContains(service, "link")){
				if (NluTools.stringContains(service, spotify)){
					service = "<" + Service.spotify_link.name() + ">";
				}else if (NluTools.stringContains(service, appleMusic)){
					service = "<" + Service.apple_music_link.name() + ">";
				}else if (NluTools.stringContains(service, youTube)){
					service = "<" + Service.youtube_link.name() + ">";
				}
			//custom widget based variant of service?
			}else if (NluTools.stringContains(service, "(embedded)$")){
				if (NluTools.stringContains(service, spotify)){
					service = "<" + Service.spotify_embedded.name() + ">";
				}else if (NluTools.stringContains(service, appleMusic)){
					service = "<" + Service.apple_music_embedded.name() + ">";
				}else if (NluTools.stringContains(service, youTube)){
					service = "<" + Service.youtube_embedded.name() + ">";
				}
			//app or default
			}else if (NluTools.stringContains(service, spotify)){
				service = "<" + Service.spotify.name() + ">";
			}else if (NluTools.stringContains(service, appleMusic)){
				service = "<" + Service.apple_music.name() + ">";
			}else if (NluTools.stringContains(service, amazonMusic)){
				service = "<" + Service.amazon_music.name() + ">";
			}else if (NluTools.stringContains(service, deezer)){
				service = "<" + Service.deezer.name() + ">";
			}else if (NluTools.stringContains(service, soundCloud)){
				service = "<" + Service.soundcloud.name() + ">";
			}else if (NluTools.stringContains(service, youTube)){
				service = "<" + Service.youtube.name() + ">";
			}else if (NluTools.stringContains(service, vlc)){
				service = "<" + Service.vlc_media_player.name() + ">";
			}
		}
		
		//store it
		pr = new ParameterResult(PARAMETERS.MUSIC_SERVICE, service, found);
		nluInput.addToParameterResultStorage(pr);
				
		return service;
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
			found = "(mittels |mit |ueber |via |auf |)" + Pattern.quote(found);
		}else{
			found = "(with |via |using |on |)" + Pattern.quote(found);
		}
		return NluTools.stringRemoveFirst(input, found);
	}
	
	@Override
	public String responseTweaker(String input){
		if (language.equals(LANGUAGES.DE)){
			return input.replaceAll("(?i).*\\b(mittels|mit|ueber|via|auf)\\b", "").trim();
		}else{
			return input.replaceAll("(?i).*\\b(with|via|using|on)\\b", "").trim();
		}
	}

	@Override
	public String build(String input) {
		String inputLocal = getLocal(input, this.language);
		if (inputLocal.isEmpty()){
			return "";
		}
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.VALUE, input.replaceAll("^<|>$", "").trim());
			JSON.add(itemResultJSON, InterviewData.VALUE_LOCAL, inputLocal);
		
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
		return buildSuccess = true;
	}

}
