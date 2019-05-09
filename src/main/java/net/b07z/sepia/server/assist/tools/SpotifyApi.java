package net.b07z.sepia.server.assist.tools;

import java.net.URLEncoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.ContentBuilder;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Class that helps to connect with Spotify APIs.
 * 
 * @author Florian Quirin
 *
 */
public class SpotifyApi {
	
	private String clientId;
	private String clientSecret;
	
	public String token = "";
	public String tokenType = "";
	public long tokenValidUntil = 0;
	public long lastRefreshTry = 0;
	public static final long FAILED_AUTH_TIMEOUT = 1000*60*15;
	
	public static final String spotifyAuthUrl = "https://accounts.spotify.com/api/token";
	public static final String spotifySearchUrl = "https://api.spotify.com/v1/search";
	
	public static final String TYPE_TRACK = "track";
	public static final String TYPE_ALBUM = "album";
	public static final String TYPE_ARTIST = "artist";
	public static final String TYPE_PLAYLIST = "playlist";
	
	public SpotifyApi(String clientId, String clientSecret){
		this.clientId = clientId;
		this.clientSecret = clientSecret;
	}
	
	/**
	 * Get an access token using client ID and secret.
	 * @return status code: 0:all good, 1:no credentials, 2:access problems, 3:unknown problems, 4:access denied  
	 */
	public int getTokenViaClientCredentials(){
		//Do we have credentials?
		if (Is.nullOrEmpty(clientId) || Is.nullOrEmpty(clientSecret)){
			return 1;
		}
		try{
			//Build auth. header entry
			String authString = "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
			
			//Request headers
			Map<String, String> headers = new HashMap<>();
			headers.put("Content-Type", "application/x-www-form-urlencoded");
			headers.put("Authorization", authString);
			
			//Request data
			String data = ContentBuilder.postForm("grant_type", "client_credentials");
			
			//Call
			long tic = System.currentTimeMillis();
			this.lastRefreshTry = tic;
			JSONObject res = Connectors.httpPOST(spotifyAuthUrl, data, headers);
			Statistics.addExternalApiHit("SpotifyApi getTokenViaClientCredentials");
			Statistics.addExternalApiTime("SpotifyApi getTokenViaClientCredentials", tic);
			//System.out.println(res.toJSONString()); 		//DEBUG
			if (!Connectors.httpSuccess(res)){
				return 2;
			}
			String token = JSON.getString(res, "access_token");
			String tokenType = JSON.getString(res, "token_type");
			long expiresIn = JSON.getLongOrDefault(res, "expires_in", 0);
			if (Is.nullOrEmpty(token)){
				return 4;
			}else{
				this.token = token;
				this.tokenType = tokenType;
				this.tokenValidUntil = System.currentTimeMillis() + (expiresIn * 1000);
			}
			return 0;
			
		}catch (Exception e){
			Statistics.addExternalApiHit("SpotifyApi-error getTokenViaClientCredentials");
			return 3;
		}
	}
	
