package net.b07z.sepia.server.assist.services;

import net.b07z.sepia.server.assist.answers.AnswerTools;
import net.b07z.sepia.server.assist.data.Card;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.data.Card.ElementType;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.DialogTaskValues;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.interviews.InterviewMetaData;
import net.b07z.sepia.server.assist.parameters.Action;
import net.b07z.sepia.server.assist.parameters.RadioStation;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

import java.net.URLEncoder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Performs radio station and music searches with cache (custom database), Laut.de and Dirble API.<br>
 * 
 * @author Florian Quirin
 *
 */
public class MusicRadioMixed implements ServiceInterface {
		
	//info
	public ServiceInfo getInfo(String language){
		//type
		ServiceInfo info = new ServiceInfo(Type.REST, Content.data, false);
		
		//Parameters:
		//preferred (but optional)
		Parameter p1 = new Parameter(PARAMETERS.RADIO_STATION, "")
				.setDialogTaskMetaData(DialogTaskValues.MUSIC)
				.setQuestion("music_radio_ask_0a");
		info.addParameter(p1);
		//optional
		Parameter p2 = new Parameter(PARAMETERS.MUSIC_GENRE, "");	//rock, pop, etc. ...
		Parameter p3 = new Parameter(PARAMETERS.ACTION, "");		//<on>, <off>, etc. ...
		//Parameter p4 = new Parameter(PARAMETERS.URL, "");			//custom stream URL - this is not a parameter one can extract so we don't include it here
		info.addParameter(p2).addParameter(p3);
		
		//... but either station or genre is required, if both are missing ask for station
		//info.getAtLeastOneOf("", p1, p2); 		
		//NOTE: this has been replaced by manual switch inside action check to support "radio off" and "resume radio" 
		
		//Answers (these are the default answers, you can add a custom answer at any point in the module with api.setCustomAnswer(..)):
		info.addSuccessAnswer("music_radio_1e")
			.addFailAnswer("music_radio_0a")
			.addOkayAnswer("music_radio_0b")
			.addCustomAnswer("streamResult", streamResultAns)
			.addCustomAnswer("radioAskStation", radioAskStation)
			.addCustomAnswer("radioOff", radioOffAns)
			.addCustomAnswer("radioResumeAns", radioResumeAns)
		.addAnswerParameters("station"); 		//be sure to use the same parameter names as in resultInfo
		
		return info;
	}
	private static String streamResultAns = "music_radio_1d";
	private static String radioAskStation = "music_radio_ask_0a";
	private static String radioOffAns = "music_radio_2a";
	private static String radioResumeAns = "ok_0b";
	
	//result
	public ServiceResult getResult(NluResult nluResult){
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult, 
				getInfoFreshOrCache(nluResult.input, this.getClass().getCanonicalName()));
		
