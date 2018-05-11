package net.b07z.sepia.server.assist.apis;

import net.b07z.sepia.server.assist.apis.ApiInfo.Content;
import net.b07z.sepia.server.assist.apis.ApiInfo.Type;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.FilesAndStreams;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;

/**
 * Ancient "draft" for XBMC/Kodi controls.
 * Kodi examples: http://kodi.wiki/view/JSON-RPC_API/Examples
 * 
 * @author Florian Quirin
 **/
public class Server_XBMC_Kodi_Controls implements ApiInterface {
	
	//Kodi server access
	String kodiPort = "7972";
	String kodiURL =  "http://localhost:" + kodiPort + "/jsonrpc?request=";
	
	//Kodi statics
	String music_folder = Config.xtensionsFolder + "Music/";	//A folder with music (optimally this should be a user choice not a fixed variable, the user can pass it as a parameter though)
	
	//info
	public ApiInfo getInfo(String language){
		return new ApiInfo(Type.program, Content.data, true);
	}

	//result
	public ApiResult getResult(NluResult NLU_result) {
		//initialize result
		API api = new API(NLU_result);
		
		//get parameters
		String action = NLU_result.getParameter(PARAMETERS.ACTION);
						
		//Kodi variables
		int player_id;					//Kodi player ID (0: music, 1: video, 2: pictures)
		
		TheSwitch:
		switch (action.toLowerCase()) {
		
			//stop player - note: use only small case letters as names!!!
			case "<off>":

				//first get XBMC playerID (-1 if not found or server access problem)
				player_id = getPlayerID();
				if (player_id !=-2){
					
					//found ID
					//check if player is actually running
					if (player_id ==-1){
						api.answer = Config.answers.getAnswer(NLU_result, "server_music_0d");		//"it seems Kodi is not playing anything."
						break;
					
					//try stop the player
					}else{
						try{
							//http GET request to stop:
							String request = "{\"jsonrpc\":\"2.0\",\"method\":\"Player.Stop\",\"params\":{\"playerid\":"+player_id+"},\"id\":1}";
							String url = kodiURL + URLEncoder.encode(request, "UTF-8");
							//send stop request
							JSONObject result = Connectors.httpGET(url.trim());
							//System.out.println("result: " + result);		//debug
							
							//double check result if player actually stopped
							String result_val = (String) result.get("result");
							//System.out.println("value: " + result_val);		//debug
							
							if (result_val.trim().matches("OK")){
								if (player_id==0){
									api.answer = Config.answers.getAnswer(NLU_result, "server_music_1b");	//"music stopped"
								}else if (player_id==1){
									api.answer = Config.answers.getAnswer(NLU_result, "server_music_1c");	//"video stopped";
								}else if (player_id==2){
									api.answer = Config.answers.getAnswer(NLU_result, "server_music_1c");	//"pictures stopped";
								}else{
									api.answer = Config.answers.getAnswer(NLU_result, "server_music_1c");	//"sorry I'm confused. I can't identify what Kodi is playing.";
								}
								break TheSwitch;
							}else{
								api.answer = Config.answers.getAnswer(NLU_result, "server_music_0c");	//"Sorry I couldn't access the Kodi player somehow."
								break TheSwitch;
							}
						}catch (Exception e){
							e.printStackTrace();
							//TODO: answer?
						}
					}
				
				//no connection to XBMC player found (Kodi server problem?)
				}else{
					api.answer = Config.answers.getAnswer(NLU_result, "server_music_0c");		//"Sorry I couldn't access the Kodi player somehow."
					break;
				}
				
			//play something from folder
			case "<on>":
				
				//check connection to XBMC/Kodi
				player_id = getPlayerID();
				if (player_id !=-2){
					//player found, load all mp3 files, make a new playlist, add music and play
					try{
						//load all files from music_folder
						List<File> music = new ArrayList<File>();
						music = FilesAndStreams.directoryToFileList(music_folder, music, false);	//last parameter decides to use sub-folders (true,false)
						
						//folder ok?
						if (music == null){
							api.answer = Config.answers.getAnswer(NLU_result, "server_music_0a"); 		//not a folder
							break TheSwitch;
						}
						
						//prepare playlist number 1
						if (!clearPlaylist("1")){
							api.answer = Config.answers.getAnswer(NLU_result, "server_music_0b");		//"Sorry I have a folder or connection problem."
							break TheSwitch;
						}
						
						//add all mp3s to playlist 1
						String song;
						for (File f : music){
							song = f.getAbsolutePath();
							if (song.matches(".*\\.mp3")){
								if (!addFileToPlaylist("1",song)){
									api.answer = Config.answers.getAnswer(NLU_result, "server_music_0b");		//"Sorry I have a folder or connection problem."
									break TheSwitch;
								}
							}
						}
						
						//play playlist 1
						if (!playPlaylist("1")){
							api.answer = Config.answers.getAnswer(NLU_result, "server_music_0b");		//"Sorry I a folder or connection problem."
							break TheSwitch;
						}
						
						//mute Kodi until ILA has spoken - it would be nice to call this every time ILA is speaking or recoding
						muteTimer();
						
						//answer
						api.answer = Config.answers.getAnswer(NLU_result, "server_music_1a");		//"alrighty lets hear some good music."
						break;
						
					//an error occurred - try catch is a bit rough here, better check each response and react, but as this is just a demo ... ;-) 
					}catch (Exception e){
						e.printStackTrace();
						api.answer = Config.answers.getAnswer(NLU_result, "server_music_0b");		//"Sorry I couldn't access the Kodi player somehow."
						break;
					}
					
				//no connection to XBMC player found (Kodi server problem?)
					//one could either start Kodi here or ask the user to start Kodi or check the connection to the player
				}else{
					api.answer = Config.answers.getAnswer(NLU_result, "server_music_0b");		//"Sorry I couldn't access the Kodi player somehow."
					break;
				}
			
		}
		//finally build the API_Result
		ApiResult result = api.buildApiResult();
				
		//return result_JSON.toJSONString();
		return result;
	}

	
	//---HELPER methods for XBMC/Kodi---
	