	/**
	 * Search ONE item in the Spotify database that fits best. 
	 * Search 'type' (track, artist, album, playlist) is automatically set using the information given for track, artist, album and playlist.
	 * @param track
	 * @param artist
	 * @param album
	 * @param playlist
	 * @return
	 */
	public JSONObject searchBestItem(String track, String artist, String album, String playlist, String genre){
		if (Is.nullOrEmpty(this.token) && (System.currentTimeMillis() - this.lastRefreshTry) < FAILED_AUTH_TIMEOUT ){
			return JSON.make(
					"error", "not authorized", 
					"status", 401
			);
		}
		if ((this.tokenValidUntil - System.currentTimeMillis()) <= 0){
			//Try to get new token
			int refreshCode = getTokenViaClientCredentials();
			if (refreshCode > 0){
				return JSON.make(
						"error", "failed to refresh token", 
						"status", 401, 
						"code", refreshCode
				);
			}
		}
		//Build request
		String type = "";
		String q = "";
		try{
			//Search
			if (Is.notNullOrEmpty(playlist)){
				type = TYPE_PLAYLIST;
				q = URLEncoder.encode(playlist, "UTF-8");
			
			}else if (Is.notNullOrEmpty(track) && Is.notNullOrEmpty(album)){
				type = TYPE_TRACK;
				q = URLEncoder.encode("track:" + track + " album:" + album, "UTF-8");
			
			}else if (Is.notNullOrEmpty(track) && Is.notNullOrEmpty(artist)){
				type = TYPE_TRACK;
				q = URLEncoder.encode("track:" + track + " artist:" + artist, "UTF-8");
			
			}else if (Is.notNullOrEmpty(album)){
				type = TYPE_ALBUM;
				q = URLEncoder.encode(album, "UTF-8");
				
			}else if (Is.notNullOrEmpty(artist)){
				type = TYPE_ARTIST;
				q = URLEncoder.encode(artist, "UTF-8");
				
			}else if (Is.notNullOrEmpty(genre)){
				type = TYPE_PLAYLIST;
				q = URLEncoder.encode(genre, "UTF-8");
			
			}else if (Is.notNullOrEmpty(track)){
				type = TYPE_TRACK;
				q = URLEncoder.encode(track, "UTF-8");
			
			}else{
				return JSON.make("error", "combination of search keys invalid", "status", 400);
			}
			int N = 1;
			String url = spotifySearchUrl + "?q=" + q + "&type=" + type + "&limit=" + N;
			
			//Header
			String authString = "Bearer  " + this.token;
			
			Map<String, String> headers = new HashMap<>();
			headers.put("Accept", "application/json");
			headers.put("Content-Type", "application/json");
			headers.put("Authorization", authString);
			
			//Call
			//System.out.println("URL: " + url); 		//DEBUG
			long tic = System.currentTimeMillis();
			JSONObject res = Connectors.httpGET(url, null, headers);
			Statistics.addExternalApiHit("SpotifyApi searchBestItem");
			Statistics.addExternalApiTime("SpotifyApi searchBestItem", tic);
			
			//Get first item
			if (Connectors.httpSuccess(res)){
				JSONArray items = null;
				if (type.equals(TYPE_PLAYLIST)){
					items = JSON.getJArray(res, new String[]{"playlists", "items"});
				}else if (type.equals(TYPE_ALBUM)){
					items = JSON.getJArray(res, new String[]{"albums", "items"});
				}else if (type.equals(TYPE_ARTIST)){
					items = JSON.getJArray(res, new String[]{"artists", "items"});
				}else if (type.equals(TYPE_TRACK)){
					items = JSON.getJArray(res, new String[]{"tracks", "items"});
				}
				if (Is.nullOrEmpty(items)){
					if (Is.notNullOrEmpty(res) && res.containsKey("error")){
						JSONObject error = JSON.getJObject(res, "error");
						JSON.put(error, "type", "error");
						return error;
					}else{
						return JSON.make(
								"message", "no item found for query",
								"query", q,
								"type", "no_match",
								"status", 200
						);
					}
				}else{
					JSONObject firstItem = JSON.getJObject(items, 0);
					if (firstItem != null){
						String resType = JSON.getString(firstItem, "type");
						if (resType.equals(TYPE_PLAYLIST)){
							return JSON.make(
									"type", TYPE_PLAYLIST, 
									"name", JSON.getString(firstItem, "name"),
									"uri", JSON.getString(firstItem, "uri"),
									"owner_display_name", JSON.getObject(firstItem, new String[]{"owner", "display_name"}),
									"total_tracks", JSON.getObject(firstItem, new String[]{"tracks", "total"})
							);
						}else if (resType.equals(TYPE_ALBUM)){
							JSONArray artists = JSON.getJArray(firstItem, "artists");
							return JSON.make(
									"type", TYPE_ALBUM, 
									"name", JSON.getString(firstItem, "name"),
									"uri", JSON.getString(firstItem, "uri"),
									"primary_artist", (Is.notNullOrEmpty(artists)? JSON.getString(JSON.getJObject(artists, 0), "name") : ""),
									"total_tracks", JSON.getIntegerOrDefault(firstItem, "total_tracks", -1)
							);
						}else if (resType.equals(TYPE_ARTIST)){
							return JSON.make(
									"type", TYPE_ARTIST, 
									"name", JSON.getString(firstItem, "name"),
									"uri", JSON.getString(firstItem, "uri"),
									"genres", JSON.getJArray(firstItem, "genres")
							);
						}else if (resType.equals(TYPE_TRACK)){
							JSONArray artists = JSON.getJArray(firstItem, "artists");
							return JSON.make(
									"type", TYPE_TRACK, 
									"name", JSON.getString(firstItem, "name"),
									"uri", JSON.getString(firstItem, "uri"),
									"primary_artist", (Is.notNullOrEmpty(artists)? JSON.getString(JSON.getJObject(artists, 0), "name") : ""),
									"album", JSON.getObject(firstItem, new String[]{"album", "name"})
							);
						}
						return firstItem;
					}
				}
			}
			return res;
			
		}catch (Exception e){
			Statistics.addExternalApiHit("SpotifyApi-error searchBestItem");
			return JSON.make("error", e.getMessage(), "status", 500);
		}
	}
}