		//get from cache or make the external API call and build API_Result
		try {
			//get parameters
			
			//Station
			Parameter stationP = nluResult.getOptionalParameter(PARAMETERS.RADIO_STATION, "");	//Note: the default defined here applies to all station keys
			String station = (String) stationP.getDataFieldOrDefault(InterviewData.INPUT);
			//String stationNorm = (String) stationP.getDataFieldOrDefault(InterviewData.VALUE);	//Note: we could use this instead of chacheStationName. See comment near bottom.
			String chacheStationName = (String) stationP.getDataFieldOrDefault(InterviewData.CACHE_ENTRY);
			/*if (!stationP.isDataEmpty()){
				JSONObject stationJSON = stationP.getData();
				chacheStationName = (String) stationJSON.get(InterviewData.CACHE_ENTRY);
			}*/
			
			//Genre
			Parameter genreP = nluResult.getOptionalParameter(PARAMETERS.MUSIC_GENRE, "");		//Note: the default defined here applies to all genre keys
			String genre = (String) genreP.getDataFieldOrDefault(InterviewData.INPUT);
			String genreNorm = (String) genreP.getDataFieldOrDefault(InterviewData.VALUE);
			
			//Action
			String action = (String) nluResult.getOptionalParameter(PARAMETERS.ACTION, "").getDataFieldOrDefault(InterviewData.VALUE);
			
			//Stream
			//String stream = (String) NLU_result.getOptionalParameter(PARAMETERS.URL, "").getDataFieldOrDefault(InterviewData.VALUE);
			String stream = nluResult.getParameter(PARAMETERS.URL);	//this parameter is not extractable but can be submitted by direct commands for example
			
			//custom answer?
			String reply = nluResult.getParameter(PARAMETERS.REPLY);	//can be defined e.g. via Teach-UI
			
			Debugger.println("cmd: Music radio, station: " + station + ", genre: " + genre + ", action: " + action, 2);				//debug
			
			/*
			System.out.println("RADIO OVERVIEW: ");
			System.out.println("station: " + station);
			System.out.println("station_n: " + stationNorm);
			System.out.println("chacheStationName: " + chacheStationName);
			System.out.println("genre: " + genre);
			System.out.println("genre_n: " + genreNorm);
			*/
						
			//define a title (used e.g. for labels and eventually for API search)
			//NOTE: either station or genre is REQUIRED (if action is not OFF)
			String title = "My stream";
			if (!station.isEmpty()){
				title = station;
			}else if (!genre.isEmpty()){
				title = genre;
			}
			api.resultInfoPut("station", title);
			
			//check actions first
			if (!action.isEmpty()){
				
				//--SWITCH OFF RADIO--
				if (action.equals("<" + Action.Type.off + ">") || action.equals("<" + Action.Type.pause + ">")){
					//add action
					api.addAction(ACTIONS.STOP_AUDIO_STREAM);
					api.hasAction = true;
					
					api.setCustomAnswer(radioOffAns);
					api.setStatusSuccess();
					
					ServiceResult result = api.buildResult();
					return result;
					//<----------- END------------
				
				//--RESUME--
				}else if (action.equals("<" + Action.Type.resume + ">")){
					//add action
					api.addAction(ACTIONS.PLAY_AUDIO_STREAM);
					api.hasAction = true;
					
					api.setCustomAnswer(radioResumeAns);
					api.setStatusSuccess();
					
					ServiceResult result = api.buildResult();
					return result;
					//<----------- END------------
				}
			}
			
			//check if we need to ask for station
			if (station.isEmpty() && genre.isEmpty()){
				//ask
				api.setIncompleteAndAsk(PARAMETERS.RADIO_STATION, radioAskStation,
					new InterviewMetaData().setDialogTask(DialogTaskValues.MUSIC));
				ServiceResult result = api.buildResult();
				return result;
				//<----------- END------------
			}
			
			//check if we have a stream
			if (!stream.isEmpty()){
				
				//add action
				api.addAction(ACTIONS.PLAY_AUDIO_STREAM);
				api.putActionInfo("audio_url", stream);
				api.putActionInfo("audio_title", title);
				api.hasAction = true;
				
				String playlistURL = RadioStation.getPlaylistUrl(title);
				if (playlistURL != null && !playlistURL.isEmpty()){
					api.addAction(ACTIONS.BUTTON_IN_APP_BROWSER);
					api.putActionInfo("url", playlistURL); 
					api.putActionInfo("title", "Playlist");
				}

				//build card
				Card card = new Card(Card.TYPE_UNI_LIST);
				JSONObject stationCard = new JSONObject();
					JSON.add(stationCard, "name", title);
					JSON.add(stationCard, "streamURL", stream);
				card.addGroupeElement(ElementType.radio, "", stationCard);
				//add it
				api.addCard(card.getJSON());
				api.hasCard = true;
				
				//default or custom reply?
				if (!reply.isEmpty()){
					reply = AnswerTools.handleUserAnswerSets(reply);
					api.setCustomAnswer(reply);
				}else{
					api.setCustomAnswer(streamResultAns);
				}
				api.setStatusSuccess();
				
				ServiceResult result = api.buildResult();
				return result;
				//<----------- END------------
			}
			
			//init card
			Card card = new Card(Card.TYPE_UNI_LIST);
			String active_stream = "";
			String active_name = "";

			//check SEPIA radio list for stations
			JSONArray stationsCollection = null;
			boolean isInSepiaStationList = false;
			boolean isInSepiaCollectionList = false;
			if (!station.isEmpty()){
				//Station list
				isInSepiaStationList = Is.notNullOrEmpty(chacheStationName);
				stationsCollection = RadioStation.getStation(chacheStationName);
				
			}else if (!genre.isEmpty()){
				//Collection/genre list
				stationsCollection = RadioStation.getGenreCollection(genreNorm);
				isInSepiaCollectionList = !stationsCollection.isEmpty();
			}
			
			if (isInSepiaStationList || isInSepiaCollectionList){
				//get all from cache and finish
				if (stationsCollection != null && !stationsCollection.isEmpty()){
					long tic = System.currentTimeMillis();
					int i = 0;
					for (Object so : stationsCollection){
						JSONObject stationCard = (JSONObject) so;
						String stationName = (String) stationCard.get("name");
						if (i==0){
							active_name = stationName;
							active_stream = (String) stationCard.get("streamURL");
						}
						//add playlist?
						String playlistURL = RadioStation.getPlaylistUrl(stationName);
						if (playlistURL == null){
							playlistURL = RadioStation.getPlaylistUrl(stationName.replaceAll(" - .*", "").trim());
						}
						if (playlistURL != null){
							JSON.put(stationCard, "playlistURL", playlistURL);
						}
						
						i++;
						card.addGroupeElement(ElementType.radio, "", stationCard);
					}
					//finish
					api.addCard(card.getJSON());
					api.hasCard = true;
					
					api.setStatusSuccess();
					
					Statistics.addExternalApiHit("Radio Cache");
					Statistics.addExternalApiTime("Radio Cache", tic);	
					//<----------- POSSIBLE END ------------
				}
			
			//search API 1 for non-popular stations
			}else{
				//search
				String search = (station.isEmpty()? genre : station); 		//station or genre, one must be
				
				//---- make the HTTP GET call to Laut.FM API ----
				//http://api.laut.fm/documentation/search
				long tic = System.currentTimeMillis();
				int limit = 5;
				String url = "http://api.laut.fm/search/stations"
										+ "?query=" + URLEncoder.encode(search, "UTF-8") 
										+ "&limit=" + limit;
				JSONObject response = Connectors.httpGET(url.trim());
				Statistics.addExternalApiHit("Radio laut.FM");
				Statistics.addExternalApiTime("Radio laut.FM", tic);
				//System.out.println("HTTP GET Response: " + response); 		//debug
				
				if (Connectors.httpSuccess(response)){
					try{
						int hits = 0;
						long N = (long) response.get("total");
						if (N>0){
							JSONArray results = ((JSONArray) response.get("results"));
							for (Object o : results){
								JSONArray items = (JSONArray)((JSONObject) o).get("items");
								for (Object oo : items){
									JSONObject stationRes = (JSONObject)((JSONObject)oo).get("station");
									String sName = (String) stationRes.get("name");
									sName = sName.replaceAll("_", " ");
									String sUrl = (String) stationRes.get("stream_url");
									String next = (String)((JSONObject)oo).get("next_artists");
									if (next != null && !next.isEmpty()){
										//sName = sName + "<br><span style='font-size:80%;'>" + next + "</span>";
										sName = sName + " - " + next;
										if (sName.length()>75){
											sName = sName.substring(0, 72) + "...";
										}
									}
									JSONObject cardStation = new JSONObject();
										JSON.add(cardStation, "name", sName);
										JSON.add(cardStation, "next", (next == null)? "" : next);
										JSON.add(cardStation, "streamURL", sUrl);
										JSON.add(cardStation, "API", "laut.FM");
									
									//System.out.println("card: " + cardStation.toJSONString()); 			//debug
									card.addGroupeElement(ElementType.radio, "", cardStation);
									//JSON.add(stationsCollection, cardStation);						//store result
									hits++;
									
									if (hits==1){
										active_stream = sUrl;
										active_name = sName;
									}
									if (hits >= limit){
										break;
									}
								}
								if (hits >= limit){
									break;
								}
							}
							//finish
							api.addCard(card.getJSON());
							api.hasCard = true;
							
							api.setStatusSuccess();
							//<----------- POSSIBLE END ------------
						
						}else{
							Debugger.println("Radio Laut.FM API - search '" + search + "' - Found no result!", 1);
							//<----------- POSSIBLE END ------------
						}
						
					//Error
					}catch (Exception e){
						Debugger.println("Radio Laut.FM API - search '" + search + "' - EXCEPTION: " + e.getMessage(), 1);
					}
				}
			}
			
			//check "other" API only when this one fails
			if (card.isEmpty()){
				stationsCollection =  new JSONArray();
				
				//search
				String search = (station.isEmpty()? genre : station); 		//station or genre, one must be
			
				//---- make the HTTP GET call to Dirble API ----
				
				JSONArray results = null;
				if (Config.dirble_key != null && !Config.dirble_key.isEmpty()){
					long tic = System.currentTimeMillis();
					String url = "http://api.dirble.com/v2/search/" + URLEncoder.encode(search, "UTF-8") 
														+ "?token=" + URLEncoder.encode(Config.dirble_key, "UTF-8");
					JSONObject response = Connectors.httpGET(url.trim());
					Statistics.addExternalApiHit("Radio Dirble");
					Statistics.addExternalApiTime("Radio Dirble", tic);
					//System.out.println("HTTP GET Response: " + response); 		//debug
					
					if (Connectors.httpSuccess(response)){
						//get Array
						results = (JSONArray) response.get("JSONARRAY");
					}
					
				}else{
					//no results because we have no API key
					Debugger.println("Radio Dirble API - search '" + search + "' - No API key to make call", 1);
				}
				
				//run through first N results (or less)
				int N=5;
				if (results !=null && results.size()>0){
					//init variables to collect some stuff
					N = (results.size()>N)? N : results.size();
					int hits = 0;
					
					for (int i=0; i<results.size(); i++){
						JSONObject o = (JSONObject) results.get(i);
						JSONObject stationCard = new JSONObject();
						//station name
						String station_name = (String) o.get("name");
						JSON.add(stationCard, "name", station_name);
						//stream
						JSONArray streams_ja = (JSONArray) o.get("streams");
						//skip m3u stations (TODO: make them available!)
						String goodURL = "";
						for (Object jo : streams_ja){
							goodURL = (String) ((JSONObject) jo).get("stream");
							if (!goodURL.contains(".m3u")){
								break;
							}else{
								goodURL = "";
							}
						}
						if (goodURL.isEmpty()){
							continue;
						}else{
							hits++;
						}
						stream = JSON.getString(streams_ja, 0, "stream");
						JSON.add(stationCard, "streamURL", stream);
						JSON.add(stationCard, "next", "");
						JSON.add(stationCard, "API", "Dirble");
						if (hits==1){
							active_stream = stream;
							active_name = station_name;
						}
						
						card.addGroupeElement(ElementType.radio, "", stationCard);
						if (isInSepiaStationList){
							JSON.add(stationsCollection, stationCard);				//store result
						}
						if (hits >= N){
							break;
						}
					}					
					//finish
					api.addCard(card.getJSON());
					api.hasCard = true;
					
					api.setStatusSuccess();
					
					//add stored Array to radio cache (but only if there is a prepared slot in the list ...) - NOTE: we could switch to stationNorm here
					if (isInSepiaStationList && !stationsCollection.isEmpty()){
						RadioStation.putStation(chacheStationName, stationsCollection);
						Debugger.println("Radio cache - wrote: '" + chacheStationName + "' with " + stationsCollection.size() + " stations", 3);
					}
				
				}else{
					//no results or communication error
					Debugger.println("Radio Dirble API - search '" + search + "' - Communication error or no result", 1);
				}
			}
			
			//add action
			if (!active_stream.isEmpty()){
				api.addAction(ACTIONS.PLAY_AUDIO_STREAM);
				api.putActionInfo("audio_url", active_stream);
				api.putActionInfo("audio_title", active_name);
				
				/*
				api.actionInfo_add_action(ACTIONS.BUTTON_CMD);
				api.actionInfo_put_info("title", ((active_name.length() > 17)? (active_name.substring(0, 17) + "...") : active_name));
				api.actionInfo_put_info("info", "direct_cmd");
				api.actionInfo_put_info("cmd", CMD.getRadio(station));
				*/
				
				String playlistURL = RadioStation.getPlaylistUrl(active_name);
				if (playlistURL == null){
					playlistURL = RadioStation.getPlaylistUrl(active_name.replaceAll(" - .*", "").trim());
				}
				if (playlistURL != null && !playlistURL.isEmpty()){
					api.addAction(ACTIONS.BUTTON_IN_APP_BROWSER);
					api.putActionInfo("url", playlistURL); 
					api.putActionInfo("title", "Playlist");
				}
				
				api.hasAction = true;
				
				//custom success reply?
				if (!reply.isEmpty()){
					reply = AnswerTools.handleUserAnswerSets(reply);
					api.setCustomAnswer(reply);
				}
			}
		
		//some error occurred somewhere 
		} catch (Exception e) {
			api.setStatusFail();
			Debugger.println("Music_Radio_Default.java - failed due to EXCEPTION: " + e.getMessage(), 1);
			Debugger.printStackTrace(e, 3);
		}
		
		//finally build the API_Result
		ServiceResult result = api.buildResult();
		
		//return result_JSON.toJSONString();
		return result;
	}
	
}