	//get player ID - returns 1,2,3 for music, video, pictures, -1 for unknown and -2 for connection error
	public int getPlayerID(){
		return 0;
		/*
		try{
			int player_id=-1;			//save player_id, start with -1 as unknown
			String request = "{\"jsonrpc\": \"2.0\", \"method\": \"Player.GetActivePlayers\", \"id\": 1}";
			String url = kodiURL + URLEncoder.encode(request, "UTF-8");
			//send request
			JSONObject result = Connectors.httpGET_JSON(url.trim());
			System.out.println("result: " + result);		//debug
			//connection error
			if (result.toJSONString().trim().matches(".*<connection error>.*")){
				return -2;
			}
			player_id = Integer.parseInt(result.get("id").toString());
			return player_id;		//return ID if found
			
		}catch(Exception e){
			e.printStackTrace();
			return -1;
		}
		*/
	}
	
	//clear playlist
	public boolean clearPlaylist(String ID){
		try{
			String request = "{\"jsonrpc\": \"2.0\", \"method\": \"Playlist.Clear\", \"params\":{\"playlistid\":" + ID + "}, \"id\": 1}";
			String url = kodiURL + URLEncoder.encode(request, "UTF-8");
			//send request
			JSONObject result = Connectors.httpGET(url.trim());
			//check result
			//System.out.println("playlist clear result: " + result);			//debug
			String result_val = (String) result.get("result");
			if (result_val != null && result_val.trim().matches(".*\\bOK\\b.*")){
				return true;
			}
			return false;
		}catch (Exception e){
			e.printStackTrace();
			return false;
		}
	}
	
