package net.b07z.sepia.server.assist.services;

import java.util.TreeSet;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.parameters.ClientFunction;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.data.Language;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * A service to search music via client control actions.
 * 
 * @author Florian Quirin
 *
 */
public class MusicSearch implements ServiceInterface{
	
	//Define some sentences for testing:
	
	@Override
	public TreeSet<String> getSampleSentences(String lang){
		TreeSet<String> samples = new TreeSet<>();
		//GERMAN
		if (lang.equals(Language.DE.toValue())){
			samples.add("Spiele All Along the Watchtower von Jimi Hendrix auf Spotify.");
			
		//OTHER
		}else{
			samples.add("Play All Along the Watchtower by Jimi Hendrix on Spotify.");
		}
		return samples;
	}
	
	//Basic service setup:

	@Override
	public ServiceInfo getInfo(String language) {
		ServiceInfo info = new ServiceInfo(Type.plain, Content.data, false);
		
		//Command
		String CMD_NAME = CMD.MUSIC;
		info.setIntendedCommand(CMD_NAME);
				
		//Parameters:
		
		//optional
		Parameter p1 = new Parameter(PARAMETERS.MUSIC_SERVICE);
		Parameter p2 = new Parameter(PARAMETERS.MUSIC_GENRE);
		Parameter p3 = new Parameter(PARAMETERS.MUSIC_ALBUM);
		Parameter p4 = new Parameter(PARAMETERS.MUSIC_ARTIST);
		Parameter p5 = new Parameter(PARAMETERS.SONG);
		Parameter p6 = new Parameter(PARAMETERS.PLAYLIST_NAME);
		
		info.addParameter(p1).addParameter(p2).addParameter(p3).addParameter(p4).addParameter(p5).addParameter(p6);
		
		//Default answers
		info.addSuccessAnswer("music_1c")
			.addFailAnswer("error_0a")
			.addOkayAnswer("default_not_possible_0a")
			.addCustomAnswer("music_type", "music_ask_0b")
			.addCustomAnswer("what_song", "music_ask_1a")
			.addCustomAnswer("what_artist", "music_ask_1b")
			.addCustomAnswer("what_playlist", "music_ask_1c")
			;
		
		return info;
	}
	
	@Override
	public ServiceResult getResult(NluResult nluResult) {
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult, 
				getInfoFreshOrCache(nluResult.input, this.getClass().getCanonicalName()));
		
		//get parameters
		Parameter serviceP = nluResult.getOptionalParameter(PARAMETERS.MUSIC_SERVICE, "");
		String service = serviceP.getValueAsString().replaceAll("^<|>$", "").trim();
		
		Parameter genreP = nluResult.getOptionalParameter(PARAMETERS.MUSIC_GENRE, "");
		String genre = genreP.getValueAsString();
		
		Parameter albumP = nluResult.getOptionalParameter(PARAMETERS.MUSIC_ALBUM, "");
		String album = albumP.getValueAsString();
		
		Parameter artistP = nluResult.getOptionalParameter(PARAMETERS.MUSIC_ARTIST, "");
		String artist = artistP.getValueAsString();
		
		Parameter songP = nluResult.getOptionalParameter(PARAMETERS.SONG, "");
		String song = songP.getValueAsString();
		
		Parameter playlistP = nluResult.getOptionalParameter(PARAMETERS.PLAYLIST_NAME, "");
		String playlistName = playlistP.getValueAsString();
		
		boolean hasMinimalInfo = Is.notNullOrEmpty(song) || Is.notNullOrEmpty(artist) 
				|| Is.notNullOrEmpty(album) || Is.notNullOrEmpty(playlistName) || Is.notNullOrEmpty(genre);
		
		if (!hasMinimalInfo){
			String customTypeSelectionParameter = "music_type";
			JSONObject typeSelection = api.getSelectedOptionOf(customTypeSelectionParameter);
			if (Is.notNullOrEmpty(typeSelection)){
				//System.out.println(typeSelection.toJSONString()); 		//DEBUG
				int selection = JSON.getIntegerOrDefault(typeSelection, "selection", 0);
				if (selection == 1){
					api.setIncompleteAndAsk(PARAMETERS.SONG, "music_ask_1a");
				}else if (selection == 2){
					api.setIncompleteAndAsk(PARAMETERS.MUSIC_ARTIST, "music_ask_1b");
				}else if (selection == 3){
					api.setIncompleteAndAsk(PARAMETERS.PLAYLIST_NAME, "music_ask_1c");
				}else{
					//This should never happen
					Debugger.println("MusicSearch had invalid selection result for 'music_type'", 1);
					api.setStatusFail();
				}
				ServiceResult result = api.buildResult();
				return result;
			}else{
				api.askUserToSelectOption(customTypeSelectionParameter, JSON.make(
						"1", "song|lied", 
						"2", "artist|kuenstler", 
						"3", "playlist"
				), "music_ask_0b");
				ServiceResult result = api.buildResult();
				//System.out.println(result.getResultJSON().toString()); 	//DEBUG
				return result;
			}
		}
		
		//This service basically cannot fail here ... only inside client
		
		//If we have only a song we should declare the 'search' field and not rely on song-name
		boolean hasOnlySong = Is.notNullOrEmpty(song) && 
				Is.nullOrEmpty(artist) && Is.nullOrEmpty(album) && Is.nullOrEmpty(playlistName) && Is.nullOrEmpty(genre);
		String search = (hasOnlySong)? song : "";
		
		String controlFun = ClientFunction.Type.searchForMusic.name();
		JSONObject controlData = JSON.make(
				"artist", artist,
				"song", song,
				"album", album,
				"playlist", playlistName,
				"service", service
		);
		JSON.put(controlData, "genre", genre);
		JSON.put(controlData, "search", search);
		api.addAction(ACTIONS.CLIENT_CONTROL_FUN);
		api.putActionInfo("fun", controlFun);
		api.putActionInfo("controlData", controlData);
		
		//some buttons - we use the custom function button but the client needs to parse the string itself!
		if (Is.nullOrEmpty(service)){
			//simple action button
			api.addAction(ACTIONS.BUTTON_CUSTOM_FUN);
			api.putActionInfo("fun", "controlFun;;" + controlFun + ";;" + controlData.toJSONString());
			api.putActionInfo("title", "Button");

		}else{
			//Cards
			/* Cards should be generated by client ...
			Card card = new Card(Card.TYPE_SINGLE);
			card.addElement(ElementType.link, 
					JSON.make("title", "S.E.P.I.A." + ":", "desc", "Client Controls"),
					null, null, "", 
					"https://sepia-framework.github.io/", 
					"https://sepia-framework.github.io/img/icon.png", 
					null, null);
			//JSON.put(linkCard, "imageBackground", "transparent");	//use any CSS background option you wish
			api.addCard(card.getJSON());
			*/
		}

		//all good
		/*if (!hasMinimalInfo){
			api.setStatusOkay();
		}else{
			api.setStatusSuccess();
		}*/
		api.setStatusSuccess();
		
		//build the API_Result
		ServiceResult result = api.buildResult();
		return result;
	}
}
