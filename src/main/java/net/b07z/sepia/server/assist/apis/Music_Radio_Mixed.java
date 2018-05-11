package net.b07z.sepia.server.assist.apis;

import net.b07z.sepia.server.assist.apis.ApiInfo.Content;
import net.b07z.sepia.server.assist.apis.ApiInfo.Type;
import net.b07z.sepia.server.assist.data.Card;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.data.Card.ElementType;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.parameters.Action;
import net.b07z.sepia.server.assist.parameters.RadioStation;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

import java.net.URLEncoder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Performs radio station and music searches with Cache, Laut.de and Dirble API.<br>
 * 
 * @author Florian Quirin
 *
 */
public class Music_Radio_Mixed implements ApiInterface {
		
	//info
	public ApiInfo getInfo(String language){
		//type
		ApiInfo info = new ApiInfo(Type.REST, Content.data, false);
		
		//Parameters:
		//preferred (but optional)
		Parameter p1 = new Parameter(PARAMETERS.RADIO_STATION, "")
				.setQuestion("music_radio_ask_0a");
		info.addParameter(p1);
		//optional
		Parameter p2 = new Parameter(PARAMETERS.MUSIC_GENRE, "");			//<rock>, <pop>, etc. ...
		Parameter p3 = new Parameter(PARAMETERS.ACTION, "");				//<on>, <off>, etc. ...
		//Parameter p4 = new Parameter(PARAMETERS.URL, "");					//custom stream URL - this is not a parameter on can extract so we don't include it here
		info.addParameter(p2).addParameter(p3);
		
		//... but one of these optional parameters is required
		info.getAtLeastOneOf("", p1, p2); 		//either station or genre is required, if both are missing ask for station
		
		//Answers (these are the default answers, you can add a custom answer at any point in the module with api.setCustomAnswer(..)):
		info.addSuccessAnswer("music_radio_1e")
			.addFailAnswer("music_radio_0a")
			.addOkayAnswer("music_radio_0b")
			.addCustomAnswer("streamResult", streamResultAns)
			.addCustomAnswer("radioOff", radioOffAns)
			.addAnswerParameters("station"); 		//be sure to use the same parameter names as in resultInfo
		
		return info;
	}
	private static String streamResultAns = "music_radio_1d";
	private static String radioOffAns = "music_radio_2a";
	