	//add file ID to playlist
	public boolean addFileToPlaylist(String playlistid, String ID){
		try{
			//stupid file systems, we need to convert windows path to unix for Kodi
			if (true){
				//TODO: fix
				ID = ID.replace("\\","/");
			}
			String request = "{\"jsonrpc\": \"2.0\", \"method\": \"Playlist.Add\", \"params\":{\"playlistid\":" + playlistid + ", \"item\" :{ \"file\" : \"" + ID + "\"}}, \"id\" : 1}";
			String url = kodiURL + URLEncoder.encode(request, "UTF-8");
			//send request
			JSONObject result = Connectors.httpGET(url.trim());
			//check result
			//System.out.println("playlist add result: " + result);			//debug
			String result_val = (String) result.get("result");
			if (result_val != null && result_val.trim().matches(".*\\bOK\\b.*")){
				return true;
			}
			return false;
		}catch (Exception e){
			e.printStackTrace();
			return false;
		}
	}
	
	//play playlist with ID
	public boolean playPlaylist(String ID){
		try{
			String request = "{\"jsonrpc\": \"2.0\", \"method\": \"Player.Open\", \"params\":{\"item\":{\"playlistid\":" + ID + ", \"position\" : 0}}, \"id\": 1}";
			String url = kodiURL + URLEncoder.encode(request, "UTF-8");
			//send request
			JSONObject result = Connectors.httpGET(url.trim());
			//check result
			//System.out.println("playlist play result: " + result);			//debug
			String result_val = (String) result.get("result");
			if (result_val != null && result_val.trim().matches(".*\\bOK\\b.*")){
				return true;
			}
			return false;
		}catch (Exception e){
			e.printStackTrace();
			return false;
		}
	}
	
	//volume control: 
	public int reduceVolume(){
		try{
			String request = "{\"jsonrpc\": \"2.0\", \"method\": \"Application.SetVolume\", \"params\": { \"volume\": \"decrement\" }, \"id\": 1 }";
			String url = kodiURL + URLEncoder.encode(request, "UTF-8");
			//send request
			JSONObject result = Connectors.httpGET(url.trim());
			//check result
			//System.out.println("volume result: " + result);			//debug
			int result_val = (int) result.get("result");
			if (result_val>=0 & result_val<=100){
				return result_val;
			}
			return -1;
		}catch (Exception e){
			e.printStackTrace();
			return -1;
		}
	}
	public int increaseVolume(){
		try{
			String request = "{\"jsonrpc\": \"2.0\", \"method\": \"Application.SetVolume\", \"params\": { \"volume\": \"increment\" }, \"id\": 1 }";
			String url = kodiURL + URLEncoder.encode(request, "UTF-8");
			//send request
			JSONObject result = Connectors.httpGET(url.trim());
			//check result
			//System.out.println("volume result: " + result);			//debug
			int result_val = (int) result.get("result");
			if (result_val>=0 & result_val<=100){
				return result_val;
			}
			return -1;
		}catch (Exception e){
			e.printStackTrace();
			return -1;
		}
	}
	public boolean mute(){
		//String request = "{\"jsonrpc\": \"2.0\", \"method\": \"Application.SetMute\", \"params\": { \"mute\": true }, \"id\": 1 }";
		//TODO: impl.
		//check result
		//System.out.println("volume mute result: " + result);			//debug
		return true;
	}
	public boolean unmute(){
		//String request = "{\"jsonrpc\": \"2.0\", \"method\": \"Application.SetMute\", \"params\": { \"mute\": false }, \"id\": 1 }";
		//TODO: impl.
		//check result
		//System.out.println("volume unmute result: " + result);			//debug
		return true;
	}
	//trigger a timed mute that ends when ILA stops speaking or recording
	public void muteTimer(){
		//TODO: impl.
		/*
		Thread worker;
		//do it in its own thread
		worker = new Thread() {
          	public void run() {
          		//mute and wait a bit (2s)
          		mute();
          		try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
          		//then start checking for ILA actions
          		int counter=100;		//max wait 20s
          		while( (ILA_interface.avatar.isILAspeaking() || ILA_interface.avatar.is_recording==1) & counter>1 ){
        			counter--;
        			ILA_debug.println("ADDON-KODI - mute timer is waiting...",2);			//debug
        			try { Thread.sleep(200); } catch (InterruptedException e) { e.printStackTrace(); }
        		}
          		//timers are over: unmute
          		unmute();
          	}
        };
        worker.start();
        */
	}
}