	//result
	public ApiResult getResult(NluResult NLU_result){
		//initialize result
		API api = new API(NLU_result, getInfo(""));
		
		//get from cache or make the external API call and build API_Result
		try {
			//get parameters
			Parameter stationP = NLU_result.getOptionalParameter(PARAMETERS.RADIO_STATION, "");
			String station = "";
			String chacheStationName = "";
			if (!stationP.isDataEmpty()){
				JSONObject stationJSON = stationP.getData();
				station = (String) stationJSON.get(InterviewData.VALUE);
				chacheStationName = (String) stationJSON.get(InterviewData.CACHE_ENTRY);
			}
			String genre = (String) NLU_result.getOptionalParameter(PARAMETERS.MUSIC_GENRE, "").getDataFieldOrDefault(InterviewData.VALUE);
			String action = (String) NLU_result.getOptionalParameter(PARAMETERS.ACTION, "").getDataFieldOrDefault(InterviewData.VALUE);
			//String stream = (String) NLU_result.getOptionalParameter(PARAMETERS.URL, "").getDataFieldOrDefault(InterviewData.VALUE);
			String stream = NLU_result.getParameter(PARAMETERS.URL);	//this parameter is not extractable but can be submitted by direct commands for example
			
			Debugger.println("cmd: Music radio, station: " + station + ", genre: " + genre + ", action: " + action, 2);				//debug
			
			//continue with station only
			if (station.isEmpty()){
				//in this case genre has to have a value
				station = genre;
			}
			api.resultInfo_add("station", station);
			
			//check actions first
			if (!action.isEmpty()){
				
				//--SWITCH OFF RADIO--
				if (action.equals("<" + Action.Type.off + ">") || action.equals("<" + Action.Type.pause + ">")){
					//add action
					api.actionInfo_add_action(ACTIONS.STOP_AUDIO_STREAM);
					api.hasAction = true;
					
					api.setCustomAnswer(radioOffAns);
					api.setStatusSuccess();
					
					ApiResult result = api.build_API_result();
					return result;
					//<----------- END------------
				}
			}
			
			//check if we have a stream
			if (!stream.isEmpty()){
				
				String title = "My stream";
				if (!station.isEmpty()){
					title = station;
				}
				//add action
				api.actionInfo_add_action(ACTIONS.PLAY_AUDIO_STREAM);
				api.actionInfo_put_info("audio_url", stream);
				api.actionInfo_put_info("audio_title", title);
				api.hasAction = true;
				
				String playlistURL = RadioStation.radioStationsPlaylist.get(title);
				if (playlistURL != null && !playlistURL.isEmpty()){
					api.actionInfo_add_action(ACTIONS.BUTTON_IN_APP_BROWSER);
					api.actionInfo_put_info("url", playlistURL); 
					api.actionInfo_put_info("title", "Playlist");
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
				
				api.setCustomAnswer(streamResultAns);
				api.setStatusSuccess();
				
				ApiResult result = api.build_API_result();
				return result;
				//<----------- END------------
			}
			
			//init card
			Card card = new Card(Card.TYPE_UNI_LIST);
			String active_stream = "";
			String active_name = "";

			//check cache for popular static stations
			JSONArray stationsCollection;
			boolean isPopular = (chacheStationName != null && !chacheStationName.isEmpty());
			if (isPopular){
				stationsCollection = RadioStation.radioStationsMap.get(chacheStationName);
				//get all from cache and finish
				if (stationsCollection != null && !stationsCollection.isEmpty()){
					long tic = System.currentTimeMillis();
					int i = 0;
					for (Object so : stationsCollection){
						JSONObject stationCard = (JSONObject) so;
						if (i==0){
							active_name = (String) stationCard.get("name");
							active_stream = (String) stationCard.get("streamURL");
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
				//---- make the HTTP GET call to Laut.FM API ----
				//http://api.laut.fm/documentation/search
				long tic = System.currentTimeMillis();
				int limit = 5;
				String url = "http://api.laut.fm/search/stations"
										+ "?query=" + URLEncoder.encode(station, "UTF-8") 
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
						}
						
					//Error
					}catch (Exception e){
						Debugger.println("Radio Laut.FM API - search: " + station + " - EXCEPTION: " + e.getMessage(), 1);
					}
				}
			}
			
			//check "other" API only when this one fails
			if (card.isEmpty()){
				stationsCollection =  new JSONArray();
				
				//search adjustments
				String search = station.replaceAll("1live", "Einslive");
			
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
					Debugger.println("Radio Dirble API - search: " + station + " - No API key to make call", 1);
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
						if (isPopular){
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
					
					//add stored Array to radio cache
					if (isPopular && !stationsCollection.isEmpty()){
						RadioStation.radioStationsMap.put(chacheStationName, stationsCollection);
						Debugger.println("Radio cache - wrote: '" + chacheStationName + "' with " + stationsCollection.size() + " stations", 3);
					}
				
				}else{
					//no results or communication error
					Debugger.println("Radio Dirble API - search: " + station + " - Communication error or no result", 1);
				}
			}
			
			//add action
			if (!active_stream.isEmpty()){
				api.actionInfo_add_action(ACTIONS.PLAY_AUDIO_STREAM);
				api.actionInfo_put_info("audio_url", active_stream);
				api.actionInfo_put_info("audio_title", active_name);
				
				/*
				api.actionInfo_add_action(ACTIONS.BUTTON_CMD);
				api.actionInfo_put_info("title", ((active_name.length() > 17)? (active_name.substring(0, 17) + "...") : active_name));
				api.actionInfo_put_info("info", "direct_cmd");
				api.actionInfo_put_info("cmd", CMD.getRadio(station));
				*/
				
				String playlistURL = RadioStation.radioStationsPlaylist.get(active_name);
				if (playlistURL == null){
					playlistURL = RadioStation.radioStationsPlaylist.get(active_name.replaceAll(" - .*", "").trim());
				}
				if (playlistURL != null && !playlistURL.isEmpty()){
					api.actionInfo_add_action(ACTIONS.BUTTON_IN_APP_BROWSER);
					api.actionInfo_put_info("url", playlistURL); 
					api.actionInfo_put_info("title", "Playlist");
				}
				
				api.hasAction = true;
			}
		
		//some error occurred somewhere 
		} catch (Exception e) {
			api.setStatusFail();
			Debugger.println("Music_Radio_Default.java - failed due to EXCEPTION: " + e.getMessage(), 1);
			Debugger.printStackTrace(e, 3);
		}
		
		//finally build the API_Result
		ApiResult result = api.build_API_result();
		
		//return result_JSON.toJSONString();
		return result;
	}
	
}
